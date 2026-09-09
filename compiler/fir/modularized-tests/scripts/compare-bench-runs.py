#!/usr/bin/env python3
"""Compares two runs of the generated isolated modularized tests instrumented by ModularizedTestInstrumentation.

Usage:
    compare-bench-runs.py [--json <file>] [--top N] [--weights cpu=0.6,mem=0.35,disk=0.05]
                          [--no-sample-check] [--parallelism-1] <baseline-run-dir> <run-dir>

Each run directory is produced by the test harness and contains `manifest.json`, `compilations.jsonl` and
`summary.json` (see modularizedTestInstrumentation.kt).

The benchmark exists to measure the overhead of the system-wide endpoint security agents (CrowdStrike Falcon and
friends), which shows up as extra *system* time and as slower syscalls, so the statistics have to be honest:

  * The headline numbers are PAIRED PER-MODULE RATIOS: for every module compiled successfully in both runs the
    ratio `run / baseline` is computed, and the distribution of these ratios is reported (n, p10, p25, median,
    p75, p90, p99 and the geometric mean). The median is robust, and the geometric mean is the right "average"
    for ratios. Sums over all the modules are still printed, but in a separate section, because they are
    dominated by a handful of heavy modules and easily produce a misleading headline.
  * A ratio greater than 1 means the second run is slower.
  * The machine speed is divided out by a MULTI-DIMENSIONAL calibration (see `calibrate.py`): a PREDICTED ratio
    is built from the cpu, memory and disk probe dimensions, and the number to look at is the RESIDUAL
    `measured / predicted` - the part of the difference that raw machine speed does not explain. A single cpu
    probe cannot do this: the legacy `cpu` probe is a serial dependency chain and reported an M5 Max and an M2
    Max as equal (0.98) where Geekbench single-core says 1.71x.
  * Any difference in the configuration, in the sampling, in the observed concurrency or in the GC time
    invalidates the comparison, so those are checked first and reported loudly.
  * When the modules are compiled several times in a row (`fir.bench.compile.repeat`) the records carry an
    `iteration`, and then the comparison is additionally done on the WARM records only, plus a cold/warm ratio
    per run which separates the file-cache and JIT warm-up from the steady state.
"""

import json
import math
import os
import re
import sys

# The probe taxonomy (which probe measures which dimension, how the per-operation value is derived, which
# direction is "slower") lives in `calibrate.py` next to this script, and the multi-dimensional calibration
# section below is that script's comparison mode applied to the two run directories.
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
try:
    import calibrate
except ImportError as import_error:  # the script still works, only the calibration section is lost
    calibrate = None
    CALIBRATE_IMPORT_ERROR = import_error

NAN = float("nan")

# The workload model of the calibration section: an ASSUMPTION about the mixture Kotlin compilation is, not a
# measurement. Kotlin compilation is pointer chasing over a large object graph plus class-file reads.
DEFAULT_WEIGHTS = (("cpu", 0.60), ("mem", 0.35), ("disk", 0.05))

# The probe `kind` each weight is fed from.
WEIGHT_KINDS = {"cpu": "cpu", "mem": "memory", "disk": "disk"}

# `testConfiguration` keys that must be identical for the two runs to be comparable at all.
SAMPLE_KEYS = ("fir.bench.sample.fraction", "fir.bench.sample.seed")

# How much the compiled module sets may differ (symmetric difference over union) at an equal sample fraction.
MODULE_SET_TOLERANCE = 0.02

# How far the observed mean concurrency may be from 1.0 for the run to count as single-threaded.
SINGLE_THREAD_TOLERANCE = 0.10

# Percentiles of the per-module ratio distribution that are printed for every metric.
PERCENTILES = (0.10, 0.25, 0.50, 0.75, 0.90, 0.99)

# `phases.total.userNanos` above this multiple of the wall time is impossible and marks the old broken harness.
BROKEN_PHASE_FACTOR = 4.0
BROKEN_PHASE_SHARE = 0.05

warnings = []


def warn(message):
    warnings.append(message)
    print("!! " + message)


# ---------------------------------------------------------------------------------------------- reading the input

def read_json(path):
    try:
        with open(path) as file:
            return json.load(file)
    except (IOError, OSError) as error:
        print("!! cannot read %s: %s" % (path, error))
        return {}
    except ValueError as error:
        print("!! cannot parse %s: %s" % (path, error))
        return {}


def read_records(run_dir):
    """`compilations.jsonl` is a concatenation of pretty-printed JSON objects, so it cannot be read line by line.

    Returns `model -> [records]` (a module is compiled more than once when `fir.bench.compile.repeat` is set, and
    then every record carries a 0-based `iteration`), the number of records skipped because the compilation
    failed, and the set of every module seen including the failed ones (needed for the sample check).
    """
    groups = {}
    seen = set()
    skipped = 0
    path = os.path.join(run_dir, "compilations.jsonl")
    try:
        with open(path) as file:
            content = file.read()
    except (IOError, OSError) as error:
        print("!! cannot read %s: %s" % (path, error))
        return groups, skipped, seen
    decoder = json.JSONDecoder()
    index = 0
    while index < len(content):
        while index < len(content) and content[index].isspace():
            index += 1
        if index >= len(content):
            break
        try:
            record, index = decoder.raw_decode(content, index)
        except ValueError as error:
            print("!! %s: stopped parsing at offset %d: %s" % (path, index, error))
            break
        seen.add(record.get("model"))
        if record.get("exitCode") == "OK":
            groups.setdefault(record.get("model"), []).append(record)
        else:
            skipped += 1
    return groups, skipped, seen


def iteration_of(record):
    value = record.get("iteration")
    return value if isinstance(value, (int, float)) else None


def has_iterations(groups):
    """True when the harness recorded the repetition index, i.e. `fir.bench.compile.repeat` was in effect."""
    return any(iteration_of(record) is not None
               for records in groups.values() for record in records)


def repeated_models(groups):
    return sum(1 for records in groups.values() if len(records) > 1)


def select_records(groups, warm_only=False):
    """One representative record per module: the one with the MEDIAN `wallNanos` among the selected iterations.

    A whole record is picked rather than a per-field median so that every field of a pair stays self-consistent.
    Without repetitions this returns the single record of the module, so the numbers of the old runs, which have
    exactly one compilation per module, are unchanged.
    """
    selected = {}
    for model, records in groups.items():
        candidates = [record for record in records if (iteration_of(record) or 0) > 0] if warm_only else records
        with_wall = [record for record in candidates if record.get("wallNanos")]
        candidates = with_wall or candidates
        if not candidates:
            continue
        ordered = sorted(candidates, key=lambda record: record.get("wallNanos") or 0)
        selected[model] = ordered[(len(ordered) - 1) // 2]
    return selected


# ------------------------------------------------------------------------------------------------------ statistics

def percentile(values, quantile):
    """Linear interpolation between the closest ranks; `values` must be sorted."""
    if not values:
        return NAN
    if len(values) == 1:
        return values[0]
    position = (len(values) - 1) * quantile
    low = int(math.floor(position))
    high = int(math.ceil(position))
    if low == high:
        return values[low]
    return values[low] + (values[high] - values[low]) * (position - low)


def geometric_mean(values):
    """Computed in the log space: the geometric mean is the only meaningful average of ratios."""
    positive = [value for value in values if value > 0]
    if not positive:
        return NAN
    return math.exp(sum(math.log(value) for value in positive) / len(positive))


def median(values):
    return percentile(sorted(values), 0.5)


def ratio(value, baseline):
    return value / baseline if baseline else NAN


def collect(common, baseline_records, records, getter, skip_zero_run=False):
    """Builds the paired samples. Pairs with a zero (or missing) baseline are skipped: no ratio is defined."""
    pairs = []
    for model in common:
        baseline_value = getter(baseline_records[model])
        value = getter(records[model])
        if baseline_value is None or value is None:
            continue
        if not baseline_value:
            continue
        if skip_zero_run and not value:
            continue
        pairs.append((baseline_value, value, model))
    return pairs


def statistics(pairs):
    baseline_values = [pair[0] for pair in pairs]
    values = [pair[1] for pair in pairs]
    ratios = sorted(ratio(pair[1], pair[0]) for pair in pairs)
    result = {
        "n": len(pairs),
        "baselineMedian": median(baseline_values),
        "runMedian": median(values),
        "geomean": geometric_mean(ratios),
    }
    for quantile in PERCENTILES:
        result["p%02d" % int(round(quantile * 100))] = percentile(ratios, quantile)
    return result


# -------------------------------------------------------------------------------------------------- formatting

def format_nanos(nanos):
    if nanos is None or nanos != nanos:
        return "n/a"
    value = abs(nanos)
    if value >= 1e9:
        return "%.3f s" % (nanos / 1e9)
    if value >= 1e6:
        return "%.1f ms" % (nanos / 1e6)
    if value >= 1e3:
        return "%.1f us" % (nanos / 1e3)
    return "%.0f ns" % nanos


def format_millis(millis):
    if millis is None or millis != millis:
        return "n/a"
    if abs(millis) >= 1000:
        return "%.2f s" % (millis / 1e3)
    return "%.0f ms" % millis


def format_count(count):
    if count is None or count != count:
        return "n/a"
    return "%.0f" % count


def format_percent(share):
    if share is None or share != share:
        return "n/a"
    return "%.2f %%" % (share * 100)


FORMATTERS = {"nanos": format_nanos, "millis": format_millis, "count": format_count, "percent": format_percent}


def format_ratio(value):
    if value is None or value != value:
        return "  n/a "
    if value >= 1000:
        return "%6.0f" % value
    return "%6.3f" % value


HEADER = "%-44s %5s %11s %11s %7s %7s %7s %7s %7s %7s %7s" % (
    "metric", "n", "baseline", "run", "p10", "p25", "median", "p75", "p90", "p99", "gmean")


def print_header():
    print(HEADER)
    print("-" * len(HEADER))


def print_metric(name, stats, kind="nanos"):
    formatter = FORMATTERS[kind]
    print("%-44s %5d %11s %11s %s %s %s %s %s %s %s" % (
        name, stats["n"], formatter(stats["baselineMedian"]), formatter(stats["runMedian"]),
        format_ratio(stats["p10"]), format_ratio(stats["p25"]), format_ratio(stats["p50"]),
        format_ratio(stats["p75"]), format_ratio(stats["p90"]), format_ratio(stats["p99"]),
        format_ratio(stats["geomean"])))


def print_section(title):
    print("")
    print("=" * 118)
    print(title)
    print("=" * 118)


# ---------------------------------------------------------------------------------------------- configuration

GC_ARG = re.compile(r"^-XX:[+-]?Use\w*GC|^-XX:[+-]?(?:UseAdaptiveSizePolicy|ExplicitGCInvokesConcurrent)"
                    r"|^-XX:(?:NewRatio|MaxGCPauseMillis|ParallelGCThreads|ConcGCThreads)=")
PINNED_ARG = re.compile(r"^-Xmx|^-Xms|ActiveProcessorCount|^-Djunit\.jupiter\.execution\.parallel|^-Dfir\.bench\."
                        r"(?:parallel|threads)")


def interesting_jvm_args(manifest):
    """Only the arguments that can change the result are compared: heap, GC, cpu pinning and test parallelism."""
    selected = {}
    for argument in manifest.get("jvmArgs") or []:
        if not isinstance(argument, str):
            continue
        if GC_ARG.search(argument) or PINNED_ARG.search(argument):
            name, _, value = argument.partition("=")
            selected[name] = value if value else "(set)"
    return selected


def compare_configuration(baseline_manifest, manifest):
    print_section("configuration")
    differences = []
    fields = ("label", "host", "osVersion", "availableProcessors", "maxHeapBytes", "javaVersion")
    print("%-50s %-30s %-30s" % ("property", "baseline", "run"))
    print("-" * 112)

    def row(name, baseline_value, value, compared=True):
        marker = "" if not compared or baseline_value == value else "   <-- differs"
        print("%-50s %-30s %-30s%s" % (name, baseline_value, value, marker))
        if compared and baseline_value != value:
            differences.append("%s: %s -> %s" % (name, baseline_value, value))

    row("cpuBrand", baseline_manifest.get("system", {}).get("cpuBrand"), manifest.get("system", {}).get("cpuBrand"))
    for field in fields:
        row(field, baseline_manifest.get(field), manifest.get(field), compared=(field != "label"))

    baseline_args = interesting_jvm_args(baseline_manifest)
    args = interesting_jvm_args(manifest)
    for name in sorted(set(baseline_args) | set(args)):
        row("jvmArg " + name, baseline_args.get(name, "(absent)"), args.get(name, "(absent)"))

    if differences:
        print("")
        warn("CONFIGURATIONS DIFFER -- an unpinned difference invalidates the comparison:")
        for difference in differences:
            print("     " + difference)
    else:
        print("\nconfigurations match")
    return differences


SAMPLING_KEYS = SAMPLE_KEYS + ("fir.bench.compile.repeat", "fir.bench.instrumentation.probes.suite")


def compare_sampling(baseline_manifest, manifest, baseline_models, models, enabled=True):
    """The two runs must have compiled the SAME module set, otherwise the paired ratios compare different code.

    `fir.bench.sample.fraction`/`.seed` select a deterministic subset of the modules; a different fraction or a
    different seed means a different subset, which is a blocking difference. Even at an equal fraction the sets
    can drift (a module failing on one machine only), so they are also compared directly.
    """
    print_section("sampling and module sets")
    if not enabled:
        print("--no-sample-check: skipped")
        return {}
    baseline_configuration = baseline_manifest.get("testConfiguration") or {}
    configuration = manifest.get("testConfiguration") or {}
    print("%-46s %-24s %-24s" % ("property", "baseline", "run"))
    print("-" * 98)
    result = {}
    for key in SAMPLING_KEYS:
        baseline_value = baseline_configuration.get(key, "(absent)")
        value = configuration.get(key, "(absent)")
        marker = "" if baseline_value == value else "   <-- differs"
        print("%-46s %-24s %-24s%s" % (key, baseline_value, value, marker))
        result[key] = {"baseline": baseline_value, "run": value}
        if baseline_value != value and key in SAMPLE_KEYS:
            warn("%s DIFFERS (%s -> %s) -- the two runs compiled different module subsets, the comparison is "
                 "INVALID" % (key, baseline_value, value))

    same_fraction = (baseline_configuration.get(SAMPLE_KEYS[0], "(absent)")
                     == configuration.get(SAMPLE_KEYS[0], "(absent)"))
    union = baseline_models | models
    difference = baseline_models ^ models
    share = len(difference) / float(len(union)) if union else 0.0
    print("")
    print("%-46s %-24d %-24d" % ("modules attempted (including failures)", len(baseline_models), len(models)))
    print("%-46s %.2f %% of the union (%d modules)" % ("module sets differ by", share * 100, len(difference)))
    result["moduleSetDifferenceShare"] = share
    if same_fraction and share > MODULE_SET_TOLERANCE:
        warn("the compiled module sets differ by %.2f%% (%d of %d modules) at an EQUAL sample fraction -- the "
             "runs are not the same experiment" % (share * 100, len(difference), len(union)))
    elif share:
        print("(within the %.0f%% tolerance)" % (MODULE_SET_TOLERANCE * 100))
    return result


def compare_summaries(baseline_summary, summary):
    print_section("run summary and GC")
    print("%-34s %14s %14s %8s" % ("metric", "baseline", "run", "ratio"))
    print("-" * 74)
    for field, kind in (("compilations", "count"), ("failedCompilations", "count"),
                        ("runWallNanos", "nanos"), ("processCpuNanos", "nanos")):
        baseline_value = baseline_summary.get(field)
        value = summary.get(field)
        if baseline_value is None and value is None:
            continue
        print("%-34s %14s %14s %8s" % (field, FORMATTERS[kind](baseline_value), FORMATTERS[kind](value),
                                       format_ratio(ratio(value or 0, baseline_value or 0))))

    baseline_gc = {entry.get("kind"): entry for entry in baseline_summary.get("gc") or [] if isinstance(entry, dict)}
    run_gc = {entry.get("kind"): entry for entry in summary.get("gc") or [] if isinstance(entry, dict)}
    if not baseline_gc and not run_gc:
        print("no GC information in the summaries")
        return
    print("")
    print("%-34s %14s %14s %8s" % ("gc collector", "baseline", "run", "ratio"))
    print("-" * 74)
    for kind in sorted(set(baseline_gc) | set(run_gc)):
        baseline_entry = baseline_gc.get(kind, {})
        entry = run_gc.get(kind, {})
        print("%-34s %14s %14s %8s" % (
            "%s, time" % kind, format_millis(baseline_entry.get("millis")), format_millis(entry.get("millis")),
            format_ratio(ratio(entry.get("millis") or 0, baseline_entry.get("millis") or 0))))
        print("%-34s %14s %14s %8s" % (
            "%s, count" % kind, format_count(baseline_entry.get("count")), format_count(entry.get("count")),
            format_ratio(ratio(entry.get("count") or 0, baseline_entry.get("count") or 0))))

    # A full GC is stop-the-world for every worker thread, so a large difference makes the wall times incomparable.
    baseline_full = full_gc_millis(baseline_gc.values())
    run_full = full_gc_millis(run_gc.values())
    print("")
    print("%-34s %14s %14s %8s" % ("full GC total", format_millis(baseline_full), format_millis(run_full),
                                   format_ratio(ratio(run_full, baseline_full))))
    if baseline_full and run_full:
        factor = max(run_full / baseline_full, baseline_full / run_full)
        if factor > 2.0:
            warn("FULL GC TIME DIFFERS BY %.2fx -- GC dominates the run, the comparison is INVALID" % factor)


def full_gc_millis(entries):
    total = 0
    for entry in entries:
        kind = (entry.get("kind") or "").lower()
        if "marksweep" in kind or "old" in kind or "g1 old" in kind or "global" in kind:
            total += entry.get("millis") or 0
    return total


# ------------------------------------------------------------------------------------------------- concurrency

def concurrency(records):
    """Peak and time-weighted mean number of compilations running at the same time.

    `inFlightAtStart` (newer harness) is used when present, otherwise the concurrency is reconstructed by sweeping
    over the (+1 at start, -1 at end) events of the [startTimeMs, startTimeMs + wallNanos] intervals.
    """
    in_flight = [record["inFlightAtStart"] for record in records
                 if isinstance(record.get("inFlightAtStart"), (int, float))]
    if in_flight and len(in_flight) == len(records):
        return {"source": "inFlightAtStart", "mean": sum(in_flight) / float(len(in_flight)), "peak": max(in_flight)}

    events = []
    for record in records:
        start = record.get("startTimeMs")
        wall = record.get("wallNanos")
        if start is None or not wall:
            continue
        events.append((float(start) * 1e6, 1))
        events.append((float(start) * 1e6 + float(wall), -1))
    if not events:
        return {"source": "none", "mean": NAN, "peak": NAN}
    events.sort()
    current = 0
    peak = 0
    weighted = 0.0
    previous = events[0][0]
    for time, delta in events:
        weighted += current * (time - previous)
        previous = time
        current += delta
        peak = max(peak, current)
    span = events[-1][0] - events[0][0]
    return {"source": "interval sweep", "mean": weighted / span if span else NAN, "peak": peak, "span": span}


def compare_concurrency(baseline_records, records):
    print_section("observed concurrency")
    baseline = concurrency(baseline_records)
    run = concurrency(records)
    print("%-34s %14s %14s %8s" % ("metric", "baseline", "run", "ratio"))
    print("-" * 74)
    print("%-34s %14s %14s" % ("source", baseline["source"], run["source"]))
    print("%-34s %14.2f %14.2f %8s" % ("mean concurrency", baseline["mean"], run["mean"],
                                       format_ratio(ratio(run["mean"], baseline["mean"]))))
    print("%-34s %14.0f %14.0f %8s" % ("peak concurrency", baseline["peak"], run["peak"],
                                       format_ratio(ratio(run["peak"], baseline["peak"]))))
    if baseline["mean"] == baseline["mean"] and run["mean"] == run["mean"] and baseline["mean"]:
        deviation = abs(run["mean"] / baseline["mean"] - 1)
        if deviation > 0.10:
            warn("MEAN CONCURRENCY DIFFERS BY %.1f%% -- the runs are not comparable "
                 "(different load, different parallelism)" % (deviation * 100))
    return {"baseline": baseline, "run": run}


# ------------------------------------------------------------------------------------------------------ probes

def probe_medians(container, key):
    probes = container.get(key)
    if not isinstance(probes, dict):
        return {}
    return {name: probe.get("medianNanos") for name, probe in probes.items() if isinstance(probe, dict)}


def compare_probes(baseline_manifest, manifest, baseline_summary, summary):
    print_section("syscall / cpu probes (median per operation)")
    before = (probe_medians(baseline_manifest, "probesBefore"), probe_medians(manifest, "probesBefore"))
    after = (probe_medians(baseline_summary, "probesAfter"), probe_medians(summary, "probesAfter"))
    names = sorted(set(before[0]) | set(before[1]) | set(after[0]) | set(after[1]))
    if not names:
        print("no probes recorded")
        return {}
    print("%-24s %11s %11s %8s   %11s %11s %8s" % (
        "probe", "base before", "run before", "ratio", "base after", "run after", "ratio"))
    print("-" * 92)
    result = {}
    for name in names:
        missing = [label for label, values in (("baseline", before[0]), ("run", before[1])) if name not in values]
        print("%-24s %11s %11s %8s   %11s %11s %8s" % (
            name, format_nanos(before[0].get(name)), format_nanos(before[1].get(name)),
            format_ratio(ratio(before[1].get(name) or 0, before[0].get(name) or 0)),
            format_nanos(after[0].get(name)), format_nanos(after[1].get(name)),
            format_ratio(ratio(after[1].get(name) or 0, after[0].get(name) or 0))))
        if missing:
            print("     (probe missing in: %s -- not comparable)" % ", ".join(missing))
        result[name] = {
            "beforeRatio": ratio(before[1].get(name) or 0, before[0].get(name) or 0),
            "afterRatio": ratio(after[1].get(name) or 0, after[0].get(name) or 0),
        }

    cpu_ratio = NAN
    if "cpu" in result:
        before_ratio = result["cpu"]["beforeRatio"]
        after_ratio = result["cpu"]["afterRatio"]
        available = [value for value in (before_ratio, after_ratio) if value == value and value > 0]
        cpu_ratio = geometric_mean(available) if available else NAN
        print("")
        print("cpu probe ratio: before %s, after %s, used for normalization: %s" % (
            format_ratio(before_ratio).strip(), format_ratio(after_ratio).strip(), format_ratio(cpu_ratio).strip()))
        if len(available) == 2:
            spread = max(available) / min(available)
            if spread > 1.10:
                warn("the cpu probe disagrees by %.1f%% between before and after -- "
                     "normalization by the cpu probe is UNRELIABLE" % ((spread - 1) * 100))
    else:
        print("\nno `cpu` probe: the results cannot be normalized by the raw cpu speed")
    result["_cpuRatio"] = cpu_ratio
    return result


# ------------------------------------------------------------------------- multi-dimensional calibration model

def parse_weights(text):
    """`cpu=0.6,mem=0.35,disk=0.05`; the unmentioned weights keep their default."""
    weights = dict(DEFAULT_WEIGHTS)
    for item in text.split(","):
        if not item.strip():
            continue
        name, separator, value = item.partition("=")
        name = name.strip()
        if not separator or name not in weights:
            return None
        try:
            weights[name] = float(value)
        except ValueError:
            return None
    if any(weight < 0 for weight in weights.values()) or not sum(weights.values()):
        return None
    return weights


def calibration_dimensions(baseline_dir, run_dir):
    """The per-`kind` calibration factors, computed by `calibrate.py` over the probes of the two run directories.

    Every factor is oriented so that above 1 means "the run is slower in that dimension", which for the bytes/s
    probes means the ratio is inverted. Returns `None` when `calibrate.py` is not importable.
    """
    if calibrate is None:
        return None
    baseline_side = calibrate.read_side([baseline_dir], "baseline")
    run_side = calibrate.read_side([run_dir], "run")
    comparison = calibrate.compare_probes(baseline_side, run_side)
    return {"probes": comparison, "kinds": calibrate.kind_ratios(comparison),
            "groups": calibrate.group_ratios(comparison)}


def predicted_ratio(dimensions, weights):
    """The linear model in the RECIPROCAL-THROUGHPUT domain: times add up, so weighted times are additive.

    predicted = w_cpu * cpu_dim + w_mem * mem_dim + w_disk * disk_dim

    Every term is already a "how much slower" factor of a dimension, i.e. a relative time, so a weighted sum of
    them is the relative time of a workload that spends the weights' share of its time in each dimension.
    Missing dimensions are dropped and the remaining weights are renormalized, which is reported.
    """
    kinds = dimensions["kinds"]
    used = {}
    for name, kind in (("cpu", "cpu"), ("mem", "memory"), ("disk", "disk")):
        entry = kinds.get(kind)
        if entry and entry["ratio"] == entry["ratio"] and weights.get(name, 0) > 0:
            used[name] = entry
    total_weight = sum(weights[name] for name in used)
    if not used or not total_weight:
        return NAN, used, 0.0
    value = sum(weights[name] * used[name]["ratio"] for name in used) / total_weight
    return value, used, total_weight


def compare_calibration(baseline_dir, run_dir, metrics, weights, legacy_cpu_ratio):
    """Replaces the old single "normalized by the cpu probe" section with the multi-dimensional model."""
    print_section("multi-dimensional calibration: what is NOT explained by the machine speed")
    if calibrate is None:
        warn("calibrate.py could not be imported (%s) -- the multi-dimensional calibration is unavailable, "
             "falling back to the legacy one-dimensional normalization" % CALIBRATE_IMPORT_ERROR)
        print_legacy_normalization(metrics, legacy_cpu_ratio)
        return {}

    dimensions = calibration_dimensions(baseline_dir, run_dir)
    kinds = dimensions["kinds"]
    print("dimension  = geometric mean of the run/baseline ratios of the probes of that kind (>1 = run slower)")
    print("%-12s %8s %8s   %s" % ("dimension", "ratio", "spread", "probes"))
    print("-" * 112)
    for name, kind in (("cpu", "cpu"), ("mem", "memory"), ("disk", "disk"), ("process", "process")):
        entry = kinds.get(kind)
        if not entry:
            print("%-12s %8s %8s   %s" % (name, "  n/a ", "    - ", "(no probe of this kind)"))
            continue
        print("%-12s %8s %8s   %s" % (name, format_ratio(entry["ratio"]),
                                      calibrate.format_spread(entry["spread"]), ", ".join(entry["members"])))
        if entry["spread"] == entry["spread"] and entry["spread"] > calibrate.UNSTABLE_SPREAD:
            warn("the %s calibration dimension has a spread of %.1f%% between the before and the after probes -- "
                 "the predicted ratio built from it is unreliable"
                 % (name, (entry["spread"] - 1) * 100))

    predicted, used, total_weight = predicted_ratio(dimensions, weights)
    print("")
    print("model: predicted_ratio = %s" % " + ".join(
        "%.2f * %s" % (weights[name], name) for name, _ in DEFAULT_WEIGHTS if weights.get(name, 0) > 0))
    print("weights: %s" % ", ".join("%s=%.2f" % (name, weights[name]) for name, _ in DEFAULT_WEIGHTS))
    print("note: the weights are an ASSUMPTION about the workload mixture, not a measurement. Kotlin compilation")
    print("      is taken to be ~60% cpu, ~35% memory (pointer chasing over a large object graph) and ~5% file")
    print("      reads. Override them with --weights cpu=..,mem=..,disk=.. and watch how much the residual moves;")
    print("      if it moves a lot, the conclusion depends on the model rather than on the data.")
    print("the model is linear in the RECIPROCAL-THROUGHPUT (time) domain, where the terms are additive")
    if not used:
        warn("NO calibration dimension is available at all: the runs contain no usable probe, so nothing can be "
             "divided out and the ratios below are raw")
    elif len(used) < 3:
        missing = [name for name, _ in DEFAULT_WEIGHTS if name not in used]
        dropped = sum(weights[name] for name in missing)
        warn("the calibration is missing the %s dimension(s) (%.0f%% of the model weight): the remaining weights "
             "are renormalized over %.2f. THIS IS A %s-DIMENSIONAL CALIBRATION AND IS UNRELIABLE"
             % (", ".join(missing), dropped * 100, total_weight, "ONE" if len(used) == 1 else "TWO"))
    if used and set(kinds.get("cpu", {}).get("members", [])) == {"cpu"}:
        warn("the only cpu probe present is the LEGACY `cpu` one: a serial integer dependency chain that "
             "saturates on every modern core (it rated an M5 Max and an M2 Max as equal, 0.98, where Geekbench "
             "single-core says 1.71x). Re-run with the multi-dimensional probe suite before trusting the residual")
    if predicted != predicted:
        print("\nno usable calibration dimension at all -- falling back to the legacy normalization")
        print_legacy_normalization(metrics, legacy_cpu_ratio)
        return {"kinds": kinds, "predicted": None}

    print("")
    print("predicted workload ratio: %s   (measured / predicted = residual)" % format_ratio(predicted).strip())
    print("%-44s %9s %9s %9s %9s" % ("metric", "measured", "predicted", "residual", "res gmean"))
    print("-" * 84)
    residuals = {}
    for name, stats, _ in metrics:
        residual = ratio(stats["p50"], predicted)
        residuals[name] = residual
        print("%-44s %9s %9s %9s %9s" % (name, format_ratio(stats["p50"]), format_ratio(predicted),
                                         format_ratio(residual), format_ratio(ratio(stats["geomean"], predicted))))
    print("")
    print("residual = measured / predicted: the part of the difference that the raw machine speed does NOT")
    print("explain. 1.00 means the two configurations differ exactly as much as their probes do, i.e. only")
    print("because one machine is faster; above 1.00 means the run pays something extra - a monitoring agent,")
    print("a different amount of system time, more syscall latency - on top of being a different machine.")

    print("")
    print("-- sensitivity: the same metrics normalized by a single dimension instead of the model")
    print("%-44s %9s %9s %9s" % ("metric", "measured", "/ cpu_dim", "/ mem_dim"))
    print("-" * 74)
    cpu_dim = kinds.get("cpu", {}).get("ratio", NAN)
    mem_dim = kinds.get("memory", {}).get("ratio", NAN)
    print("%-44s %9s %9s %9s" % ("(the dimension itself)", "", format_ratio(cpu_dim), format_ratio(mem_dim)))
    for name, stats, _ in metrics:
        print("%-44s %9s %9s %9s" % (name, format_ratio(stats["p50"]), format_ratio(ratio(stats["p50"], cpu_dim)),
                                     format_ratio(ratio(stats["p50"], mem_dim))))
    print("")
    print("a residual that changes sign of its conclusion between these columns means the model choice, not the")
    print("measurement, is driving the answer")
    return {"kinds": kinds, "groups": dimensions["groups"], "predicted": predicted, "residuals": residuals,
            "weights": weights}


def print_legacy_normalization(metrics, cpu_ratio):
    """The pre-calibration behaviour: divide every headline ratio by the single legacy `cpu` probe ratio."""
    if cpu_ratio != cpu_ratio or cpu_ratio <= 0:
        print("no `cpu` probe either -- the results cannot be normalized at all")
        return
    print("")
    print("legacy normalization by the single cpu probe (headline ratio / %s)" % format_ratio(cpu_ratio).strip())
    print("%-44s %8s %8s %8s" % ("metric", "median", "gmean", "raw med"))
    print("-" * 72)
    for name, stats, _ in metrics:
        print("%-44s %8s %8s %8s" % (name, format_ratio(ratio(stats["p50"], cpu_ratio)),
                                     format_ratio(ratio(stats["geomean"], cpu_ratio)), format_ratio(stats["p50"])))


# ------------------------------------------------------------------------------------- iterations (cold vs warm)

def cold_warm_ratios(groups, field):
    """Per module: iteration 0 divided by the median of the iterations above 0."""
    ratios = []
    for records in groups.values():
        cold = [record for record in records if iteration_of(record) == 0]
        warm = [record.get(field) for record in records
                if (iteration_of(record) or 0) > 0 and record.get(field)]
        cold_value = cold[0].get(field) if cold else None
        if not cold_value or not warm:
            continue
        ratios.append(cold_value / median(warm))
    return ratios


def compare_iterations(baseline_groups, run_groups):
    """Cold (first compilation of a module) versus warm (the steady state of the following ones).

    The difference between the two is the file cache, the JIT and the class-loading of the compiler itself. It is
    a property of one run, so it is reported per run and not as a ratio between the runs: it tells how much of
    the measured time is warm-up, i.e. how much a difference in warm-up could contribute to the headline.
    """
    print_section("cold vs warm (iteration 0 / median of iterations > 0, per run)")
    result = {}
    if not has_iterations(baseline_groups) and not has_iterations(run_groups):
        print("no `iteration` field in the records: every module was compiled once "
              "(`fir.bench.compile.repeat` was not in effect), cold and warm cannot be separated")
        return result
    print("%-14s %-24s %6s %8s %8s %8s" % ("run", "metric", "n", "median", "gmean", "p90"))
    print("-" * 74)
    for label, groups in (("baseline", baseline_groups), ("run", run_groups)):
        print("%-14s %-24s %6d modules with more than one compilation"
              % (label, "(repeated)", repeated_models(groups)))
        for field in ("wallNanos", "threadCpuNanos", "threadSystemNanos"):
            ratios = sorted(cold_warm_ratios(groups, field))
            if not ratios:
                continue
            result.setdefault(label, {})[field] = {"n": len(ratios), "median": percentile(ratios, 0.5),
                                                   "geomean": geometric_mean(ratios)}
            print("%-14s %-24s %6d %8s %8s %8s" % (
                label, field, len(ratios), format_ratio(percentile(ratios, 0.5)),
                format_ratio(geometric_mean(ratios)), format_ratio(percentile(ratios, 0.90))))
    print("")
    print("above 1 means the first compilation of a module is slower than the following ones, which is the")
    print("file-cache + JIT warm-up. If this differs a lot between the two runs, the headline all-records")
    print("numbers are partly a warm-up comparison -- use the [warm] rows below, they are the steady state.")
    return result


# ------------------------------------------------------------------------------------------------ record metrics

def phase_value(record, phase, key):
    phases = record.get("phases")
    if not isinstance(phases, dict):
        return None
    entry = phases.get(phase)
    if not isinstance(entry, dict):
        return None
    return entry.get(key)


def lookup_value(record, lookup, key):
    entry = record.get(lookup)
    if not isinstance(entry, dict):
        return None
    return entry.get(key)


def per_call(record, lookup):
    count = lookup_value(record, lookup, "count")
    nanos = lookup_value(record, lookup, "nanos")
    if not count or nanos is None:
        return None
    return nanos / float(count)


def system_share(record):
    cpu = record.get("threadCpuNanos")
    system = record.get("threadSystemNanos")
    if not cpu or system is None:
        return None
    return system / float(cpu)


def phase_names(baseline_records, records):
    """`total` first, then the individual phases in a stable order."""
    names = set()
    for source in (baseline_records, records):
        for record in source.values():
            phases = record.get("phases")
            if isinstance(phases, dict):
                names.update(phases.keys())
    names.discard("total")
    return ["total"] + sorted(names)


def check_broken_phase_times(records, label):
    """`phases.total.userNanos` used to be summed over all the threads, which made it wildly exceed the wall time."""
    total = 0
    broken = 0
    for record in records.values():
        wall = record.get("wallNanos")
        user = phase_value(record, "total", "userNanos")
        if not wall or not user:
            continue
        total += 1
        if user > wall * BROKEN_PHASE_FACTOR:
            broken += 1
    if total and broken / float(total) > BROKEN_PHASE_SHARE:
        warn("%s: phases.total.userNanos exceeds %.0fx wallNanos for %d of %d records (%.1f%%) -- this run "
             "predates the harness fix, the per-phase user/cpu numbers are BROKEN and must be ignored"
             % (label, BROKEN_PHASE_FACTOR, broken, total, 100.0 * broken / total))
        return True
    return False


def compare_lookup_counts(common, baseline_records, records, lookup):
    """The number of class lookups must be identical for the same module: it is an equality check, not a ratio."""
    differing = 0
    compared = 0
    for model in common:
        baseline_count = lookup_value(baseline_records[model], lookup, "count")
        count = lookup_value(records[model], lookup, "count")
        if baseline_count is None or count is None:
            continue
        compared += 1
        if baseline_count != count:
            differing += 1
    if not compared:
        return
    if differing:
        warn("%s.count differs for %d of %d common modules -- the two runs did not compile the same code, "
             "the per-call times are only indicative" % (lookup, differing, compared))
    else:
        print("%-44s %5d modules, counts identical in both runs" % ("%s.count" % lookup, compared))


# --------------------------------------------------------------------------------------------- parallelism 1

def headline_metric(metrics, field):
    """The warm variant of a headline metric is preferred over the all-records one when it exists."""
    by_name = {name: stats for name, stats, _ in metrics}
    return by_name.get("%s [warm]" % field) or by_name.get(field)


def report_single_thread(concurrency_report, metrics, forced=False):
    """A run with one compilation at a time measures SINGLE-THREAD performance and nothing else.

    That is the case that can be compared with a single-core benchmark score (Geekbench, SPECint rate-1): no
    memory-bandwidth contention between the workers, no shared-cache thrashing, no GC threads competing with the
    compilation. With 8 compilations in flight the same numbers are a throughput measurement of the whole
    machine and must not be compared with a single-core score.
    """
    print_section("parallelism")
    baseline_mean = concurrency_report.get("baseline", {}).get("mean", NAN)
    run_mean = concurrency_report.get("run", {}).get("mean", NAN)
    observed = all(mean == mean and abs(mean - 1.0) <= SINGLE_THREAD_TOLERANCE
                   for mean in (baseline_mean, run_mean))
    print("observed mean concurrency: baseline %.2f, run %.2f" % (baseline_mean, run_mean))
    if not observed and not forced:
        print("more than one compilation was in flight on average: these numbers are the THROUGHPUT of the whole")
        print("machine, not single-thread performance, and must NOT be compared with a single-core benchmark")
        print("score. Re-run with --parallelism 1 on both sides (or pass --parallelism-1 to assume it) for a")
        print("number comparable with Geekbench single-core.")
        return {"singleThread": False}
    if forced and not observed:
        warn("--parallelism-1 was passed but the observed mean concurrency is %.2f / %.2f, not 1 -- the "
             "single-core interpretation below is ASSUMED, not measured" % (baseline_mean, run_mean))
    print("both runs executed one compilation at a time: this comparison measures SINGLE-THREAD performance and")
    print("is directly comparable with single-core benchmark scores such as Geekbench single-core.")
    stats = headline_metric(metrics, "wallNanos")
    if not stats:
        print("no wallNanos metric available, cannot state the implied per-core speedup")
        return {"singleThread": True}
    wall_ratio = stats["p50"]
    speedup = ratio(1.0, wall_ratio)
    print("")
    print("%-44s %s" % ("median wallNanos ratio (run / baseline)", format_ratio(wall_ratio).strip()))
    print("%-44s %s" % ("implied per-core speedup (1 / that ratio)", format_ratio(speedup).strip()))
    print("")
    print("the per-core speedup is what a single-core benchmark would have to report for the two machines if the")
    print("compiler workload were the same mixture the benchmark is; a large disagreement with the published")
    print("score is the interesting finding, not an error.")
    return {"singleThread": True, "wallRatio": wall_ratio, "perCoreSpeedup": speedup}


# -------------------------------------------------------------------------------------------------------- main

def main(baseline_dir, run_dir, json_path=None, top=0, weights=None, sample_check=True, single_thread=False):
    weights = dict(DEFAULT_WEIGHTS) if weights is None else weights
    baseline_manifest = read_json(os.path.join(baseline_dir, "manifest.json"))
    manifest = read_json(os.path.join(run_dir, "manifest.json"))
    baseline_summary = read_json(os.path.join(baseline_dir, "summary.json"))
    summary = read_json(os.path.join(run_dir, "summary.json"))

    print("baseline: %s (%s)" % (baseline_manifest.get("label"), baseline_dir))
    print("run:      %s (%s)" % (manifest.get("label"), run_dir))
    print("all the ratios are run / baseline, a value above 1 means the second run is slower")

    report = {"baselineDir": baseline_dir, "runDir": run_dir}
    report["configurationDifferences"] = compare_configuration(baseline_manifest, manifest)

    baseline_groups, baseline_failed, baseline_seen = read_records(baseline_dir)
    run_groups, failed, run_seen = read_records(run_dir)
    report["sampling"] = compare_sampling(baseline_manifest, manifest, baseline_seen, run_seen, sample_check)

    compare_summaries(baseline_summary, summary)
    report["probes"] = compare_probes(baseline_manifest, manifest, baseline_summary, summary)
    cpu_ratio = report["probes"].get("_cpuRatio", NAN)

    baseline_records = select_records(baseline_groups)
    records = select_records(run_groups)
    all_baseline_records = [record for group in baseline_groups.values() for record in group]
    all_run_records = [record for group in run_groups.values() for record in group]
    report["concurrency"] = compare_concurrency(all_baseline_records, all_run_records)

    common = sorted(set(baseline_records) & set(records))
    print_section("paired per-module ratios (headline; median and geometric mean are the robust numbers)")
    print("modules OK: %d baseline, %d run, %d common; skipped (not OK): %d baseline, %d run" % (
        len(baseline_records), len(records), len(common), baseline_failed, failed))
    only_baseline = sorted(set(baseline_records) - set(records))
    only_run = sorted(set(records) - set(baseline_records))
    if only_baseline or only_run:
        print("modules present in one run only: %d baseline-only, %d run-only (excluded from the ratios)"
              % (len(only_baseline), len(only_run)))
    if not common:
        print("\n!! no common successfully compiled modules, nothing to compare")
        return 1

    baseline_broken = check_broken_phase_times(baseline_records, "baseline")
    run_broken = check_broken_phase_times(records, "run")
    print("")

    metrics = []

    def add(name, getter, kind="nanos", skip_zero_run=False, models=None,
            baseline_source=None, source=None):
        models = common if models is None else models
        baseline_source = baseline_records if baseline_source is None else baseline_source
        source = records if source is None else source
        pairs = collect(models, baseline_source, source, getter, skip_zero_run)
        if not pairs:
            print("%-44s %5d (no comparable pairs)" % (name, 0))
            return None
        stats = statistics(pairs)
        metrics.append((name, stats, kind))
        print_metric(name, stats, kind)
        return pairs

    print_header()
    wall_pairs = None
    for field in ("wallNanos", "threadCpuNanos", "threadUserNanos", "threadSystemNanos"):
        pairs = add(field, lambda record, field=field: record.get(field))
        if field == "wallNanos":
            wall_pairs = pairs

    print("")
    print_header()
    add("systemShare (system/cpu)", system_share, "percent")
    absolute_shares = []
    for model in common:
        absolute_shares.append((system_share(baseline_records[model]), system_share(records[model])))
    baseline_shares = [value for value, _ in absolute_shares if value is not None]
    run_shares = [value for _, value in absolute_shares if value is not None]
    print("  absolute system time share: baseline median %s, run median %s (of thread cpu time)" % (
        format_percent(median(baseline_shares)), format_percent(median(run_shares))))

    print("")
    print_header()
    for lookup in ("findJavaClass", "findKotlinClass"):
        add("%s ns/call" % lookup, lambda record, lookup=lookup: per_call(record, lookup))
    for lookup in ("findJavaClass", "findKotlinClass"):
        compare_lookup_counts(common, baseline_records, records, lookup)

    print("")
    if baseline_broken or run_broken:
        print("per-phase userNanos/cpuNanos rows below are marked with `*`: broken data, ignore them")
    phase_keys = ["nanos", "userNanos", "cpuNanos", "systemNanos"]
    broken_keys = ("userNanos", "cpuNanos") if (baseline_broken or run_broken) else ()
    print_header()
    for phase in phase_names(baseline_records, records):
        for key in phase_keys:
            suffix = " *" if key in broken_keys else ""
            # A phase absent or not measured (zero) in either run carries no information, so such pairs are dropped.
            add("phases.%s.%s%s" % (phase, key, suffix),
                lambda record, phase=phase, key=key: phase_value(record, phase, key), skip_zero_run=True)

    optional = [("gcDeltaMillis", "millis"), ("jitDeltaMillis", "millis"), ("gcDeltaCount", "count")]
    present = [(field, kind) for field, kind in optional
               if any(field in record for record in baseline_records.values())
               and any(field in record for record in records.values())]
    if present:
        print("")
        print_header()
        for field, kind in present:
            add(field, lambda record, field=field: record.get(field), kind)

    # The records of the repeated compilations: the warm-up is separated from the steady state, and the paired
    # comparison is redone on the steady state only. Both are kept, because a difference between them is itself
    # a result (a slower cold path is exactly what a per-file caching agent looks like).
    report["coldVsWarm"] = compare_iterations(baseline_groups, run_groups)
    warm_baseline = select_records(baseline_groups, warm_only=True)
    warm_records = select_records(run_groups, warm_only=True)
    warm_common = sorted(set(warm_baseline) & set(warm_records))
    if warm_common and (has_iterations(baseline_groups) or has_iterations(run_groups)):
        print_section("paired per-module ratios, WARM records only (iteration > 0): the steady state")
        print("%d common modules have at least one warm compilation in both runs; the rows are marked [warm] and"
              % len(warm_common))
        print("are the ones to quote when the harness ran with fir.bench.compile.repeat > 1")
        print("")
        print_header()
        for field in ("wallNanos", "threadCpuNanos", "threadUserNanos", "threadSystemNanos"):
            add("%s [warm]" % field, lambda record, field=field: record.get(field), models=warm_common,
                baseline_source=warm_baseline, source=warm_records)
        add("systemShare (system/cpu) [warm]", system_share, "percent", models=warm_common,
            baseline_source=warm_baseline, source=warm_records)

    # Everything that remains after dividing by the predicted ratio is not explained by the machine speed.
    report["calibration"] = compare_calibration(baseline_dir, run_dir, metrics, weights, cpu_ratio)
    report["singleThread"] = report_single_thread(report["concurrency"], metrics, single_thread)

    print_section("throughput (sum of totals; tail-sensitive, not a per-module ratio)")
    print("%-34s %14s %14s %8s" % ("metric", "baseline", "run", "ratio"))
    print("-" * 74)
    for field in ("wallNanos", "threadCpuNanos", "threadUserNanos", "threadSystemNanos"):
        baseline_total = sum(baseline_records[model].get(field) or 0 for model in common)
        run_total = sum(records[model].get(field) or 0 for model in common)
        print("%-34s %14s %14s %8s" % ("sum " + field, format_nanos(baseline_total), format_nanos(run_total),
                                       format_ratio(ratio(run_total, baseline_total))))
    baseline_phase_total = sum(phase_value(baseline_records[model], "total", "nanos") or 0 for model in common)
    run_phase_total = sum(phase_value(records[model], "total", "nanos") or 0 for model in common)
    print("%-34s %14s %14s %8s" % ("sum phases.total.nanos", format_nanos(baseline_phase_total),
                                   format_nanos(run_phase_total),
                                   format_ratio(ratio(run_phase_total, baseline_phase_total))))
    print("\nthe sums are dominated by the few heaviest modules; prefer the paired medians above")

    if top and wall_pairs:
        print_section("top %d modules by wallNanos ratio" % top)
        print("%-64s %11s %11s %8s" % ("module", "baseline", "run", "ratio"))
        print("-" * 98)
        ordered = sorted(wall_pairs, key=lambda pair: ratio(pair[1], pair[0]), reverse=True)[:top]
        for baseline_value, value, model in ordered:
            print("%-64s %11s %11s %8s" % (model[:64], format_nanos(baseline_value), format_nanos(value),
                                           format_ratio(ratio(value, baseline_value))))

    print_section("warnings")
    if warnings:
        for message in warnings:
            print("!! " + message)
    else:
        print("none")

    report["metrics"] = {name: stats for name, stats, _ in metrics}
    report["cpuProbeRatio"] = cpu_ratio if cpu_ratio == cpu_ratio else None
    report["weights"] = weights
    report["warnings"] = warnings
    report["brokenPhaseUserTimes"] = {"baseline": baseline_broken, "run": run_broken}
    if json_path:
        with open(json_path, "w") as file:
            json.dump(report, file, indent=2, default=str)
        print("\ncomparison written to %s" % json_path)
    return 0


def parse_arguments(argv):
    json_path = None
    top = 0
    weights = dict(DEFAULT_WEIGHTS)
    sample_check = True
    single_thread = False
    positional = []
    index = 0
    while index < len(argv):
        argument = argv[index]
        if argument == "--json" and index + 1 < len(argv):
            json_path = argv[index + 1]
            index += 1
        elif argument == "--top" and index + 1 < len(argv):
            try:
                top = int(argv[index + 1])
            except ValueError:
                return None
            index += 1
        elif argument == "--weights" and index + 1 < len(argv):
            weights = parse_weights(argv[index + 1])
            if weights is None:
                return None
            index += 1
        elif argument == "--sample-check":
            sample_check = True
        elif argument == "--no-sample-check":
            sample_check = False
        elif argument == "--parallelism-1":
            single_thread = True
        elif argument.startswith("-"):
            return None
        else:
            positional.append(argument)
        index += 1
    if len(positional) != 2:
        return None
    return positional[0], positional[1], json_path, top, weights, sample_check, single_thread


if __name__ == "__main__":
    parsed = parse_arguments(sys.argv[1:])
    if parsed is None:
        print(__doc__)
        sys.exit(2)
    sys.exit(main(*parsed))
