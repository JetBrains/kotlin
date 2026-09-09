#!/usr/bin/env bash
#
# Runs a whole *matrix* of run-bench.sh configurations unattended, one cell after another.
#
# Why a driver: a single pinned run answers "is this machine slower", the matrix answers "why". The thread
# scaling sweep separates a per-syscall cost from a memory bandwidth limit, the serial cells remove the
# scheduling noise of 8 concurrent compilations, and the calibration cells - run first and last - show whether
# the machine itself drifted during the session. Running them by hand takes hours of attention, so this script
# never runs two cells at the same time, never aborts on a failing cell, and records what it did.
#
# Usage:
#   scripts/run-experiments.sh --label m5max-falcon-on --set all
#   scripts/run-experiments.sh --label m2max-falcon-off --set serial --set scaling
#   scripts/run-experiments.sh --label m5max-falcon-on --set all --dry-run
#
# Run the very same command line on both machines (or in both agent configurations), changing only --label.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
RUN_BENCH="$SCRIPT_DIR/run-bench.sh"
ORIGINAL_ARGS=("$@")

# ---------------------------------------------------------------------------------------------------------------
# Rough cost model, only used for the up-front estimate. The full IntelliJ model compiled by a single thread
# takes ~2900 s on these machines; at a parallelism of 8 the same suite takes ~450-570 s rather than the ideal
# 2900/8 = 362 s, so a parallel cell is charged an extra factor. A sample of fraction f costs about f times the
# full suite, because the modules are selected by a hash and not by size.
# ---------------------------------------------------------------------------------------------------------------
FULL_SUITE_SERIAL_SECONDS=2900
PARALLEL_OVERHEAD=1.4
CALIBRATION_SECONDS=90

LABEL=""
SETS=()
OUT_DIR=""
TESTS=""
CALIBRATION_TEST="*FullPipelineTestsGenerated.testFleet_andel"
DRY_RUN=0
PREBUILD=1
CAFFEINATE=1
PASSTHROUGH=()

# The thread counts of the scaling sweep: the interesting region is 1-8, then the two machines being compared
# (12 and 18 cores) and whatever this machine really has.
SCALING_PARALLELISM=(1 2 4 6 8 12)

usage() {
    sed -n '3,16p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
    cat <<EOF

Options:
  -l, --label BASE        base label; every cell gets BASE__<cell> (required)
  -s, --set NAME          experiment set, may be repeated: $(all_set_names)
  -o, --out-dir DIR       root directory for the run directories (default: <repo>/tmp/fir-bench)
  -t, --tests PATTERN     override the suite of the cells that compile
      --calibration-test PATTERN  test used by the calibration cells (default: $CALIBRATION_TEST)
      --no-prebuild       do not build :dist and the test classes before the first cell
      --no-caffeinate     do not prevent the machine from sleeping
  -y, --yes               accepted and ignored, the driver never prompts
      --dry-run           print the ordered list of run-bench.sh invocations and exit
  -h, --help              this text
  --                      everything after it is passed to run-bench.sh for every cell

Experiment sets:
  calibration  probes only, no compilation, --repeat 5; always run first and last, so that probe drift over
               the session is visible. Requesting it alone runs only those two cells.
  serial       parallelism 1, one pinned core, a small deterministic sample, every module compiled 3 times:
               the cleanest per-module distribution, unaffected by the scheduling of concurrent compilations
  scaling      parallelism ${SCALING_PARALLELISM[*]} (plus the native core count) with the heap scaled per thread,
               to find where the machine stops scaling
  pinned       the reference configuration of the agent comparison: --cores 8 --parallelism 8 --heap 12g,
               full module set, --repeat 3
  native       --native --heap-per-thread 1g, full module set: what the developer actually experiences
  suites       the pinned configuration for both generated suites, IntelliJ and Kotlin
  all          calibration, serial, scaling, pinned
EOF
}

all_set_names() {
    echo "calibration | serial | scaling | pinned | native | suites | all"
}

has_set() {
    local wanted="$1" name
    for name in ${SETS[@]+"${SETS[@]}"}; do
        if [[ "$name" == "$wanted" ]]; then
            return 0
        fi
        # "all" deliberately leaves out native and suites: they compile the full model again and would double
        # the session time without adding anything to the agent comparison
        if [[ "$name" == "all" ]]; then
            case "$wanted" in
                serial|scaling|pinned) return 0 ;;
            esac
        fi
    done
    return 1
}

native_cpu_count() {
    if [[ "$(uname)" == "Darwin" ]]; then
        sysctl -n hw.ncpu
    else
        getconf _NPROCESSORS_ONLN
    fi
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        -l|--label) LABEL="$2"; shift 2 ;;
        -s|--set) SETS+=("$2"); shift 2 ;;
        -o|--out-dir) OUT_DIR="$2"; shift 2 ;;
        -t|--tests) TESTS="$2"; shift 2 ;;
        --calibration-test) CALIBRATION_TEST="$2"; shift 2 ;;
        --no-prebuild) PREBUILD=0; shift ;;
        --no-caffeinate) CAFFEINATE=0; shift ;;
        -y|--yes) shift ;;
        --dry-run) DRY_RUN=1; shift ;;
        -h|--help) usage; exit 0 ;;
        --) shift; PASSTHROUGH=("$@"); break ;;
        *) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
    esac
done

if [[ -z "$LABEL" ]]; then
    echo "--label is required: it identifies the machine and the configuration, e.g. m5max-falcon-on" >&2
    exit 2
fi
if [[ ${#SETS[@]} -eq 0 ]]; then
    echo "--set is required, one of: $(all_set_names)" >&2
    exit 2
fi
for name in "${SETS[@]}"; do
    case "$name" in
        calibration|serial|scaling|pinned|native|suites|all) ;;
        *) echo "Unknown experiment set: $name (expected $(all_set_names))" >&2; exit 2 ;;
    esac
done
[[ -n "$OUT_DIR" ]] || OUT_DIR="$REPO_ROOT/tmp/fir-bench"
[[ -x "$RUN_BENCH" ]] || { echo "run-bench.sh not found next to this script: $RUN_BENCH" >&2; exit 2; }

TESTS_ARGS=()
[[ -z "$TESTS" ]] || TESTS_ARGS=("--tests" "$TESTS")

# ---------------------------------------------------------------------------------------------------------------
# The matrix. A cell is a label plus the run-bench.sh arguments; the unit separator keeps the arguments intact,
# an argument such as the calibration test filter contains characters that must not be re-split.
# ---------------------------------------------------------------------------------------------------------------
CELLS=()
CELL_ESTIMATES=()

add_cell() {
    local seconds="$1" cell="$2"
    shift 2
    local joined="$cell" arg
    for arg in "$@"; do
        joined+=$'\x1f'"$arg"
    done
    CELLS+=("$joined")
    CELL_ESTIMATES+=("$seconds")
}

estimate_compile() {
    awk -v f="$1" -v p="$2" -v compileRepeat="$3" -v repeat="$4" \
        -v total="$FULL_SUITE_SERIAL_SECONDS" -v overhead="$PARALLEL_OVERHEAD" \
        'BEGIN { printf "%.0f", f * total / p * (p > 1 ? overhead : 1) * compileRepeat * repeat }'
}

add_calibration_cell() {
    add_cell "$((CALIBRATION_SECONDS * 5))" "$1" \
        --tests "$CALIBRATION_TEST" --probe-suite full --repeat 5
}

# The probes are cheap and are the only way to tell a drifting machine from a real difference
add_calibration_cell "calib-pre"

if has_set serial; then
    # 5% of the modules, three compilations each, three repetitions of the cell: ~20 minutes for a distribution
    # that is not polluted by 8 compilations competing for the memory bandwidth
    add_cell "$(estimate_compile 0.05 1 3 3)" "p1-s0.05-r3" \
        --serial --sample 0.05 --compile-repeat 3 --heap 4g --repeat 3 ${TESTS_ARGS[@]+"${TESTS_ARGS[@]}"}
fi

if has_set scaling; then
    parallelisms=("${SCALING_PARALLELISM[@]}")
    native="$(native_cpu_count)"
    if [[ "$native" -gt "${SCALING_PARALLELISM[${#SCALING_PARALLELISM[@]} - 1]}" ]]; then
        parallelisms+=("$native")
    fi
    for p in "${parallelisms[@]}"; do
        # The heap follows the thread count, otherwise the sweep measures the shrinking heap per compilation
        # instead of the scaling of the machine
        add_cell "$(estimate_compile 0.1 "$p" 1 1)" "scale-p$p-s0.1" \
            --cores "$p" --parallelism "$p" --heap-per-thread 1g --sample 0.1 \
            ${TESTS_ARGS[@]+"${TESTS_ARGS[@]}"}
    done
fi

if has_set pinned; then
    add_cell "$(estimate_compile 1 8 1 3)" "pinned-p8-full-r3" \
        --cores 8 --parallelism 8 --heap 12g --repeat 3 ${TESTS_ARGS[@]+"${TESTS_ARGS[@]}"}
fi

if has_set native; then
    add_cell "$(estimate_compile 1 "$(native_cpu_count)" 1 1)" "native-p$(native_cpu_count)-full" \
        --native --heap-per-thread 1g ${TESTS_ARGS[@]+"${TESTS_ARGS[@]}"}
fi

if has_set suites; then
    # Two independent module sets in the same pinned configuration: a difference that shows up in only one of
    # them is a property of that project model, not of the machine
    add_cell "$(estimate_compile 1 8 1 3)" "suite-intellij-p8-r3" \
        --cores 8 --parallelism 8 --heap 12g --repeat 3 --tests "IntelliJFullPipelineTestsGenerated"
    add_cell "$(estimate_compile 1 8 1 3)" "suite-kotlin-p8-r3" \
        --cores 8 --parallelism 8 --heap 12g --repeat 3 --tests "KotlinFullPipelineTestsGenerated"
fi

add_calibration_cell "calib-post"

# ---------------------------------------------------------------------------------------------------------------
# Execution
# ---------------------------------------------------------------------------------------------------------------
shell_quote() {
    local arg out=""
    for arg in "$@"; do
        if [[ "$arg" =~ ^[A-Za-z0-9_./=:,+-]+$ ]]; then
            out+="$arg "
        else
            out+="'${arg//\'/\'\\\'\'}' "
        fi
    done
    printf '%s' "${out% }"
}

cell_command() {
    local cell="$1"
    shift
    local run_label="${LABEL}__${cell}"
    # --no-prebuild everywhere: the distribution is built once, before the first cell, and is not measured
    CELL_COMMAND=("$RUN_BENCH" --label "$run_label" --out-dir "$OUT_DIR" --no-prebuild "$@")
    if [[ ${#PASSTHROUGH[@]} -gt 0 ]]; then
        CELL_COMMAND+=("${PASSTHROUGH[@]}")
    fi
}

format_duration() {
    awk -v s="$1" 'BEGIN {
        minutes = int((s + 59) / 60)
        if (minutes < 60) printf "%d min", minutes
        else printf "%dh %02dm", minutes / 60, minutes % 60
    }'
}

TOTAL_SECONDS=0
for estimate in "${CELL_ESTIMATES[@]}"; do
    TOTAL_SECONDS=$((TOTAL_SECONDS + estimate))
done

echo "label:      $LABEL"
echo "sets:       ${SETS[*]}"
echo "out dir:    $OUT_DIR"
echo "cells:      ${#CELLS[@]}"
echo "estimate:   $(format_duration "$TOTAL_SECONDS") of measured time, plus the prebuild"
echo "            This is a rough extrapolation from ~2900 s for the full model on a single thread; a machine"
echo "            with an endpoint security agent, a cold file cache or a thermal limit takes noticeably longer."
echo

if [[ "$DRY_RUN" == "1" ]]; then
    echo "Plan (in this order, one after another, never in parallel):"
    echo
    index=0
    for entry in "${CELLS[@]}"; do
        IFS=$'\x1f' read -r -a parts <<< "$entry"
        cell_command "${parts[@]}"
        echo "# ${parts[0]}  (~$(format_duration "${CELL_ESTIMATES[$index]}"))"
        echo "$(shell_quote "${CELL_COMMAND[@]}")"
        echo
        index=$((index + 1))
    done
    exit 0
fi

# One caffeinate for the whole session: a display sleep in the middle of the matrix invalidates every cell after it
if [[ "$CAFFEINATE" == "1" && -z "${FIR_BENCH_CAFFEINATED:-}" ]] && command -v caffeinate >/dev/null 2>&1; then
    export FIR_BENCH_CAFFEINATED=1
    exec caffeinate -dimsu "${BASH_SOURCE[0]}" ${ORIGINAL_ARGS[@]+"${ORIGINAL_ARGS[@]}"}
fi

cd "$REPO_ROOT"
mkdir -p "$OUT_DIR"
INDEX_FILE="$OUT_DIR/experiments-$(date +%Y%m%d-%H%M%S)-$LABEL.txt"
{
    echo "date: $(date -Iseconds)"
    echo "host: $(hostname)"
    echo "label: $LABEL"
    echo "sets: ${SETS[*]}"
    echo "cells: ${#CELLS[@]}"
    echo "estimate: $(format_duration "$TOTAL_SECONDS")"
    echo
} > "$INDEX_FILE"
echo "index:      $INDEX_FILE"

if [[ "$PREBUILD" == "1" ]]; then
    echo
    echo "=== prebuilding the distribution and the test classes once (not measured)"
    ./gradlew --max-workers="$(getconf _NPROCESSORS_ONLN)" \
        :dist :plugins:compose-compiler-plugin:compiler-hosted:jar \
        :compiler:fir:modularized-tests:testClasses
fi

# The harness names its run directory <timestamp>-<label>, and --repeat adds an -rN suffix; the marker file makes
# sure that a directory of an earlier session with the same label is not attributed to this cell
run_dirs_since() {
    local run_label="$1" marker="$2"
    find "$OUT_DIR" -maxdepth 1 -type d -newer "$marker" \
        \( -name "*-$run_label" -o -name "*-$run_label-r*" \) 2>/dev/null | sort | tr '\n' ' '
}

SUCCEEDED=0
FAILED=0
CELL_INDEX=0
for entry in "${CELLS[@]}"; do
    IFS=$'\x1f' read -r -a parts <<< "$entry"
    cell="${parts[0]}"
    cell_command "${parts[@]}"
    run_label="${LABEL}__${cell}"
    CELL_INDEX=$((CELL_INDEX + 1))

    echo
    echo "############################################################################################"
    echo "### [$CELL_INDEX/${#CELLS[@]}] $run_label  (~$(format_duration "${CELL_ESTIMATES[$((CELL_INDEX - 1))]}"))"
    echo "### $(shell_quote "${CELL_COMMAND[@]}")"
    echo "############################################################################################"

    marker="$(mktemp -t fir-experiments-marker)"
    started="$(date -Iseconds)"
    status=0
    # A failing cell is not fatal: some modules of the model never compile, and a cell may hit an OOM at a
    # parallelism the machine cannot take - the remaining cells are still worth having
    "${CELL_COMMAND[@]}" || status=$?
    finished="$(date -Iseconds)"
    dirs="$(run_dirs_since "$run_label" "$marker")"
    rm -f "$marker"

    {
        echo "cell: $cell"
        echo "  label:    $run_label"
        echo "  command:  $(shell_quote "${CELL_COMMAND[@]}")"
        echo "  start:    $started"
        echo "  end:      $finished"
        echo "  status:   $status"
        echo "  runDirs:  ${dirs:-none}"
        echo
    } >> "$INDEX_FILE"

    if [[ -n "$dirs" ]]; then
        SUCCEEDED=$((SUCCEEDED + 1))
    else
        FAILED=$((FAILED + 1))
        echo "WARNING: no run directory for $run_label (exit code $status); continuing with the next cell" >&2
    fi
done

echo
echo "Done. $SUCCEEDED of ${#CELLS[@]} cells produced a run directory, $FAILED did not."
echo "Index: $INDEX_FILE"
echo "Compare two runs with:"
echo "  $SCRIPT_DIR/compare-bench-runs.py <baseline-run-dir> <run-dir>"
echo "Repeat the very same command line on the other machine, changing only --label."

[[ "$SUCCEEDED" -gt 0 ]] || exit 1
