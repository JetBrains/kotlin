#!/usr/bin/env python3
"""Reads the multi-dimensional calibration probe suite recorded by ModularizedTestInstrumentation.

Usage:
    calibrate.py [--json <file>] <run-dir> [<run-dir> ...]
    calibrate.py [--json <file>] --compare <baseline-run-dir> [...] -- <run-dir> [...]

The first form prints the machine profile of one configuration, the second one the calibration factors between
two configurations. Several run directories per side are aggregated by taking the MEDIAN across the runs of each
probe's own median, which is how 3-5 repetitions of the calibration cell are meant to be fed in.

Why more than one probe: the legacy `cpu` probe is a serial integer dependency chain, so it measures one single
thing - the latency of an add on the shortest path. It saturates on every modern core and reported an M5 Max and
an M2 Max as equal (ratio 0.98) while Geekbench single-core says 1.71x. A machine differs from another machine in
several independent dimensions - core width (instruction-level parallelism), branch prediction, memory latency at
every level of the hierarchy, memory bandwidth, allocation/GC throughput, syscall and file-system latency, process
creation cost - and a workload is a different mixture of those dimensions than any single probe. Each dimension
has to be measured separately before anything can be normalized by it.

Reading the output:

  * every value is per operation (ns/op) or a rate (bytes/s), derived from `medianNanos`/`opsPerMeasurement`, so
    probes with different internal repetition counts are directly comparable;
  * in the comparison the ratios are oriented so that a value ABOVE 1 always means "the run is slower", which for
    the bytes/s probes means the ratio is inverted (baseline/run);
  * a probe is taken twice per run, before the first compilation and after the last one. If the two disagree by
    more than 15% the probe measured the machine's mood rather than the machine, and it is marked `WARN`: such a
    dimension cannot calibrate anything;
  * the harness writes the legacy integer-latency chain under two names (`cpu` and `cpuIntLatency`), so the alias
    is printed but excluded from every geometric mean - one measurement must contribute once.
"""

import json
import math
import os
import sys

NAN = float("nan")

# A probe whose before/after (or run-to-run) values disagree by more than this is useless for calibration.
UNSTABLE_SPREAD = 1.15

# The plausibility ranges below are rules of thumb, so a value right at a boundary is not worth a comment.
PLAUSIBILITY_SLACK = 1.05

# Fallback classification for the runs recorded before the probes carried an explicit `kind`.
PROBE_KINDS = {
    "cpu": "cpu",
    "cpuIntLatency": "cpu",
    "cpuIntThroughput": "cpu",
    "cpuBranch": "cpu",
    "cpuFpu": "cpu",
    "memLatency32k": "memory",
    "memLatency1m": "memory",
    "memLatency8m": "memory",
    "memLatency256m": "memory",
    "memBandwidthRead": "memory",
    "memBandwidthCopy": "memory",
    "allocRate": "memory",
    "fileOpenRead": "disk",
    "fileStat": "disk",
    "fileOpenReadUnique": "disk",
    "fileCreateDelete": "disk",
    "diskReadSequential": "disk",
    "diskWriteFsync": "disk",
    "dirScan": "disk",
    "processExec": "process",
}

# The probes that report a rate: higher is better, and the ratio has to be inverted to keep ">1 means slower".
BANDWIDTH_PROBES = ("memBandwidthRead", "memBandwidthCopy", "allocRate", "diskReadSequential")

# The harness reports the legacy integer-latency chain under BOTH names, so that the older run directories stay
# comparable. Both are printed, but the alias is excluded from every geometric mean: the same measurement must
# not be counted twice, it would give integer latency a double weight in the cpu dimension.
ALIASES = {"cpuIntLatency": "cpu"}

KIND_ORDER = ("cpu", "memory", "disk", "process", "other")

# The grouped summary of the comparison: each group is the geometric mean of the members that are present.
GROUPS = (
    ("cpu_latency", ("cpu", "cpuIntLatency")),
    ("cpu_throughput", ("cpuIntThroughput",)),
    ("cpu_branch", ("cpuBranch",)),
    ("cpu_fp", ("cpuFpu",)),
    ("mem_latency", ("memLatency32k", "memLatency1m", "memLatency8m", "memLatency256m")),
    ("mem_bandwidth", ("memBandwidthRead", "memBandwidthCopy")),
    ("alloc", ("allocRate",)),
    ("disk_latency", ("fileOpenRead", "fileStat", "fileOpenReadUnique", "fileCreateDelete",
                      "diskWriteFsync", "dirScan")),
    ("disk_bandwidth", ("diskReadSequential",)),
    ("exec", ("processExec",)),
)

# The kinds the linear workload model of compare-bench-runs.py is built from.
MODEL_KINDS = ("cpu", "memory", "disk")

# `name: (low, high, what the range means)`, in the probe's own unit (ns/op or bytes/s). Only a plausibility
# check: a value outside the range means the probe, the machine state or the machine itself is not what is
# expected, and a value off by 10x means one of them is broken.
PLAUSIBLE = {
    "cpu": (5e6, 1e8, "legacy serial chain, whole measurement"),
    "cpuIntLatency": (0.2, 5.0, "latency of one dependent integer add"),
    "cpuIntThroughput": (0.05, 2.0, "must be well BELOW cpuIntLatency: core width"),
    "cpuBranch": (0.5, 30.0, "unpredictable branch, ~15-20 cycles when mispredicted"),
    "cpuFpu": (0.1, 5.0, "independent double multiply-add"),
    "memLatency32k": (0.3, 5.0, "L1-resident pointer chase, ~3-4 cycles"),
    "memLatency1m": (1.0, 20.0, "L2-resident pointer chase"),
    "memLatency8m": (3.0, 60.0, "L2/SLC boundary"),
    "memLatency256m": (80.0, 150.0, "DRAM, 80-150 ns on Apple Silicon"),
    "memBandwidthRead": (1e10, 8e11, "tens to hundreds of GB/s"),
    "memBandwidthCopy": (5e9, 4e11, "tens of GB/s on Apple Silicon"),
    "allocRate": (5e8, 1e11, "JVM allocation + young GC throughput"),
    "fileStat": (5e2, 9e3, "single-digit us at worst; sub-us without a monitoring agent"),
    "fileOpenRead": (1e3, 5e4, "open+read of a cached file"),
    "fileOpenReadUnique": (1e3, 1e5, "open+read of a file touched once"),
    "fileCreateDelete": (5e3, 1e6, "create+write+delete, hits the journal"),
    "diskReadSequential": (2e8, 1e10, "hundreds of MB/s to several GB/s on NVMe"),
    "diskWriteFsync": (5e4, 2e7, "4K write + fsync, dominated by the device"),
    "dirScan": (3e2, 5e4, "one stat while walking a tree"),
    "processExec": (5e5, 2e7, "fork+exec+wait; AUTH_EXEC is synchronous, an agent is most visible here"),
}

# The dimensions worth printing in the compact machine profile of a single run.
PROFILE_ORDER = ("cpuIntLatency", "cpuIntThroughput", "cpuBranch", "cpuFpu", "cpu",
                 "memLatency32k", "memLatency1m", "memLatency8m", "memLatency256m",
                 "memBandwidthRead", "memBandwidthCopy", "allocRate",
                 "fileStat", "fileOpenRead", "diskReadSequential", "diskWriteFsync", "processExec")

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


def probe_kind(name, probe):
    kind = probe.get("kind") if isinstance(probe, dict) else None
    if isinstance(kind, str) and kind:
        return kind
    return PROBE_KINDS.get(name, "other")


def probe_unit(name, probe):
    unit = probe.get("unit") if isinstance(probe, dict) else None
    if isinstance(unit, str) and unit:
        return unit
    return "bytes/s" if name in BANDWIDTH_PROBES else "ns/op"


def probe_ops(probe):
    ops = probe.get("opsPerMeasurement") if isinstance(probe, dict) else None
    return float(ops) if isinstance(ops, (int, float)) and ops > 0 else 1.0


def probe_lower_is_better(name, probe):
    flag = probe.get("lowerIsBetter") if isinstance(probe, dict) else None
    if isinstance(flag, bool):
        return flag
    return probe_unit(name, probe) != "bytes/s"


def probe_value(name, probe):
    """The derived per-operation figure: ns per operation, or bytes per second for the rate probes.

    `medianNanosPerOp`/`medianBytesPerSecond` are used when the harness provides them; otherwise the value is
    derived from `medianNanos` and `opsPerMeasurement` (absent means 1, i.e. the whole measurement is one op).
    """
    if not isinstance(probe, dict):
        return None
    if probe_unit(name, probe) == "bytes/s":
        value = probe.get("medianBytesPerSecond")
        return float(value) if isinstance(value, (int, float)) and value > 0 else None
    per_op = probe.get("medianNanosPerOp")
    if isinstance(per_op, (int, float)) and per_op > 0:
        return float(per_op)
    median = probe.get("medianNanos")
    if not isinstance(median, (int, float)) or median <= 0:
        return None
    ops = probe.get("opsPerMeasurement")
    if not isinstance(ops, (int, float)) or ops <= 0:
        ops = 1
    return float(median) / float(ops)


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


def median(values):
    return percentile(sorted(values), 0.5)


def geometric_mean(values):
    """Computed in the log space: the geometric mean is the only meaningful average of ratios."""
    positive = [value for value in values if value == value and value > 0]
    if not positive:
        return NAN
    return math.exp(sum(math.log(value) for value in positive) / len(positive))


def spread_of(values):
    """max/min: 1.0 means the samples agree perfectly."""
    positive = [value for value in values if value == value and value > 0]
    if len(positive) < 2:
        return NAN
    return max(positive) / min(positive)


def oriented_ratio(baseline_value, value, lower_is_better):
    """run / baseline for a latency, baseline / run for a rate: above 1 always means the run is slower."""
    if not baseline_value or not value or baseline_value != baseline_value or value != value:
        return NAN
    return value / baseline_value if lower_is_better else baseline_value / value


# -------------------------------------------------------------------------------------------------- formatting

def format_per_op(nanos):
    if nanos is None or nanos != nanos:
        return "n/a"
    if nanos >= 1e6:
        return "%.2f ms/op" % (nanos / 1e6)
    if nanos >= 1e3:
        return "%.2f us/op" % (nanos / 1e3)
    if nanos >= 1:
        return "%.2f ns/op" % nanos
    return "%.3f ns/op" % nanos


def format_rate(bytes_per_second):
    if bytes_per_second is None or bytes_per_second != bytes_per_second:
        return "n/a"
    if bytes_per_second >= 1e9:
        return "%.2f GB/s" % (bytes_per_second / 1e9)
    if bytes_per_second >= 1e6:
        return "%.1f MB/s" % (bytes_per_second / 1e6)
    if bytes_per_second >= 1e3:
        return "%.1f kB/s" % (bytes_per_second / 1e3)
    return "%.0f B/s" % bytes_per_second


def format_value(value, unit):
    return format_rate(value) if unit == "bytes/s" else format_per_op(value)


def format_ratio(value):
    if value is None or value != value:
        return "  n/a "
    if value >= 1000:
        return "%6.0f" % value
    return "%6.3f" % value


def format_spread(value):
    if value is None or value != value:
        return "    - "
    return "%5.1f%%" % ((value - 1) * 100)


def print_section(title):
    print("")
    print("=" * 118)
    print(title)
    print("=" * 118)


# --------------------------------------------------------------------------------------------- one configuration

def read_side(run_dirs, label):
    """Aggregates the probes of several runs of the same configuration into one dictionary per probe name.

    `beforeValue`/`afterValue` are the medians across the runs of the respective probe median, `value` is the
    geometric mean of the two (the number used for calibration) and `spread` is max/min over every single sample,
    i.e. it covers both the run-to-run and the before/after disagreement.
    """
    side = {"label": label, "dirs": list(run_dirs), "probes": {}, "runs": [], "configuration": {}}
    for run_dir in run_dirs:
        manifest = read_json(os.path.join(run_dir, "manifest.json"))
        summary = read_json(os.path.join(run_dir, "summary.json"))
        run = {"dir": run_dir, "label": manifest.get("label"),
               "cpuBrand": (manifest.get("system") or {}).get("cpuBrand")}
        side["runs"].append(run)
        if isinstance(manifest.get("testConfiguration"), dict):
            side["configuration"].update(manifest["testConfiguration"])
        found = 0
        for phase, container, key in (("before", manifest, "probesBefore"), ("after", summary, "probesAfter")):
            probes = container.get(key)
            if not isinstance(probes, dict):
                print("!! %s: no %s" % (run_dir, key))
                continue
            for name, probe in probes.items():
                value = probe_value(name, probe)
                if value is None:
                    continue
                found += 1
                entry = side["probes"].setdefault(name, {
                    "kind": probe_kind(name, probe),
                    "unit": probe_unit(name, probe),
                    "lowerIsBetter": probe_lower_is_better(name, probe),
                    "ops": probe_ops(probe),
                    "before": [], "after": [],
                })
                entry[phase].append(value)
        if not found:
            print("!! %s: no usable probe values at all" % run_dir)

    for entry in side["probes"].values():
        samples = entry["before"] + entry["after"]
        entry["beforeValue"] = median(entry["before"]) if entry["before"] else NAN
        entry["afterValue"] = median(entry["after"]) if entry["after"] else NAN
        entry["value"] = geometric_mean([entry["beforeValue"], entry["afterValue"]])
        entry["spread"] = spread_of(samples)
        entry["samples"] = len(samples)
        entry["beforeAfterSpread"] = spread_of([entry["beforeValue"], entry["afterValue"]])
        entry["unstable"] = entry["spread"] == entry["spread"] and entry["spread"] > UNSTABLE_SPREAD
    for name, canonical in ALIASES.items():
        entry = side["probes"].get(name)
        other = side["probes"].get(canonical)
        if entry and other and same_value(entry["value"], other["value"]):
            entry["aliasOf"] = canonical
    return side


def same_value(first, second):
    """The alias of a probe is written from the same measurement array, so the values are bit-identical."""
    if not first or not second or first != first or second != second:
        return False
    return abs(first / second - 1) < 1e-6


def plausibility(name, value, ops=1.0):
    # The legacy `cpu` probe reported the duration of the whole measurement (no `opsPerMeasurement`), the current
    # one reports the same chain per operation, so the expected range depends on which of the two this is.
    if name == "cpu" and ops > 1:
        name = "cpuIntLatency"
    if name not in PLAUSIBLE or value is None or value != value or value <= 0:
        return ""
    low, high, meaning = PLAUSIBLE[name]
    if value < low / PLAUSIBILITY_SLACK:
        factor = low / value
        verdict = "BROKEN?" if factor >= 10 else "low"
        return "%s %.1fx below the expected %s -- %s" % (verdict, factor, format_value(low, unit_of(name)), meaning)
    if value > high * PLAUSIBILITY_SLACK:
        factor = value / high
        verdict = "BROKEN?" if factor >= 10 else "high"
        return "%s %.1fx above the expected %s -- %s" % (verdict, factor, format_value(high, unit_of(name)), meaning)
    return "plausible (%s)" % meaning


def unit_of(name):
    return "bytes/s" if name in BANDWIDTH_PROBES else "ns/op"


def print_single(side):
    print_section("probe report: %s" % side["label"])
    for run in side["runs"]:
        print("  %s  label=%s  cpu=%s" % (run["dir"], run["label"], run["cpuBrand"]))
    if len(side["runs"]) > 1:
        print("  %d runs aggregated: every value is the median across the runs of that run's probe median"
              % len(side["runs"]))
    if not side["probes"]:
        print("\nno probes recorded in any of the run directories -- nothing to report")
        return
    header = "%-22s %-8s %13s %13s %13s %8s %s" % (
        "probe", "unit", "value", "before", "after", "spread", "")
    for kind in KIND_ORDER:
        names = sorted(name for name, entry in side["probes"].items() if entry["kind"] == kind)
        if not names:
            continue
        print("")
        print("-- %s" % kind)
        print(header)
        print("-" * 92)
        for name in names:
            entry = side["probes"][name]
            unit = entry["unit"]
            notes = []
            if entry.get("aliasOf"):
                notes.append("same measurement as `%s`, not counted twice" % entry["aliasOf"])
            if entry["unstable"]:
                notes.append("WARN unstable")
            print("%-22s %-8s %13s %13s %13s %8s %s" % (
                name, unit, format_value(entry["value"], unit), format_value(entry["beforeValue"], unit),
                format_value(entry["afterValue"], unit), format_spread(entry["spread"]), "; ".join(notes)))
    print("")
    print("spread is max/min over all the before and after samples; WARN marks more than %.0f%% disagreement, "
          "which makes the dimension unusable for calibration" % ((UNSTABLE_SPREAD - 1) * 100))

    print("")
    print("-- machine profile (plausibility of the well-known dimensions)")
    print("%-22s %13s   %s" % ("dimension", "value", "comment"))
    print("-" * 110)
    printed = 0
    for name in PROFILE_ORDER:
        entry = side["probes"].get(name)
        if not entry:
            continue
        printed += 1
        comment = plausibility(name, entry["value"], entry.get("ops", 1.0))
        print("%-22s %13s   %s" % (name, format_value(entry["value"], entry["unit"]), comment))
        if comment.startswith("BROKEN?"):
            warn("%s: %s is implausible (%s) -- the probe or the machine state is broken, do not calibrate with it"
                 % (side["label"], name, format_value(entry["value"], entry["unit"])))
    if not printed:
        print("(none of the well-known dimensions is present in this run)")
    latency = side["probes"].get("cpuIntLatency") or side["probes"].get("cpu")
    throughput = side["probes"].get("cpuIntThroughput")
    if latency and throughput and latency["value"] > 0 and throughput["value"] > 0:
        print("")
        print("cpuIntLatency / cpuIntThroughput = %s -- the effective instruction-level parallelism of the core "
              "(1.0 would mean the core executes one dependent op at a time and the two probes measure the same "
              "thing)" % format_ratio(latency["value"] / throughput["value"]).strip())


# ------------------------------------------------------------------------------------------------- comparison

def compare_probes(baseline_side, run_side):
    """Per-probe calibration factors, oriented so that above 1 always means the run is slower."""
    result = {}
    names = sorted(set(baseline_side["probes"]) | set(run_side["probes"]))
    for name in names:
        baseline_entry = baseline_side["probes"].get(name)
        run_entry = run_side["probes"].get(name)
        if not baseline_entry or not run_entry:
            result[name] = {"kind": (baseline_entry or run_entry)["kind"], "ratio": NAN,
                            "missing": "baseline" if not baseline_entry else "run"}
            continue
        spread = max(value for value in (baseline_entry["spread"], run_entry["spread"], 1.0)
                     if value == value)
        result[name] = {
            "kind": baseline_entry["kind"],
            "unit": baseline_entry["unit"],
            "aliasOf": baseline_entry.get("aliasOf") or run_entry.get("aliasOf"),
            "baseline": baseline_entry["value"],
            "run": run_entry["value"],
            "ratio": oriented_ratio(baseline_entry["value"], run_entry["value"], baseline_entry["lowerIsBetter"]),
            "beforeRatio": oriented_ratio(baseline_entry["beforeValue"], run_entry["beforeValue"],
                                          baseline_entry["lowerIsBetter"]),
            "afterRatio": oriented_ratio(baseline_entry["afterValue"], run_entry["afterValue"],
                                         baseline_entry["lowerIsBetter"]),
            "spread": spread,
            "unstable": spread > UNSTABLE_SPREAD,
            "missing": None,
        }
    return result


def group_ratios(comparison):
    """The grouped summary: each group is the geometric mean of the ratios of its present members."""
    groups = {}
    for group, members in GROUPS:
        usable = [name for name in members if usable_member(comparison, name)]
        present = [comparison[name] for name in usable]
        if not present:
            continue
        spreads = [entry["spread"] for entry in present if entry.get("spread") == entry.get("spread")]
        spread = max(spreads) if spreads else NAN
        groups[group] = {
            "ratio": geometric_mean([entry["ratio"] for entry in present]),
            "members": usable,
            "spread": spread,
            "unstable": spread == spread and spread > UNSTABLE_SPREAD,
        }
    return groups


def usable_member(comparison, name):
    """A probe contributes to a geometric mean only if it has a ratio and is not an alias of another probe."""
    entry = comparison.get(name)
    return bool(entry) and entry["ratio"] == entry["ratio"] and not entry.get("aliasOf")


def kind_ratios(comparison):
    """The geometric mean of the ratios per probe `kind`: the input of the linear workload model."""
    result = {}
    for kind in MODEL_KINDS + ("process",):
        names = sorted(name for name, entry in comparison.items()
                       if entry.get("kind") == kind and usable_member(comparison, name))
        if not names:
            continue
        spreads = [comparison[name]["spread"] for name in names
                   if comparison[name].get("spread") == comparison[name].get("spread")]
        result[kind] = {"ratio": geometric_mean([comparison[name]["ratio"] for name in names]),
                        "members": names, "spread": max(spreads) if spreads else NAN}
    return result


def print_comparison(baseline_side, run_side, comparison, groups):
    print_section("calibration factors: run / baseline (above 1 = the RUN IS SLOWER in that dimension)")
    print("baseline: %s" % ", ".join(side_description(baseline_side)))
    print("run:      %s" % ", ".join(side_description(run_side)))
    print("the bytes/s probes are inverted (baseline/run) so that above 1 keeps meaning `the run is slower`")
    if not comparison:
        print("\nno probes recorded on either side -- nothing to calibrate")
        return
    header = "%-22s %-8s %13s %13s %8s %8s %8s %8s %s" % (
        "probe", "unit", "baseline", "run", "ratio", "before", "after", "spread", "")
    for kind in KIND_ORDER:
        names = sorted(name for name, entry in comparison.items() if entry.get("kind") == kind)
        if not names:
            continue
        print("")
        print("-- %s" % kind)
        print(header)
        print("-" * 112)
        for name in names:
            entry = comparison[name]
            if entry["missing"]:
                print("%-22s %-8s %13s %13s %8s %8s %8s %8s %s" % (
                    name, unit_of(name), "-", "-", "  n/a ", "  n/a ", "  n/a ", "    - ",
                    "missing in %s -- not comparable" % entry["missing"]))
                continue
            notes = []
            if entry.get("aliasOf"):
                notes.append("same measurement as `%s`, excluded from the means" % entry["aliasOf"])
            if entry["unstable"]:
                notes.append("WARN unstable")
            print("%-22s %-8s %13s %13s %8s %8s %8s %8s %s" % (
                name, entry["unit"], format_value(entry["baseline"], entry["unit"]),
                format_value(entry["run"], entry["unit"]), format_ratio(entry["ratio"]),
                format_ratio(entry["beforeRatio"]), format_ratio(entry["afterRatio"]),
                format_spread(entry["spread"]), "; ".join(notes)))

    print("")
    print("-- grouped dimensions (geometric mean of the members)")
    print("%-18s %8s %8s   %-46s %s" % ("dimension", "ratio", "spread", "members", ""))
    print("-" * 112)
    for group, _ in GROUPS:
        entry = groups.get(group)
        if not entry:
            print("%-18s %8s %8s   %-46s %s" % (group, "  n/a ", "    - ", "(no probe present)", ""))
            continue
        print("%-18s %8s %8s   %-46s %s" % (
            group, format_ratio(entry["ratio"]), format_spread(entry["spread"]),
            ",".join(entry["members"])[:46], "WARN unusable for calibration" if entry["unstable"] else ""))
        if entry["unstable"]:
            warn("dimension %s has a spread of %.1f%% across the supplied runs/before/after -- it is UNUSABLE "
                 "for calibration" % (group, (entry["spread"] - 1) * 100))
    print("")
    print("spread is the largest max/min disagreement of a member across the supplied runs and between the")
    print("before and the after measurement; above %.0f%% the dimension says more about the noise than about the"
          % ((UNSTABLE_SPREAD - 1) * 100))
    print("machine. Feed 3-5 repetitions of the calibration cell per side to make this column meaningful.")


def side_description(side):
    parts = []
    for run in side["runs"]:
        parts.append("%s (%s)" % (run["label"] or "?", run["cpuBrand"] or "?"))
    return parts or ["(nothing read)"]


def compare_configuration(baseline_side, run_side):
    keys = ("fir.bench.sample.fraction", "fir.bench.sample.seed", "fir.bench.compile.repeat",
            "fir.bench.instrumentation.probes.suite")
    baseline_configuration = baseline_side["configuration"]
    configuration = run_side["configuration"]
    if not any(key in baseline_configuration or key in configuration for key in keys):
        return
    print("")
    print("-- sampling / probe-suite configuration")
    print("%-44s %-24s %-24s" % ("property", "baseline", "run"))
    print("-" * 96)
    for key in keys:
        baseline_value = baseline_configuration.get(key, "(absent)")
        value = configuration.get(key, "(absent)")
        marker = "" if baseline_value == value else "   <-- differs"
        print("%-44s %-24s %-24s%s" % (key, baseline_value, value, marker))
        if baseline_value != value and key.startswith("fir.bench.sample"):
            warn("%s differs between the sides (%s -> %s) -- the two sides did not compile the same module set"
                 % (key, baseline_value, value))


# -------------------------------------------------------------------------------------------------------- main

def main(baseline_dirs, run_dirs, json_path=None):
    report = {}
    if baseline_dirs is None:
        side = read_side(run_dirs, "run")
        print_single(side)
        report["probes"] = {name: {key: entry[key] for key in ("kind", "unit", "value", "beforeValue",
                                                               "afterValue", "spread", "samples")}
                            for name, entry in side["probes"].items()}
    else:
        baseline_side = read_side(baseline_dirs, "baseline")
        run_side = read_side(run_dirs, "run")
        print_single(baseline_side)
        print_single(run_side)
        comparison = compare_probes(baseline_side, run_side)
        groups = group_ratios(comparison)
        print_comparison(baseline_side, run_side, comparison, groups)
        compare_configuration(baseline_side, run_side)
        report["probes"] = comparison
        report["dimensions"] = groups
        report["kinds"] = kind_ratios(comparison)

    print_section("warnings")
    if warnings:
        for message in warnings:
            print("!! " + message)
    else:
        print("none")

    report["warnings"] = warnings
    report["baselineDirs"] = baseline_dirs
    report["runDirs"] = run_dirs
    if json_path:
        try:
            with open(json_path, "w") as file:
                json.dump(report, file, indent=2, default=str)
            print("\ncalibration written to %s" % json_path)
        except (IOError, OSError) as error:
            print("!! cannot write %s: %s" % (json_path, error))
    return 0


def parse_arguments(argv):
    """`--compare <baseline dirs> -- <run dirs>`, or plain run directories for the single-configuration report."""
    json_path = None
    compare = False
    separator_seen = False
    left = []
    right = []
    index = 0
    while index < len(argv):
        argument = argv[index]
        if argument == "--json" and index + 1 < len(argv):
            json_path = argv[index + 1]
            index += 1
        elif argument == "--compare":
            compare = True
        elif argument == "--":
            if not compare:
                return None
            separator_seen = True
        elif argument.startswith("-"):
            return None
        elif separator_seen:
            right.append(argument)
        else:
            left.append(argument)
        index += 1
    if compare:
        if not separator_seen or not left or not right:
            return None
        return left, right, json_path
    if not left:
        return None
    return None, left, json_path


if __name__ == "__main__":
    parsed = parse_arguments(sys.argv[1:])
    if parsed is None:
        print(__doc__)
        sys.exit(2)
    sys.exit(main(*parsed))
