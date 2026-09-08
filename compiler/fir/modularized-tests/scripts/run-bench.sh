#!/usr/bin/env bash
#
# Runs the generated modularized full pipeline tests as a benchmark with a *pinned* configuration.
#
# The point of pinning: a comparison of two machines (or of the same machine with and without an endpoint
# security agent) is meaningless unless the JVM sees exactly the same number of CPUs, the same heap, the same
# GC and the same test parallelism. Otherwise the difference in the GC pressure and in the scheduling is much
# larger than the effect being measured - see BENCHMARK_HARNESS.md.
#
# Usage:
#   scripts/run-bench.sh --label m5max-falcon-on
#   scripts/run-bench.sh --label m2max-falcon-off --cores 8 --heap 12g --repeat 3
#
# Run the very same command line on both machines, changing only --label.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

# ---------------------------------------------------------------------------------------------------------------
# Defaults. They are deliberately conservative: 8 cores is available on every Apple Silicon machine, and the
# heap is large enough to keep the full GC out of the picture at this parallelism.
# ---------------------------------------------------------------------------------------------------------------
LABEL=""
TESTS="IntelliJFullPipelineTestsGenerated"
CORES=8
PARALLELISM=""
HEAP="12g"
GC="Parallel"
REPEAT=1
OUT_DIR=""
WARMUP=0
PREBUILD=1
OFFLINE=1
CAFFEINATE=1
PURGE=0
TEST_INSTRUMENTER=1
DRY_RUN=0
EXTRA_ARGS=()

usage() {
    sed -n '3,14p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
    cat <<EOF

Options:
  -l, --label NAME        configuration label, part of the run directory name (required)
  -t, --tests PATTERN     test filter passed to Gradle --tests (default: $TESTS)
  -c, --cores N           pinned number of CPUs visible to the JVM (default: $CORES)
  -p, --parallelism N     pinned number of concurrent tests (default: same as --cores)
  -x, --heap SIZE         pinned -Xms and -Xmx, JVM notation (default: $HEAP)
  -g, --gc NAME           Parallel | G1 | none (default: $GC)
  -r, --repeat N          run the suite N times, labels get an -rN suffix (default: $REPEAT)
  -o, --out-dir DIR       root directory for the run directories (default: <repo>/tmp/fir-bench)
      --warmup            do a throwaway run first, to warm the file cache up
      --no-prebuild       do not build :dist and the test classes beforehand
      --no-offline        allow Gradle to access the network during the measured run
      --no-caffeinate     do not prevent the machine from sleeping
      --purge             drop the file system cache before every run (needs sudo, macOS only)
      --no-test-instrumenter  disable the test instrumentation agent of the build
      --dry-run           only print what would be executed
  --                      everything after it is passed to Gradle as is
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        -l|--label) LABEL="$2"; shift 2 ;;
        -t|--tests) TESTS="$2"; shift 2 ;;
        -c|--cores) CORES="$2"; shift 2 ;;
        -p|--parallelism) PARALLELISM="$2"; shift 2 ;;
        -x|--heap) HEAP="$2"; shift 2 ;;
        -g|--gc) GC="$2"; shift 2 ;;
        -r|--repeat) REPEAT="$2"; shift 2 ;;
        -o|--out-dir) OUT_DIR="$2"; shift 2 ;;
        --warmup) WARMUP=1; shift ;;
        --no-prebuild) PREBUILD=0; shift ;;
        --no-offline) OFFLINE=0; shift ;;
        --no-caffeinate) CAFFEINATE=0; shift ;;
        --purge) PURGE=1; shift ;;
        --no-test-instrumenter) TEST_INSTRUMENTER=0; shift ;;
        --dry-run) DRY_RUN=1; shift ;;
        -h|--help) usage; exit 0 ;;
        --) shift; EXTRA_ARGS=("$@"); break ;;
        *) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
    esac
done

if [[ -z "$LABEL" ]]; then
    echo "--label is required: it identifies the configuration, e.g. m5max-falcon-on" >&2
    exit 2
fi
[[ -n "$PARALLELISM" ]] || PARALLELISM="$CORES"
[[ -n "$OUT_DIR" ]] || OUT_DIR="$REPO_ROOT/tmp/fir-bench"

mkdir -p "$OUT_DIR"

# ---------------------------------------------------------------------------------------------------------------
# The pinned JVM arguments of the test process.
#
# ActiveProcessorCount is the important one: the JVM derives the ForkJoinPool size, the number of GC threads and
# the JIT thread count from it, and the test engine derives its parallelism from Runtime.availableProcessors().
# ---------------------------------------------------------------------------------------------------------------
JVM_ARGS="-XX:ActiveProcessorCount=$CORES"
JVM_ARGS="$JVM_ARGS -XX:ParallelGCThreads=$CORES"
# Touch the whole heap upfront so that the page faults are not counted into the measured compilations, and do not
# let the heap grow lazily - a growing heap makes the early modules look slower than the late ones
JVM_ARGS="$JVM_ARGS -XX:+AlwaysPreTouch"
# Same code cache behaviour on both machines
JVM_ARGS="$JVM_ARGS -XX:-UseCodeCacheFlushing"

GRADLE_ARGS=(
    ":compiler:fir:modularized-tests:test"
    "--tests" "$TESTS"
    "--rerun"
    # Do not let Gradle itself compete with the measured process
    "--max-workers=1"
    "-Pkotlin.test.xmx=$HEAP"
    "-Pkotlin.test.xms=$HEAP"
    "-Pkotlin.test.junit5.maxParallelForks=$PARALLELISM"
)
if [[ "$GC" != "none" ]]; then
    GRADLE_ARGS+=("-Pkotlin.test.garbage.collector=$GC")
fi
if [[ "$TEST_INSTRUMENTER" == "0" ]]; then
    GRADLE_ARGS+=("-Pkotlin.test.instrumentation.disable=true")
fi
if [[ "$OFFLINE" == "1" ]]; then
    # The network is monitored by the very agents being measured; keep Gradle off it during the run
    GRADLE_ARGS+=("--offline")
fi

check_active_processor_count() {
    local java_home
    java_home="$(/usr/libexec/java_home -v 1.8 2>/dev/null || true)"
    [[ -n "$java_home" ]] || return 0
    if ! "$java_home/bin/java" -XX:ActiveProcessorCount=2 -version >/dev/null 2>&1; then
        echo "WARNING: $java_home does not support -XX:ActiveProcessorCount," >&2
        echo "         the number of CPUs cannot be pinned and the runs will not be comparable." >&2
    fi
}

# The state of the machine that the harness itself does not record, and that silently invalidates a comparison:
# thermal throttling, low power mode, battery power, a nearly full disk.
write_env_report() {
    local file="$1"
    {
        echo "date: $(date -Iseconds)"
        echo "host: $(hostname)"
        echo "label: $LABEL"
        echo "pinned: cores=$CORES parallelism=$PARALLELISM heap=$HEAP gc=$GC"
        echo "jvmArgs: $JVM_ARGS"
        echo "gradleArgs: ${GRADLE_ARGS[*]} ${EXTRA_ARGS[*]+${EXTRA_ARGS[*]}}"
        echo "uptime: $(uptime)"
        if [[ "$(uname)" == "Darwin" ]]; then
            echo "cpu: $(sysctl -n machdep.cpu.brand_string)"
            echo "hw.ncpu: $(sysctl -n hw.ncpu)"
            echo "hw.perflevel0.logicalcpu: $(sysctl -n hw.perflevel0.logicalcpu 2>/dev/null || echo n/a)"
            echo "hw.perflevel1.logicalcpu: $(sysctl -n hw.perflevel1.logicalcpu 2>/dev/null || echo n/a)"
            echo "hw.memsize: $(sysctl -n hw.memsize)"
            echo "os: $(sw_vers -productVersion) ($(uname -r))"
            echo "--- pmset -g (lowpowermode, powermode must match on both machines)"
            pmset -g 2>/dev/null || true
            echo "--- pmset -g batt"
            pmset -g batt 2>/dev/null || true
            echo "--- thermal"
            pmset -g therm 2>/dev/null || true
            echo "--- system extensions (the endpoint security agents)"
            systemextensionsctl list 2>/dev/null || true
            echo "--- security agent processes"
            pgrep -fl 'falcon|kandji|littlesnitch|lulu' 2>/dev/null || echo "none"
            echo "--- vm_stat"
            vm_stat 2>/dev/null | head -8 || true
        fi
        echo "--- df"
        df -h "$REPO_ROOT" 2>/dev/null || true
        echo "--- java"
        ./gradlew --version 2>/dev/null | sed -n '1,12p' || true
    } > "$file" 2>&1
}

# The harness names its run directory <timestamp>-<label>, so it is only known after the JVM has started
latest_run_dir_for() {
    local run_label="$1"
    # shellcheck disable=SC2012
    ls -1dt "$OUT_DIR"/*-"$run_label" 2>/dev/null | head -1
}

run_once() {
    local run_label="$1"
    local args=(
        "${GRADLE_ARGS[@]}"
        "-Pfir.bench.instrumentation.label=$run_label"
        "-Pfir.bench.instrumentation.dir=$OUT_DIR"
        "-Pfir.modularized.jvm.args=$JVM_ARGS"
    )
    if [[ ${#EXTRA_ARGS[@]} -gt 0 ]]; then
        args+=("${EXTRA_ARGS[@]}")
    fi

    echo
    echo "=== $run_label"
    echo "    ./gradlew ${args[*]}"
    if [[ "$DRY_RUN" == "1" ]]; then
        return 0
    fi

    if [[ "$PURGE" == "1" && "$(uname)" == "Darwin" ]]; then
        echo "    dropping the file system cache (sudo purge)"
        sync
        sudo purge || echo "WARNING: purge failed, the file cache state differs between the runs" >&2
    fi

    local log
    log="$(mktemp -t fir-bench-gradle)"
    local status=0
    if [[ "$CAFFEINATE" == "1" ]] && command -v caffeinate >/dev/null 2>&1; then
        caffeinate -dimsu ./gradlew "${args[@]}" 2>&1 | tee "$log" || status=$?
    else
        ./gradlew "${args[@]}" 2>&1 | tee "$log" || status=$?
    fi

    local run_dir
    run_dir="$(latest_run_dir_for "$run_label")"
    if [[ -n "$run_dir" ]]; then
        write_env_report "$run_dir/run-env.txt"
        mv "$log" "$run_dir/gradle.log"
        echo "    run directory: $run_dir"
    else
        rm -f "$log"
        echo "WARNING: no run directory found for the label $run_label; was the instrumentation enabled?" >&2
    fi
    # A failing test is expected (some modules of the model do not compile), only report the exit code
    [[ "$status" == "0" ]] || echo "    gradle exit code: $status (some modules are expected to fail)"
}

cd "$REPO_ROOT"
check_active_processor_count

if [[ "$PREBUILD" == "1" && "$DRY_RUN" == "0" ]]; then
    echo "=== prebuilding the distribution and the test classes (not measured)"
    ./gradlew --max-workers="$(getconf _NPROCESSORS_ONLN)" \
        :dist :plugins:compose-compiler-plugin:compiler-hosted:jar \
        :compiler:fir:modularized-tests:testClasses
fi

if [[ "$WARMUP" == "1" ]]; then
    run_once "$LABEL-warmup"
fi

for ((i = 1; i <= REPEAT; i++)); do
    if [[ "$REPEAT" == "1" ]]; then
        run_once "$LABEL"
    else
        run_once "$LABEL-r$i"
    fi
done

echo
echo "Done. Compare two runs with:"
echo "  $SCRIPT_DIR/compare-bench-runs.py <baseline-run-dir> <run-dir>"
echo "Repeat the very same command line on the other machine, changing only --label."
