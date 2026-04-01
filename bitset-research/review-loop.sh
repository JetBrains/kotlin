#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# Automated review loop for BitSet research artifacts
# Codex reviews artifacts -> Claude validates & applies fixes
# Usage: ./review-loop.sh <step> <from> <to(included)>
# ============================================================

COMMIT_MSG_FILE=""
REVIEW_TMP=""
cleanup() { rm -f "${COMMIT_MSG_FILE:-}" "${REVIEW_TMP:-}"; }
trap 'cleanup; echo " Interrupted."; exit 130' INT TERM
trap 'cleanup' EXIT

FORCE_REGEN=false
if [[ "${1:-}" == "--force-regen" ]]; then
    FORCE_REGEN=true
    shift
fi

if [[ $# -ne 3 ]]; then
    echo "Usage: $0 [--force-regen] <step> <from> <to(included)>"
    echo "  --force-regen: re-generate review files even if they already exist"
    echo "  step: step number (1-14)"
    echo "  from: first review iteration number"
    echo "  to:   last review iteration number (included)"
    exit 1
fi

STEP=$1
START=$2
END=$3

if ! [[ "$STEP" =~ ^[0-9]+$ ]] || ! [[ "$START" =~ ^[0-9]+$ ]] || ! [[ "$END" =~ ^[0-9]+$ ]]; then
    echo "ERROR: all arguments must be positive integers" >&2
    exit 1
fi

if [[ $STEP -lt 1 ]]; then
    echo "ERROR: step must be >= 1, got: $STEP" >&2
    exit 1
fi

if [[ $START -gt $END ]]; then
    echo "ERROR: from ($START) must be <= to ($END)" >&2
    exit 1
fi

STEP_PADDED=$(printf '%02d' "$STEP")

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

REVIEW_DIR="bitset-research/review/step-${STEP_PADDED}"
PLAN="bitset-research/bitset-research-plan.md"

# ---- Artifact discovery ----
shopt -s nullglob
ARTIFACTS=(bitset-research/step-${STEP_PADDED}-*.md)
shopt -u nullglob

if [[ ${#ARTIFACTS[@]} -eq 0 ]]; then
    echo "ERROR: No artifacts found for step ${STEP_PADDED} (glob: bitset-research/step-${STEP_PADDED}-*.md)" >&2
    exit 1
fi

# ---- Pre-flight checks ----
for cmd in codex claude git; do
    if ! command -v "$cmd" &>/dev/null; then
        echo "ERROR: '$cmd' not found in PATH" >&2
        exit 1
    fi
done

if [[ -n $(git status --porcelain) ]]; then
    echo "ERROR: Working tree has uncommitted changes. Commit or stash first." >&2
    git status --short >&2
    exit 1
fi

# A valid review must contain the "## Статус" section
review_is_valid() { [[ -f "$1" ]] && grep -q '## Статус' "$1"; }

mkdir -p "$REVIEW_DIR"

echo "=== Review loop: step ${STEP_PADDED}, iterations ${START}..${END} ==="
echo "=== Branch: $(git branch --show-current) ==="
echo "=== Artifacts (${#ARTIFACTS[@]} files): ==="
printf '  %s\n' "${ARTIFACTS[@]}"
echo ""

COMMITS_MADE=0
CONSECUTIVE_NO_CHANGES=0

print_iter_footer() {
    local nn=$1 start_epoch=$2
    local end_epoch elapsed
    end_epoch=$(date +%s)
    elapsed=$((end_epoch - start_epoch))
    echo "=== Iteration ${nn} finished at $(date '+%H:%M:%S') (elapsed: $((elapsed / 60))m $((elapsed % 60))s) ==="
    echo ""
}

for i in $(seq "$START" "$END"); do
    NN=$(printf '%02d' "$i")
    REVIEW_FILE="${REVIEW_DIR}/codex-review-${NN}.md"
    CODEX_LOG="${REVIEW_DIR}/log-${NN}-codex.txt"
    CLAUDE_LOG="${REVIEW_DIR}/log-${NN}-claude.txt"
    COMMIT_MSG_FILE="${REVIEW_DIR}/.commit-msg-${NN}.tmp"

    rm -f "$COMMIT_MSG_FILE"
    ITER_START_EPOCH=$(date +%s)

    echo "=== Iteration ${NN} started at $(date '+%H:%M:%S') ==="

    HEAD_BEFORE=$(git rev-parse HEAD)

    # ---- Phase 1: Codex writes review ----

    if [[ "$FORCE_REGEN" == true ]] && [[ -f "$REVIEW_FILE" ]]; then
        echo "[${NN}] --force-regen: removing existing review file."
        rm -f "$REVIEW_FILE"
    fi

    if review_is_valid "$REVIEW_FILE"; then
        echo "[${NN}] Review file already exists and is valid, skipping Codex phase."
    else
        if [[ -f "$REVIEW_FILE" ]]; then
            echo "[${NN}] WARNING: Review file exists but appears incomplete, regenerating."
            rm -f "$REVIEW_FILE"
        fi
        echo "[${NN}] Phase 1: Codex review..."

        # Collect previous reviews for context
        shopt -s nullglob
        PREV_REVIEW_FILES=("${REVIEW_DIR}"/codex-review-*.md)
        shopt -u nullglob
        PREV_REVIEWS=""
        if [[ ${#PREV_REVIEW_FILES[@]} -gt 0 ]]; then
            PREV_REVIEWS=$(printf '   - %s\n' "${PREV_REVIEW_FILES[@]}")
        fi

        # Codex writes to a temp file; we atomically move it on success
        REVIEW_TMP="${REVIEW_FILE}.tmp"
        rm -f "$REVIEW_TMP"

        CODEX_PROMPT="Ты — ревьюер исследовательских артефактов. Твоя задача — провести тщательное ревью шага ${STEP} из исследовательского плана.

Прочитай следующие файлы:
1. План исследования: ${PLAN} (секция «Шаг ${STEP}»)
2. Все артефакты шага ${STEP}:
$(printf '   - %s\n' "${ARTIFACTS[@]}")

3. Все предыдущие ревью (чтобы НЕ повторять уже разобранные замечания):
${PREV_REVIEWS}

Напиши ревью в файл: ${REVIEW_TMP}

Требования к ревью:
- Формат: # Ревью step-${STEP_PADDED}-*.md (итерация ${NN}) / ## Резюме / ## Статус / ## Замечания / ## Итог
- Каждое замечание: приоритет (High/Medium/Low), заголовок, список проблемных мест (со ссылками на файлы и разделы), описание расхождения с первоисточником или внутреннего противоречия, конкретное предложение по исправлению, ссылки на источники (официальная документация, исходный код).
- НЕ повторяй замечания из предыдущих ревью, если они уже были исправлены. Сверяйся с текущим состоянием артефактов.
- Ищи: фактические ошибки (несоответствие официальной документации), внутренние противоречия между детальными артефактами и агрегирующим, пробелы в покрытии осей сравнения из плана, устаревшую информацию.
- Для проверки фактов используй веб-поиск и официальную документацию языков/библиотек.
- Статус: «Требует доработки» или «Готов как входной артефакт для следующих шагов».
- Язык: русский."

        PHASE1_START=$(date +%s)
        echo "" >> "$CODEX_LOG"
        echo "=== Run started at $(date '+%Y-%m-%d %H:%M:%S') (iteration ${NN}) ===" >> "$CODEX_LOG"
        # Note: --full-auto grants Codex unrestricted shell access
        CODEX_OK=true
        if ! codex exec --full-auto --ephemeral "$CODEX_PROMPT" \
                >> "$CODEX_LOG" 2>&1; then
            echo "[${NN}] WARNING: Codex exited with error, checking if review was written..."
            CODEX_OK=false
        fi
        PHASE1_ELAPSED=$(( $(date +%s) - PHASE1_START ))
        echo "[${NN}] Phase 1 done ($((PHASE1_ELAPSED / 60))m $((PHASE1_ELAPSED % 60))s)"

        # Atomic move: only promote temp file if it looks valid
        if review_is_valid "$REVIEW_TMP"; then
            mv -f "$REVIEW_TMP" "$REVIEW_FILE"
        else
            echo "[${NN}] WARNING: Review temp file missing or incomplete (no '## Статус' section)."
            rm -f "$REVIEW_TMP"
            if [[ "$CODEX_OK" == false ]]; then
                echo "[${NN}] Codex failed and review is invalid. Skipping to next iteration."
                print_iter_footer "$NN" "$ITER_START_EPOCH"
                continue
            fi
        fi
    fi

    # ---- Phase 2: Verify review file ----
    if [[ ! -f "$REVIEW_FILE" ]]; then
        echo "[${NN}] Phase 2: ERROR — review file not created. Skipping Claude phase."
        print_iter_footer "$NN" "$ITER_START_EPOCH"
        continue
    fi
    echo "[${NN}] Phase 2: Review verified, $(wc -l < "$REVIEW_FILE") lines"

    if grep -qi -- 'готов.*как входной' "$REVIEW_FILE"; then
        echo "[${NN}] Phase 2: Review status READY. Stopping loop."
        print_iter_footer "$NN" "$ITER_START_EPOCH"
        break
    fi

    # ---- Phase 3: Claude validates and fixes ----
    echo "[${NN}] Phase 3: Claude validates and fixes..."

    CLAUDE_PROMPT="Ты — редактор исследовательских артефактов проекта Multiplatform BitSet для Kotlin stdlib.

Прочитай ревью: ${REVIEW_FILE}

Прочитай все артефакты шага ${STEP}:
$(printf '%s\n' "${ARTIFACTS[@]/#/- }")

Прочитай план исследования: ${PLAN} (секция «Шаг ${STEP}»), чтобы понимать требования к артефактам.

Для каждого замечания из ревью:
1. Проверь, действительно ли указанная проблема присутствует в текущей версии артефакта (замечание могло быть уже исправлено в предыдущей итерации).
2. Если замечание валидно — внеси исправление в соответствующий артефакт. При исправлении:
   - Сохраняй стиль и структуру документа.
   - Если исправление в детальном артефакте затрагивает данные в агрегирующем, обнови оба.
   - Не удаляй корректную информацию при исправлении ошибочной.
3. Если замечание невалидно (уже исправлено, или ревьюер ошибся) — пропусти его.

После всех исправлений:
- Если были внесены изменения, запиши краткое описание основных исправлений (одна строка, на английском) в файл: ${COMMIT_MSG_FILE}
  Формат: apply review ${NN} — <краткое описание основных исправлений>
  НЕ добавляй префикс «Update \`bitset-research\`:» — его добавит скрипт.
  НЕ выполняй git add, git commit и любые другие git-команды.
- Если ни одно замечание не потребовало исправлений, НЕ создавай файл с commit message.
- Не трогай файлы за пределами bitset-research/step-${STEP_PADDED}-*.md.

Ultrathink."

    PHASE3_START=$(date +%s)
    CLAUDE_OK=true
    echo "" >> "$CLAUDE_LOG"
    echo "=== Run started at $(date '+%Y-%m-%d %H:%M:%S') (iteration ${NN}) ===" >> "$CLAUDE_LOG"
    # Note: --dangerously-skip-permissions grants Claude unrestricted shell access
    if ! claude -p \
            --dangerously-skip-permissions \
            --no-session-persistence \
            --output-format stream-json \
            --verbose \
            "$CLAUDE_PROMPT" \
            >> "$CLAUDE_LOG" 2>&1; then
        echo "[${NN}] WARNING: Claude exited with error (see ${CLAUDE_LOG})"
        CLAUDE_OK=false
    fi
    PHASE3_ELAPSED=$(( $(date +%s) - PHASE3_START ))
    echo "[${NN}] Phase 3 done ($((PHASE3_ELAPSED / 60))m $((PHASE3_ELAPSED % 60))s)"

    # ---- Phase 4: Commit if changes were made ----

    # Safety net: check if Claude committed despite instructions
    HEAD_AFTER=$(git rev-parse HEAD)
    if [[ "$HEAD_BEFORE" != "$HEAD_AFTER" ]]; then
        echo "[${NN}] WARNING: Claude made a commit despite instructions. Accepting it."
        echo "[${NN}] Claude committed: $(git log --oneline -1)"
        COMMITS_MADE=$((COMMITS_MADE + 1))
        CONSECUTIVE_NO_CHANGES=0
        rm -f "$COMMIT_MSG_FILE"
        print_iter_footer "$NN" "$ITER_START_EPOCH"
        continue
    fi

    # Revert stray changes outside artifacts
    STRAY_FILES=()
    while IFS= read -r f; do
        [[ -n "$f" ]] && STRAY_FILES+=("$f")
    done < <(git diff HEAD --name-only | grep -v -x -F -f <(printf '%s\n' "${ARTIFACTS[@]}") || true)
    if [[ ${#STRAY_FILES[@]} -gt 0 ]]; then
        echo "[${NN}] WARNING: Claude modified files outside artifacts, reverting:"
        printf '  %s\n' "${STRAY_FILES[@]}"
        echo "=== Stray file diff (iteration ${NN}, reverted) ===" >> "$CLAUDE_LOG"
        git diff HEAD -- "${STRAY_FILES[@]}" >> "$CLAUDE_LOG"
        git checkout HEAD -- "${STRAY_FILES[@]}"
    fi

    # Remove untracked files Claude may have created anywhere in the repo
    UNTRACKED_STRAY=()
    while IFS= read -r f; do
        [[ -n "$f" ]] && UNTRACKED_STRAY+=("$f")
    done < <(git ls-files --others --exclude-standard | grep -v -x -F -f <(printf '%s\n' "${ARTIFACTS[@]}") || true)
    if [[ ${#UNTRACKED_STRAY[@]} -gt 0 ]]; then
        echo "[${NN}] WARNING: Claude created untracked files outside artifacts, removing:"
        printf '  %s\n' "${UNTRACKED_STRAY[@]}"
        rm -f -- "${UNTRACKED_STRAY[@]}"
    fi

    if ! git diff HEAD --quiet -- "${ARTIFACTS[@]}"; then
        if [[ -f "$COMMIT_MSG_FILE" ]] && [[ -s "$COMMIT_MSG_FILE" ]]; then
            COMMIT_DESC=$(head -n 1 -- "$COMMIT_MSG_FILE" | cut -c1-200)
            COMMIT_MSG="Update \`bitset-research\`: ${COMMIT_DESC}"
        else
            COMMIT_MSG="Update \`bitset-research\`: apply review ${NN} fixes to step-${STEP_PADDED} artifacts"
        fi

        git add -- "${ARTIFACTS[@]}"
        echo "$COMMIT_MSG" | git commit -F -
        echo "[${NN}] Committed: $(git log --oneline -1)"
        COMMITS_MADE=$((COMMITS_MADE + 1))
        CONSECUTIVE_NO_CHANGES=0
    else
        echo "[${NN}] No changes from this iteration."
        if [[ "$CLAUDE_OK" == true ]]; then
            CONSECUTIVE_NO_CHANGES=$((CONSECUTIVE_NO_CHANGES + 1))
            if [[ $CONSECUTIVE_NO_CHANGES -ge 3 ]]; then
                echo "=== ${CONSECUTIVE_NO_CHANGES} consecutive no-change iterations. Stopping. ==="
                rm -f "$COMMIT_MSG_FILE"
                print_iter_footer "$NN" "$ITER_START_EPOCH"
                break
            fi
        else
            echo "[${NN}] (not counting toward no-change limit due to AI error)"
        fi
    fi

    rm -f "$COMMIT_MSG_FILE"
    print_iter_footer "$NN" "$ITER_START_EPOCH"
done

echo "=== Done. Commits: ${COMMITS_MADE}, Consecutive no-change: ${CONSECUTIVE_NO_CHANGES} ==="
