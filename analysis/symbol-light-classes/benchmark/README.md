# Symbol light classes benchmarks

Local tools of the symbol light classes (SLC) performance investigation. They are not for merge.

The results and the conclusions are in the investigation document: https://claude.ai/artifact/DvLieQdb8DwSe3PtJx3y3h

## Contents

- `SymbolLightClassesBenchmark` in `analysis/symbol-light-classes/tests`: the Kotlin benchmark.
  Its KDoc describes the phases and the system properties.
- `testData/benchmark/synthetic40.kt`: the generated code base of the benchmark, made by `gen.py 40`.
- The IntelliJ benchmark `SymbolLightClassesJavaHighlightingBenchmarkTest` is on the IntelliJ branch `belkov/slc-perf-bench`.
- The scripts in this directory run the benchmarks, compare states and aggregate profiles.

## Setup

The scripts write all results to the directory in `SLC_BENCH_WORK`.
The IntelliJ scripts use the IntelliJ checkout in `INTELLIJ_HOME` (default: `~/code/intellij`).

```bash
export SLC_BENCH_WORK=/path/to/work
cd analysis/symbol-light-classes/benchmark

# The CPU agent: appends the process CPU, wall and GC time of a JVM to a file at exit
mkdir -p $SLC_BENCH_WORK/agent
javac -d $SLC_BENCH_WORK/agent CpuAgent.java
printf 'Premain-Class: CpuAgent\n' > $SLC_BENCH_WORK/agent/MANIFEST.MF
jar cfm $SLC_BENCH_WORK/cpu-agent.jar $SLC_BENCH_WORK/agent/MANIFEST.MF -C $SLC_BENCH_WORK/agent CpuAgent.class

# The inputs of the IntelliJ benchmark: the Kotlin files of synthetic40.kt and 40 Java files that use them
python3 split.py ../testData/benchmark/synthetic40.kt $SLC_BENCH_WORK/ijbench/kotlin/bench
python3 genjava.py 40 $SLC_BENCH_WORK/ijbench/java
```

On macOS, `TMPDIR` must be the system temporary directory, or the test security policy rejects the test JVM.

## Kotlin benchmark

| Script | What it does |
|---|---|
| `slcbench.sh <label> ['-Dkey=value\|...'] [jfr]` | Prebuilds, then runs the benchmark once. The results go to `$SLC_BENCH_WORK/bench/slc-bench.tsv`. |
| `ab.sh <series> <rounds> <commit-a> <commit-b>` | Runs A and B in turn, `<rounds>` times each. Summarize with `abtable.py $SLC_BENCH_WORK/bench/slc-bench.tsv <series>`. |
| `ladder.sh <series> <rounds> <name>=<commit>...` | Runs every state per round, in a rotated order. Summarize with `laddertable.py`. |

`ab.sh` and `ladder.sh` check out `analysis/symbol-light-classes/src` and `analysis/light-classes-base/src` of each commit into
the working tree. At the end, they restore the sources of `HEAD`. Commit your changes before you start them.

The retained memory of the light classes is measured with a class histogram:

```bash
./slcbench.sh hist-head '-Dslc.bench.histogram=true|-Dslc.bench.phases=render_warm|-Dslc.bench.rounds=1|-Dslc.bench.warmup=0'
```

The log lists the classes with the largest retained size, and `$SLC_BENCH_WORK/bench/slc-retained.tsv` gets all of them.

## IntelliJ benchmark

| Script | What it does |
|---|---|
| `build-ide-jar.sh <label> <commit>` | Builds `symbol-light-classes-for-ide` from the SLC sources of a commit into `$SLC_BENCH_WORK/ijjars/<label>.jar`. |
| `use-ide-jar.sh <label>` | Puts a jar into the Kotlin snapshot repository of IntelliJ. The first call saves the original jar as `orig`. |
| `ij-bench.sh <label> <jar-label> [rounds] [jfr]` | Puts the jar in place and runs the benchmark test. The results go to `$SLC_BENCH_WORK/ij/ijbench.tsv`. Summarize an A/B series with `ijtable.py`. |
| `ij-suites-ab.sh <series> <rounds> <jar-a> <jar-b>` | Runs three IntelliJ test suites with the CPU agent for both jars. Summarize with `cputable.py`. |

Bazel recompiles the dependent IntelliJ modules once for each new jar, so the first run after a jar swap takes longer.

## Profiles

`jfr.sh`, `slcbench.sh ... jfr`, `ij-jfr.sh` and `ij-bench.sh ... jfr` record JFR with 2,048-frame stacks and 1 ms sampling
(`profile-1ms.jfc`). `JfrAgg.java` aggregates a recording into `$SLC_BENCH_WORK/jfr/<label>.cpu.collapsed` and
`<label>.alloc.collapsed`. These scripts analyze the collapsed stacks:

- `collapsed.py`: inclusive and self shares, callers and callees of frames
- `tree.py`: the merged call tree below a frame
- `leafgroups.py` with `slc-groups.tsv`: the cost by subsystem, from the innermost matching frame
- `slcshare.py`: the share of SLC production code and what it spends time in
