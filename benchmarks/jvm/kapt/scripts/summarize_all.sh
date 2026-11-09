#!/usr/bin/env bash
# Print one combined summary table for all 9 scenarios from existing benchmark
# results in profile-out/<workload>_<size>/benchmark.csv. Does not run any
# benchmarks; only reads CSVs from disk.
#
# Use this after run_all_benchmarks.sh (or after a manual run) to re-render
# the combined table without re-running anything.
#
# Usage (from the repository root):
#   ./scripts/summarize_all.sh

set -euo pipefail

cd "$(dirname "$0")/.."

if ! command -v python3 >/dev/null 2>&1; then
    echo "Error: python3 is not on PATH." >&2
    exit 1
fi

WORKLOADS=(synthetic dagger mapstruct)
SIZES=(small medium large)

csv_files=()
missing=()
for w in "${WORKLOADS[@]}"; do
    for s in "${SIZES[@]}"; do
        path="profile-out/${w}_${s}/benchmark.csv"
        if [[ -f "$path" ]]; then
            csv_files+=("$path")
        else
            missing+=("$path")
        fi
    done
done

if (( ${#missing[@]} > 0 )); then
    echo "Note: ${#missing[@]} scenario(s) not yet run:" >&2
    for p in "${missing[@]}"; do
        echo "  - $p" >&2
    done
    echo "" >&2
fi

if (( ${#csv_files[@]} == 0 )); then
    echo "Error: no benchmark.csv files found under profile-out/." >&2
    echo "Run ./scripts/run_all_benchmarks.sh (or any gradle-profiler --benchmark invocation) first." >&2
    exit 1
fi

python3 scripts/summarize_benchmark.py "${csv_files[@]}"
