# Measurement harness for the generated modularized tests

`IntelliJFullPipelineTestsGenerated` (and the other `*FullPipelineTestsGenerated` classes) compile one real module
per test, in parallel, so the suite is a realistic and heavy CPU + disk workload. `ModularizedTestInstrumentation`
(`testFixtures/.../modularizedTestInstrumentation.kt`) turns such a run into a machine-comparable measurement.

Why not just look at the CPU consumed by a monitoring agent: an Endpoint Security extension (CrowdStrike Falcon,
Kandji Iru, …) subscribes to `exec`, `open`, `close`, `mmap`, `rename`, `unlink` events, and the `AUTH_*` ones are
synchronous. The cost is therefore paid as extra latency and extra *system* time inside the measured process, not
as the agent's own CPU. The harness records exactly that.

## Running

Use the runner script - it pins everything that must be identical in the runs being compared:

```bash
compiler/fir/modularized-tests/scripts/run-bench.sh --label m5max-falcon-on --repeat 3
```

Run the very same command line on the other machine (or in the other configuration), changing only `--label`.
`--help` lists the options; the defaults are `--cores 8 --parallelism 8 --heap 12g --gc Parallel`.

For a whole matrix of configurations in one unattended session (calibration cells first and last, the serial
cells, the thread-scaling sweep, the pinned reference configuration):

```bash
compiler/fir/modularized-tests/scripts/run-experiments.sh --label m5max-falcon-on --set all --dry-run
compiler/fir/modularized-tests/scripts/run-experiments.sh --label m5max-falcon-on --set all
```

The single most informative cell is the **single-threaded** one, because it is the only configuration directly
comparable to a published single-core score:

```bash
scripts/run-bench.sh --label m5max-falcon-on --serial --sample 0.05 --compile-repeat 3 --heap 4g --repeat 3
```

`--sample` picks a deterministic subset of the modules (a stable hash of the model file name, identical on every
machine), `--compile-repeat` compiles each of them several times so that the cold iteration can be separated from
the warm ones. See [CALIBRATION.md](CALIBRATION.md) for the methodology and for how to read the result.

What the script pins and why:

| Pinned | How | Why |
|---|---|---|
| number of CPUs | `-XX:ActiveProcessorCount` | the JUnit engine derives its parallelism, and the JVM its GC/JIT thread counts, from `availableProcessors`; an 18-core machine otherwise runs 50% more compilations against the same heap than a 12-core one |
| concurrent tests | `-Pkotlin.test.junit5.maxParallelForks` → `junit.jupiter.execution.parallel.config.fixed.parallelism` | same reason, explicitly rather than implicitly |
| heap | `-Pkotlin.test.xmx` / `-Pkotlin.test.xms` (a plain `-Xmx` in `jvmArgs` would be overridden by the build) | full GC time is the single largest confounder; with 8 GB and 18-wide parallelism it reached 510 s vs 103 s and produced individual modules 90x slower |
| GC | `-Pkotlin.test.garbage.collector` | the module deliberately selects no GC by default |
| GC threads, heap pre-touch, code cache flushing | `-Pfir.modularized.jvm.args` | removes the startup/warm-up asymmetry between machines |
| network | `--offline` | the agents being measured also monitor the network |
| sleep / thermal state | `caffeinate`, `run-env.txt` | a throttled or low-power run is not comparable |

Individual properties (all optional, `-P` to Gradle or `-D` to the test JVM) if you drive the run manually:

| Property | Default | Meaning |
|---|---|---|
| `fir.bench.instrumentation` | `true` | `false` disables the harness completely |
| `fir.bench.instrumentation.dir` | `tmp/fir-bench` | root directory for the run directories |
| `fir.bench.instrumentation.label` | `default` | label of the configuration, part of the run directory name |
| `fir.bench.instrumentation.detailed` | `true` | per-thread user/cpu time; `false` leaves wall time only |
| `fir.bench.instrumentation.probes` | `true` | the calibration probe suite |
| `fir.bench.instrumentation.probes.suite` | `full` | `quick` (smaller working sets, fewer repetitions) or `off` |
| `fir.bench.instrumentation.probes.<name>.iterations` | per probe | repetitions of one probe, e.g. `…probes.processExec.iterations=100` |
| `fir.bench.sample.fraction` | `1.0` | compile only a deterministic subset of the modules |
| `fir.bench.sample.seed` | `0` | changes which subset is selected |
| `fir.bench.sample.list` | — | file with the model file names to compile, one per line |
| `fir.bench.compile.repeat` | `1` | compile every selected module N times in a row |

## Output

`tmp/fir-bench/<timestamp>-<label>/`:

* `manifest.json` — machine and JVM description, the pinned `testConfiguration` (parallelism and the `fir.bench.*`
  properties), the list of the active system extensions and of the running security agents, and the probes taken
  before the first compilation;
* `compilations.jsonl` — one JSON object per test: wall time, thread cpu/user/**system** time, per-phase
  `nanos`/`userNanos`/`cpuNanos`/`systemNanos`, the number and the total time of Java/Kotlin class lookups
  (the most syscall-intensive part of the pipeline), `inFlightAtStart`/`inFlightAtEnd` (how many compilations ran
  in parallel at that moment), the per-compilation `gcDeltaMillis`/`gcDeltaCount`/`jitDeltaMillis` and
  `iteration`/`repeatCount` (`iteration = 0` is the cold compilation of that module, see `fir.bench.compile.repeat`);
* `summary.json` — run totals and the probes taken after the last compilation (the cleanest ones, nothing else runs);
* `run-env.txt`, `gradle.log` — written by the runner script: power/thermal state, system extensions, the exact
  command line.

The probes are one per *dimension* - a single "cpu score" is provably insufficient, see
[CALIBRATION.md](CALIBRATION.md): `cpu`/`cpuIntLatency`, `cpuIntThroughput`, `cpuBranch`, `cpuFpu`;
`memLatency32k`/`1m`/`8m`/`256m` (pointer chase, one dependent load per operation), `memBandwidthRead`,
`memBandwidthCopy`, `allocRate`; `fileOpenRead` and `fileStat` (repeated access to the same 512 files - the
friendliest case for any per-file caching in the agent), `fileOpenReadUnique` (a deep tree, each file opened
exactly once - the case a caching agent cannot amortize), `fileCreateDelete`, `dirScan`, `diskReadSequential`,
`diskWriteFsync`; `processExec` (`fork` + `exec` + `wait`; `ES_EVENT_TYPE_AUTH_EXEC` is synchronous, so this is
where an agent is most visible). Every probe records `kind`, `unit`, `opsPerMeasurement` and the derived
`medianNanosPerOp` / `medianBytesPerSecond`, so no consumer needs to know the shape of a particular probe.

Caveats when reading the records:

* `processCpuNanos`, `gcDelta*` and `jitDeltaMillis` are process-wide, so under parallel execution they are shared
  between the simultaneously running compilations. They are useful for *explaining* an outlier, not for comparing.
* `phases.Initialization` may include a constant shared between the modules; `wallNanos`, `threadCpuNanos`,
  `threadUserNanos` and `threadSystemNanos` are per-compilation and are the numbers to trust.

## Comparing two runs

```bash
compiler/fir/modularized-tests/scripts/compare-bench-runs.py <baseline-run-dir> <run-dir> --top 10
```

The script starts with the things that can *invalidate* the comparison and shouts about them: a configuration
difference (CPU model, `availableProcessors`, heap, GC, parallelism), full GC time differing by more than 2x,
observed concurrency differing by more than 10%, an unreliable CPU probe.

The headline numbers are **paired per-module ratios** (p10/p25/median/p75/p90/p99 and the geometric mean), not
sums: a handful of memory-hungry modules can be 90x slower and turn a 2% difference into a "2x" one. The sums are
still printed, in a separate section labelled as tail-sensitive throughput.

How to read the result: the script builds a *predicted* ratio from the calibration dimensions and reports the
residual (measured / predicted) — what is left is not explained by the machine. Start, however, with the
dimensionless metrics that need no calibration at all: the system-time share, the per-class-lookup time relative
to the user time of the same compilation, and `processExec` relative to the `cpu` probe. A configuration with an
active endpoint security agent shows a normal `threadUserNanos`, an inflated `threadSystemNanos` and system-time
share, and a dramatically slower `processExec` probe. The per-dimension calibration report is
`scripts/calibrate.py`; the methodology and the acceptance thresholds are in [CALIBRATION.md](CALIBRATION.md).

For the cleanest numbers: `--repeat 3` on both configurations, alternate them (A/B/A/B/A/B) rather than running
all of A and then all of B, keep the machine otherwise idle, and use the same power state.

## Benchmarking the Kotlin build itself

The suite above measures a single process doing CPU and file work, and it forks almost nothing - so it cannot see
the largest effect measured so far, ~2.8x on `fork` + `exec` + `wait`. The Kotlin Gradle build is the complementary
workload: it forks constantly (workers, the Kotlin daemon, `javac`, `git`) and its configuration phase stats tens
of thousands of files.

The build is a poor *instrument* on its own, though - incremental state, daemon warm-up, dependency resolution,
the configuration cache and the build cache all make two invocations incomparable. Use `gradle-profiler`, which
handles exactly that (warm-ups, repetitions, controlled source mutations, per-iteration CSV with median/stddev):

```bash
brew install gradle-profiler
gradle-profiler --benchmark --measure-gc \
    --scenario-file compiler/fir/modularized-tests/scripts/gradle-profiler-scenarios.conf \
    --output-dir tmp/gradle-profiler/<label>
```

`scripts/gradle-profiler-scenarios.conf` defines: `configuration` (the `help` task - configuration phase only,
the purest syscall-latency scenario), `abi_change` and `non_abi_change` (incremental compilation, exec-heavy and
read-heavy respectively), and, on demand, `cold_daemon` and `dist`. Compare the medians in `benchmark.csv` of the
two output directories and ignore differences below the reported standard deviation.

Do **not** wire the benchmark into the build itself (a task that measures the build that runs it): the numbers
would be affected by the very configuration and daemon state they try to measure, and the build cache would make
repeated runs meaningless. Keep the measurement outside: `run-bench.sh` for the controlled compiler workload,
`gradle-profiler` for the end-to-end build.

Useful external observation alongside a run (macOS):

* `sudo powermetrics --samplers cpu_power,thermal -i 5000` - E/P core residency, package power, thermal pressure;
  a throttled run explains more than any agent;
* `ps -o time,comm -p <pid of the ESF extension>` before and after a run - the CPU actually consumed by the agent
  processes themselves (`systemextensionsctl list` gives the bundle identifiers);
* `sudo fs_usage -w -f filesys` for a few seconds - the raw syscall stream with latencies, to confirm which
  operations are being hooked synchronously.

## A note on `detailedPerf`

`PerformanceManager` starts measuring the `Initialization` phase in its constructor, while `detailedPerf` is
switched on afterwards (from the compiler arguments). Before the fix in `PerformanceManager.kt` the user/cpu
baseline of that phase was therefore zero, and the phase reported *the whole lifetime cpu time of the thread*.
In the CLI it is barely visible (a fresh thread), but here the compilations run on reused `ForkJoinPool` workers,
so the per-phase user/cpu numbers were meaningless. Runs recorded before that fix are detected and flagged by
`compare-bench-runs.py`; use only their `threadCpuNanos`/`threadUserNanos`/`threadSystemNanos`.
