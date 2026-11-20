#!/usr/bin/env python3
"""
Summarize one or more gradle-profiler benchmark.csv files.

Usage:
    python3 scripts/summarize_benchmark.py path/to/benchmark.csv [more.csv ...]

Reads gradle-profiler's CSV output (per-iteration task wall times) and prints
a single table with N, mean, stderr, and 95% CI per scenario. When multiple
CSV files are given, scenarios from all files are merged into one table
(scenarios with identical titles have their measurements concatenated).

gradle-profiler benchmark.csv shape:
    Row 0: scenario,<title-1>,<title-2>,...
    Row 1: version,<gradle-version>,...
    Row 2: tasks,<tasks>,...
    Row 3: value,total execution time,...
    Row 4+: <build-label>,<ms>,<ms>,...

Only "measured build #N" rows are used; warm-up rows are discarded.
"""

import csv
import math
import sys
from typing import Dict, List, TextIO


def parse_benchmark_csv(stream: TextIO) -> Dict[str, List[float]]:
    """Parse a gradle-profiler benchmark.csv stream.

    Returns a dict mapping scenario title to a list of measured-build durations (ms).
    """
    rows = list(csv.reader(stream))
    if len(rows) < 5:
        raise ValueError(
            f"benchmark.csv has only {len(rows)} rows; expected at least 5 "
            "(scenario header + 3 metadata rows + at least one measured build)"
        )

    scenario_titles = rows[0][1:]  # skip the "scenario" label in column 0
    if not scenario_titles:
        raise ValueError("No scenarios found in benchmark.csv")

    result: Dict[str, List[float]] = {title: [] for title in scenario_titles}

    for row in rows[4:]:  # rows 0-3 are metadata
        label = row[0]
        if not label.startswith("measured build"):
            continue
        for i, value_str in enumerate(row[1:]):
            value_str = value_str.strip()
            if not value_str:
                continue
            result[scenario_titles[i]].append(float(value_str))

    return result


def compute_stats(values: List[float]) -> Dict[str, float]:
    """Compute mean, stddev (sample), stderr, and 95% CI bounds.

    For n=1, stddev/stderr are returned as 0.0 (CI degenerates to [mean, mean]).
    """
    n = len(values)
    if n == 0:
        return {
            "n": 0, "mean": 0.0, "stddev": 0.0, "stderr": 0.0,
            "ci95_low": 0.0, "ci95_high": 0.0,
        }
    mean = sum(values) / n
    if n == 1:
        return {
            "n": 1, "mean": mean, "stddev": 0.0, "stderr": 0.0,
            "ci95_low": mean, "ci95_high": mean,
        }
    variance = sum((v - mean) ** 2 for v in values) / (n - 1)
    stddev = math.sqrt(variance)
    stderr = stddev / math.sqrt(n)
    half = 1.96 * stderr
    return {
        "n": n, "mean": mean, "stddev": stddev, "stderr": stderr,
        "ci95_low": mean - half, "ci95_high": mean + half,
    }


def render_table(data: Dict[str, List[float]]) -> str:
    """Render the per-scenario stats table as a string."""
    if not data:
        return "(no scenarios)\n"

    rows = []
    for title, values in data.items():
        s = compute_stats(values)
        rows.append({
            "scenario": title,
            "n": s["n"],
            "mean": s["mean"],
            "stderr": s["stderr"],
            "ci_low": s["ci95_low"],
            "ci_high": s["ci95_high"],
        })

    # Compute column widths
    scenario_w = max(len("Scenario"), max(len(r["scenario"]) for r in rows))
    n_w = 3
    mean_w = 10
    stderr_w = 8
    ci_w = 22

    header = (
        f"{'Scenario':<{scenario_w}} | {'N':>{n_w}} | "
        f"{'Mean (ms)':>{mean_w}} | {'Stderr':>{stderr_w}} | "
        f"{'95% CI (ms)':>{ci_w}}"
    )
    sep = "-" * len(header)

    out = [header, sep]
    for r in rows:
        ci_str = f"[{r['ci_low']:.1f}, {r['ci_high']:.1f}]"
        out.append(
            f"{r['scenario']:<{scenario_w}} | {r['n']:>{n_w}} | "
            f"{r['mean']:>{mean_w}.1f} | {r['stderr']:>{stderr_w}.1f} | "
            f"{ci_str:>{ci_w}}"
        )

    return "\n".join(out) + "\n"


def combine_csv_files(paths: List[str]) -> Dict[str, List[float]]:
    """Parse multiple benchmark.csv files and merge their scenarios into one dict.

    Scenarios are emitted in input order. If the same scenario title appears in
    multiple files, the measurements are concatenated.
    """
    combined: Dict[str, List[float]] = {}
    for path in paths:
        with open(path, "r") as f:
            data = parse_benchmark_csv(f)
        for title, values in data.items():
            if title in combined:
                combined[title].extend(values)
            else:
                combined[title] = list(values)
    return combined


def main(argv: List[str]) -> int:
    if len(argv) < 2:
        print(
            f"Usage: python3 {argv[0]} path/to/benchmark.csv [more.csv ...]",
            file=sys.stderr,
        )
        return 2
    try:
        data = combine_csv_files(argv[1:])
    except (FileNotFoundError, ValueError) as e:
        print(f"Error: {e}", file=sys.stderr)
        return 1
    print(render_table(data), end="")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
