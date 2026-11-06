#!/usr/bin/env bash
# Run kapt-benchmark scenarios and print a per-scenario summary table.
#
# Requirements:
#   - gradle-profiler on PATH (https://github.com/gradle/gradle-profiler/releases)
#   - python3 (for the summary script)
#
# Total wall time depends on the machine and on how many cells are selected; the 9-cell default
# suite is roughly half an hour on a fast workstation. Time it on your own hardware before
# planning around it.

set -euo pipefail

cd "$(dirname "$0")/.."

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
gradle-profiler --benchmark \
    --output-dir profile-out \
    --scenario-file kapt.scenarios \
    "$@"

python3 scripts/summarize_benchmark.py "profile-out/benchmark.csv"
