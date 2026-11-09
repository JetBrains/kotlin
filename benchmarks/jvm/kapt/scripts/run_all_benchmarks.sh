#!/usr/bin/env bash
# Run kapt-benchmark scenarios and print a per-scenario summary table.
#
# Scenarios are DISCOVERED from scenarios/*.scenarios, not hardcoded — adding a cell to a
# .scenarios file is enough to make it runnable here. (This script used to hardcode
# `3 workloads x 3 sizes`, so the wide/nested/deeppkg/incremental cells added later were
# unreachable through it.)
#
# Requirements:
#   - gradle-profiler on PATH (https://github.com/gradle/gradle-profiler/releases)
#   - python3 (for the summary script)
#
# Usage (from the repository root):
#   ./scripts/run_all_benchmarks.sh                  # the default suite (see DEFAULT_SUITE)
#   ./scripts/run_all_benchmarks.sh --all            # every discovered scenario except ALWAYS_SKIP
#   ./scripts/run_all_benchmarks.sh --list           # print what is discoverable, run nothing
#   ./scripts/run_all_benchmarks.sh synthetic_wide dagger_large   # just these
#
# Output:
#   profile-out/<scenario>/benchmark.csv
#   profile-out/<scenario>/benchmark.html
#
# Total wall time depends on the machine and on how many cells are selected; the 9-cell default
# suite is roughly half an hour on a fast workstation. Time it on your own hardware before
# planning around it.

set -euo pipefail

cd "$(dirname "$0")/.."

# The 9 clean-build cells that are comparable with one another and safe to run unattended.
DEFAULT_SUITE=(
    synthetic_small synthetic_medium synthetic_large
    dagger_small    dagger_medium    dagger_large
    mapstruct_small mapstruct_medium mapstruct_large
)

# Excluded from --all too, because running these alongside the others is misleading rather than
# merely slow:
#   synthetic_incremental — the only non-clean cell; not comparable with clean-build cells and
#                           must be reported separately.
#   compiler_corpus_*     — an A/B pair only meaningful against each other, and `stripped` is a
#                           measurement artefact rather than a workload anyone ships.
ALWAYS_SKIP=(synthetic_incremental compiler_corpus_baseline compiler_corpus_stripped)

# --- discovery -------------------------------------------------------------------------------
# Maps scenario name -> the file defining it, by scanning for top-level `name {` headers.
declare -A SCENARIO_FILE=()
ALL_SCENARIOS=()
for f in scenarios/*.scenarios; do
    [ -e "$f" ] || continue
    while read -r name; do
        [ -n "$name" ] || continue
        if [ -n "${SCENARIO_FILE[$name]:-}" ]; then
            echo "Error: scenario '$name' is defined in both ${SCENARIO_FILE[$name]} and $f." >&2
            exit 1
        fi
        SCENARIO_FILE[$name]="$f"
        ALL_SCENARIOS+=("$name")
    done < <(grep -oE '^[a-zA-Z_0-9]+[[:space:]]*\{' "$f" | sed 's/[[:space:]]*{//')
done

if [ ${#ALL_SCENARIOS[@]} -eq 0 ]; then
    echo "Error: no scenarios found under scenarios/*.scenarios." >&2
    exit 1
fi

in_list() { local n="$1"; shift; local x; for x in "$@"; do [ "$x" = "$n" ] && return 0; done; return 1; }

# --- selection -------------------------------------------------------------------------------
SELECTED=()
case "${1:-}" in
    --list)
        printf '%-28s %-34s %s\n' "SCENARIO" "FILE" "IN DEFAULT SUITE"
        for n in "${ALL_SCENARIOS[@]}"; do
            if in_list "$n" "${DEFAULT_SUITE[@]}"; then tag="yes"
            elif in_list "$n" "${ALWAYS_SKIP[@]}"; then tag="no (excluded from --all, see script)"
            else tag="no (opt-in, --all includes it)"; fi
            printf '%-28s %-34s %s\n' "$n" "${SCENARIO_FILE[$n]}" "$tag"
        done
        exit 0
        ;;
    --all)
        for n in "${ALL_SCENARIOS[@]}"; do
            in_list "$n" "${ALWAYS_SKIP[@]}" && continue
            SELECTED+=("$n")
        done
        ;;
    "")
        SELECTED=("${DEFAULT_SUITE[@]}")
        ;;
    *)
        SELECTED=("$@")
        ;;
esac

# Validate up front: a typo should not surface 20 minutes into a run.
for n in "${SELECTED[@]}"; do
    if [ -z "${SCENARIO_FILE[$n]:-}" ]; then
        echo "Error: unknown scenario '$n'. Run with --list to see what is available." >&2
        exit 1
    fi
done

# --- prerequisites ---------------------------------------------------------------------------
if ! command -v gradle-profiler >/dev/null 2>&1; then
    echo "Error: gradle-profiler is not on PATH." >&2
    echo "Install via 'brew install gradle-profiler' or download from" >&2
    echo "https://github.com/gradle/gradle-profiler/releases" >&2
    exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
    echo "Error: python3 is not on PATH." >&2
    exit 1
fi

# --- run -------------------------------------------------------------------------------------
echo "Running ${#SELECTED[@]} scenario(s): ${SELECTED[*]}"
echo ""

csv_files=()
for n in "${SELECTED[@]}"; do
    echo "=== Running ${n} (${SCENARIO_FILE[$n]}) ==="
    gradle-profiler --benchmark \
        --output-dir "profile-out/${n}" \
        --scenario-file "${SCENARIO_FILE[$n]}" \
        "${n}"
    csv_files+=("profile-out/${n}/benchmark.csv")
done

echo ""
echo "=== Summary (${#SELECTED[@]} scenario(s)) ==="
python3 scripts/summarize_benchmark.py "${csv_files[@]}"
