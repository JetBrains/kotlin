"""Isolated classpath-snapshot experiments. Run from any directory; artifacts stay in build/.

Capture before edits: python3 benchmarks/snapshot-experiments.py capture baseline
Compile captured sources: python3 benchmarks/snapshot-experiments.py compile baseline
Run counterbalanced forks: python3 benchmarks/snapshot-experiments.py run baseline,jar --tag confirm
"""
import argparse
import csv
import difflib
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import statistics

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "benchmarks/build/snapshot-experiments-20260909"
JAR = ROOT / "benchmarks/build/benchmarks/test/jars/benchmarks-test-jmh-2.5.255-SNAPSHOT-JMH.jar"
JAVA_HOME = subprocess.check_output(["/usr/libexec/java_home", "-v", "21"], text=True).strip()
ENV = dict(os.environ, JAVA_HOME=JAVA_HOME)
JAVA = str(Path(JAVA_HOME) / "bin/java")
SOURCES = [
    "compiler/incremental-compilation-impl/src/org/jetbrains/kotlin/incremental/classpathDiff/ClasspathEntrySnapshotter.kt",
    *["compiler/incremental-compilation-impl/src/org/jetbrains/kotlin/incremental/classpathDiff/impl/" + name + ".kt"
      for name in ["BasicClassInfo", "ClassFile", "ClassListSnapshotter", "SingleClassSnapshotter", "InlinedClassSnapshotter"]],
    "build-common/src/org/jetbrains/kotlin/incremental/KotlinClassInfo.kt",
    "build-common/src/org/jetbrains/kotlin/incremental/impl/ExtraClassInfoGenerator.kt",
    "compiler/frontend.java/src/org/jetbrains/kotlin/inline/inlineUtil.kt",
]


def classpath(variant, compatibility=False):
    paths = [OUT / part / "classes" for part in variant.split("+")]
    if compatibility:
        paths.append(OUT / "compatibility")
    return os.pathsep.join(map(str, [*paths, JAR]))


def capture(name, sources):
    target = OUT / name / "sources"
    target.mkdir(parents=True, exist_ok=False)
    for source in sources:
        shutil.copyfile(ROOT / source, target / Path(source).name)
    (target.parent / "manifest.json").write_text(json.dumps({
        "head": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
        "sources": {s: hashlib.sha256((ROOT / s).read_bytes()).hexdigest() for s in sources},
        "jmhJarSha256": hashlib.sha256(JAR.read_bytes()).hexdigest(),
    }, indent=2))


def compile_variant(name):
    target = OUT / name
    sources = sorted((target / "sources").iterdir())
    subprocess.run([str(ROOT / "dist/kotlinc/bin/kotlinc"), *map(str, sources),
                    "-classpath", str(JAR), "-Xfriend-paths=" + str(JAR), "-opt-in=org.jetbrains.kotlin.K1Deprecation",
                    "-d", str(target / "classes")], cwd=ROOT, env=ENV, check=True)


def assemble_baseline():
    target = OUT / "baseline-all" / "sources"
    target.mkdir(parents=True, exist_ok=False)
    for source in SOURCES:
        name = Path(source).name
        choices = [OUT / "baseline/sources" / name, OUT / "asm-baseline/sources" / name,
                   OUT / "metadata/baseline" / source]
        saved = next((p for p in choices if p.is_file()), None)
        if saved:
            shutil.copyfile(saved, target / name)
        else:
            assert name == "InlinedClassSnapshotter.kt"
            (target / name).write_bytes(subprocess.check_output(["git", "show", "HEAD:" + source], cwd=ROOT))


def export_baseline_patch():
    # Apply to final sources in an isolated checkout to reconstruct the measured pre-experiment baseline.
    diff = []
    for source in SOURCES:
        before = (ROOT / source).read_text().splitlines(keepends=True)
        after = (OUT / "baseline-all/sources" / Path(source).name).read_text().splitlines(keepends=True)
        diff.extend(difflib.unified_diff(before, after, fromfile="a/" + source, tofile="b/" + source))
    (ROOT / "benchmarks/snapshot-experiments-baseline.patch").write_text("".join(diff))


def baseline_from_patch(name):
    target = OUT / name
    tree = target / "tree"
    (target / "sources").mkdir(parents=True, exist_ok=False)
    for source in SOURCES:
        (tree / source).parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(ROOT / source, tree / source)
    subprocess.run(["git", "apply", "--directory=" + str(tree.relative_to(ROOT)),
                    str(ROOT / "benchmarks/snapshot-experiments-baseline.patch")], cwd=ROOT, check=True)
    for source in SOURCES:
        shutil.copyfile(tree / source, target / "sources" / Path(source).name)


def run(args):
    variants = args.variants.split(",")
    for round_number in range(args.rounds):
        order = variants if round_number % 2 == 0 else variants[::-1]
        for case in args.cases.split(","):
            for variant in order:
                tag = f"{args.tag}-{round_number + 1}-{variant.replace('/', '-')}-{case}"
                command = [JAVA, "-DclasspathSnapshot.stdlib=" + str(ROOT / args.artifact),
                           "-cp", classpath(variant),
                           "org.openjdk.jmh.Main", "-f", "1", "-wi", str(args.warmups),
                           "-i", str(args.iterations), "-w", "1s", "-r", "1s", "-prof", "gc",
                           "-jvmArgsAppend", "-Xms1g -Xmx1g -XX:+UseG1GC", "-foe", "true",
                           "-rf", "json", "-rff", str(OUT / (tag + ".json"))]
                if case == "stdlib":
                    command += [".*StdlibClasspathEntrySnapshotBenchmark.*", "-p", "granularity=" + args.granularity]
                else:
                    command += [".*jmh.ClasspathEntrySnapshotBenchmark.snapshot" + ("Directory" if case == "directory" else "Jar"),
                                "-p", "classCount=10000", "-p", "publicClasses=true", "-p", "granularity=" + args.granularity]
                print("RUN", tag, subprocess.check_output(["uptime"], text=True).strip(), flush=True)
                with (OUT / (tag + ".log")).open("w") as log:
                    subprocess.run(command, cwd=ROOT, env=ENV, stdout=log, stderr=subprocess.STDOUT, check=True)
                for row in json.loads((OUT / (tag + ".json")).read_text()):
                    metric = row["primaryMetric"]
                    alloc = row["secondaryMetrics"]["gc.alloc.rate.norm"]["score"]
                    print(row["params"], f'{metric["score"]:.3f} ms/op; {alloc / 1024 / 1024:.3f} MiB/op', flush=True)


def verify(args):
    inputs = ["dist/kotlinc/lib/" + name + ".jar" for name in
              ["kotlin-stdlib", "kotlin-reflect", "kotlinx-coroutines-core-jvm"]]
    inputs.append("compiler/incremental-compilation-impl/testData/org/jetbrains/kotlin/incremental/classpathDiff/ClasspathSnapshotterTest")
    expected = None
    for variant in args.variants.split(","):
        cp = classpath(variant, compatibility=True)
        output = subprocess.check_output([JAVA, "-Xmx1g", "-cp", cp,
                                          "org.jetbrains.kotlin.benchmarks.jmh.ClasspathSnapshotCompatibilityKt", *inputs],
                                         cwd=ROOT, env=ENV)
        (OUT / (variant.replace('/', '-') + "-compatibility.txt")).write_bytes(output)
        if expected is None:
            expected = output
        else:
            assert output == expected, f"Serialized snapshot mismatch: {variant}"
        print(variant, len(output.splitlines()), "serialized snapshots match", flush=True)


def memory(args):
    for round_number in range(args.rounds):
        variants = args.variants.split(",")
        if round_number % 2:
            variants.reverse()
        for variant in variants:
            for granularity in args.granularity.split(","):
                tag = f"{args.tag}-{round_number + 1}-{variant.replace('/', '-')}-{granularity}"
                command = ["/usr/bin/time", "-l", JAVA, "-Xms128m", "-Xmx" + args.memory_heap, "-XX:+UseG1GC", "-cp",
                           classpath(variant, compatibility=True),
                           "org.jetbrains.kotlin.benchmarks.jmh.ClasspathSnapshotMemory", str(ROOT / args.artifact), granularity]
                result = subprocess.run(command, cwd=ROOT, env=ENV, capture_output=True, text=True, check=True)
                (OUT / (tag + ".memory.log")).write_text(result.stdout + result.stderr)
                print(tag, result.stdout.strip(), flush=True)
                print(next(line.strip() for line in result.stderr.splitlines() if "maximum resident set size" in line), flush=True)


def summarize(args):
    rows = []
    for path in sorted(OUT.iterdir()):
        if not path.name.startswith(args.tag + "-") or path.suffix != ".json" or path.name.endswith("-summary.json"):
            continue
        for result in json.loads(path.read_text()):
            rows.append({"file": path.name, "benchmark": result["benchmark"], "params": result["params"],
                         "msPerOp": result["primaryMetric"]["score"],
                         "iterationMsPerOp": result["primaryMetric"]["rawData"],
                         "bytesPerOp": result["secondaryMetrics"]["gc.alloc.rate.norm"]["score"]})
    for variant in args.variants.split(","):
        matches = [row for row in rows if any(row["file"].endswith(f"-{variant}-{case}.json")
                                             for case in ["jar", "directory", "stdlib"])]
        for benchmark in sorted({row["benchmark"] for row in matches}):
            selected = [row for row in matches if row["benchmark"] == benchmark]
            times = [row["msPerOp"] for row in selected]
            allocations = [row["bytesPerOp"] / 1024 / 1024 for row in selected]
            print(variant, benchmark.rsplit(".", 1)[-1], len(times),
                  f'{statistics.mean(times):.3f} [{min(times):.3f}, {max(times):.3f}] ms/op;',
                  f'{statistics.mean(allocations):.3f} MiB/op')
    (OUT / (args.tag + "-summary.json")).write_text(json.dumps(rows, indent=2))


def export_results():
    with (ROOT / "benchmarks/classpath-snapshot-experiments.csv").open("w", newline="") as output:
        writer = csv.writer(output)
        writer.writerow(["run", "benchmark", "parameters", "ms/op", "bytes/op", "iteration ms/op", "retained bytes/snapshot", "peak RSS bytes"])
        for path in sorted(OUT.iterdir()):
            if path.name.endswith(".memory.log"):
                text = path.read_text()
                writer.writerow([path.name, "memory", "", "", "", "",
                                 re.search(r"retainedBytesPerSnapshot=(\d+)", text)[1],
                                 re.search(r"(\d+)\s+maximum resident set size", text)[1]])
            elif path.suffix == ".json" and not path.name.endswith("-summary.json"):
                data = json.loads(path.read_text())
                if not isinstance(data, list):
                    continue
                for result in data:
                    writer.writerow([path.name, result["benchmark"], json.dumps(result["params"], sort_keys=True),
                                     result["primaryMetric"]["score"], result["secondaryMetrics"]["gc.alloc.rate.norm"]["score"],
                                     json.dumps(result["primaryMetric"]["rawData"]), "", ""])


parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("action", choices=["capture", "compile", "run", "verify", "assemble-baseline", "export-baseline", "baseline-from-patch", "memory", "summarize", "export-results"])
parser.add_argument("variants")
parser.add_argument("--sources", nargs="+", default=SOURCES)
parser.add_argument("--tag", default="screen")
parser.add_argument("--rounds", type=int, default=4)
parser.add_argument("--warmups", type=int, default=5)
parser.add_argument("--iterations", type=int, default=5)
parser.add_argument("--cases", default="jar,stdlib")
parser.add_argument("--granularity", default="CLASS_LEVEL")
parser.add_argument("--artifact", default="dist/kotlinc/lib/kotlin-stdlib.jar")
parser.add_argument("--memory-heap", default="512m")
args = parser.parse_args()
if args.action == "capture":
    capture(args.variants, args.sources)
elif args.action == "compile":
    compile_variant(args.variants)
elif args.action == "verify":
    verify(args)
elif args.action == "assemble-baseline":
    assemble_baseline()
elif args.action == "export-baseline":
    export_baseline_patch()
elif args.action == "baseline-from-patch":
    baseline_from_patch(args.variants)
elif args.action == "memory":
    memory(args)
elif args.action == "summarize":
    summarize(args)
elif args.action == "export-results":
    export_results()
else:
    run(args)
