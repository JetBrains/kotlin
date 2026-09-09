# Calibration methodology

The question this harness has to answer is *"how much do the endpoint security agents cost?"*, and the only
available experiment is often *"machine A with agents vs. machine B without them"*. That comparison mixes three
independent effects:

1. machine speed (CPU, memory, disk),
2. configuration (cores, heap, GC, parallelism),
3. the agents.

Effect 2 is eliminated by pinning — `scripts/run-bench.sh` does that, and `compare-bench-runs.py` refuses to
compare runs that disagree on it. This document is about effect 1: how to *measure the machine* well enough to
subtract it, and — more importantly — which metrics need no subtraction at all.

## 1. Why a single "cpu score" does not work

The first version of the harness had one probe: a serial `x = x * A + C` loop. It reported the M5 Max and the
M2 Max as equal (ratio 0.98) while the published single-core scores differ by 1.71x, and while the real workload
differed by 1.26x. A serial multiply-add chain measures *one dependent operation per cycle* — it saturates on any
out-of-order core and is sensitive to clock only. It cannot see issue width, branch prediction, cache latency,
memory bandwidth or the allocation path, which is where the generational difference actually is.

Measured on an M5 Max (`fir.bench.instrumentation.probes.suite=full`), the same machine, in one pass:

```
cpu / cpuIntLatency   0.97  ns/op     serial dependency chain
cpuIntThroughput      0.13  ns/op     8 independent chains  -> ILP factor 7.7x
cpuBranch             0.57  ns/op     unpredictable branches
cpuFpu                0.36  ns/op     4 independent fp chains
memLatency32k         1.18  ns/load   L1
memLatency1m          3.56  ns/load   L2
memLatency8m         11.17  ns/load   L2/SLC boundary
memLatency256m      104.60  ns/load   DRAM
memBandwidthRead     34.3  GB/s       single thread
memBandwidthCopy    124.1  GB/s       single thread, read+write
allocRate            20.7  GB/s       JVM allocation + young GC
fileStat              9.5  us/op
fileOpenRead         18.3  us/op
fileOpenReadUnique   17.7  us/op      cold paths, deep tree
fileCreateDelete     82.5  us/op
dirScan               6.5  us/stat
diskReadSequential   26.3  GB/s       warm page cache, i.e. the read() path
diskWriteFsync       22.8  us/op
processExec           8.45 ms/op      fork + exec + wait
```

These numbers span four orders of magnitude and move independently. Collapsing them into one factor is what
produced the wrong conclusions so far.

## 2. The three levels of calibration

### Level 0 — dimensionless metrics, no calibration needed (use these first)

Any metric that is a **ratio of two quantities measured inside the same process on the same machine** cancels
machine speed exactly. These are the numbers to lead with, and the reason the harness records user and system
time separately:

| metric | why it is machine-independent | agent signature |
|---|---|---|
| `threadSystemNanos / threadCpuNanos` (system share) | both scale with the same clock and the same workload | rises: the agent's work is charged to *your* thread as kernel time |
| `findKotlinClass.nanos / findKotlinClass.count` divided by `threadUserNanos` | per-lookup syscall cost relative to the compute of the same compilation | rises if `open`/`mmap` are hooked synchronously |
| `processExec` median divided by the `cpu` probe median | both are ns on the same core | rose 3x — the strongest finding so far |
| iteration 0 vs. iterations > 0 of the same module (`fir.bench.compile.repeat`) | same module, same machine, same process | a cold-path penalty shows here first |

The measured system share was 14.8% (M2, no agents) vs. 17.6% (M5, agents) — a 1.19x ratio that survives despite
the M5 being 20% faster overall. That is a *calibration-free* observation, and it is stronger evidence than any
normalized wall time.

### Level 1 — per-dimension ratios

For everything that is not dimensionless, do **not** normalize by one number. Compute one ratio per dimension
(`scripts/calibrate.py --compare`), grouped as:

```
cpu_latency     = cpu / cpuIntLatency
cpu_throughput  = cpuIntThroughput
cpu_branch      = cpuBranch
cpu_fp          = cpuFpu
mem_latency     = geomean(memLatency32k, memLatency1m, memLatency8m, memLatency256m)
mem_bandwidth   = geomean(memBandwidthRead, memBandwidthCopy)      [inverted: >1 means slower]
alloc           = allocRate                                        [inverted]
disk_latency    = geomean(fileStat, fileOpenRead, fileOpenReadUnique, fileCreateDelete, dirScan, diskWriteFsync)
disk_bandwidth  = diskReadSequential                               [inverted]
exec            = processExec
```

Rules for using them:

* a dimension whose **before/after spread inside one run exceeds 15%** is unusable — it is measuring the machine's
  mood, not the machine. The scripts mark it and exclude it.
* aggregate across repetitions with the **median of medians**, never the mean: one thermal event ruins a mean.
* `disk_*` and `exec` are *not* calibration dimensions when comparing agents-on vs. agents-off — they are the
  thing being measured. Only use them as calibration when comparing two machines in the *same* agent state.

That last point is the fundamental limitation of the cross-machine experiment: on two machines with different
agent states, the disk and exec dimensions are contaminated by the effect under study, so only the CPU and memory
dimensions can calibrate. This is why the single-machine A/B remains the decisive experiment.

### Level 2 — a workload model, with the assumption made explicit

To normalize wall/CPU time you need to know what the workload is bound by. `compare-bench-runs.py` uses an
explicit linear model in the reciprocal-throughput domain:

```
predicted_ratio = w_cpu * cpu_dim + w_mem * mem_dim + w_disk * disk_dim
residual        = measured_ratio / predicted_ratio
```

with defaults `w_cpu = 0.60`, `w_mem = 0.35`, `w_disk = 0.05` (`--weights cpu=…,mem=…,disk=…` to override).
The residual is the number of interest: *what is not explained by the machine*.

The weights are an **assumption, not a measurement**, and the report says so. Two ways to stop guessing:

* **Bound it.** Run the analysis with `w_cpu=1.0` and with `w_mem=1.0` and report the residual range. If both
  extremes lead to the same conclusion, the weights do not matter for that conclusion.
* **Measure it.** The per-phase and side-stats data already decompose the workload: `phases.Analysis` vs.
  `phases.Backend`, `findKotlinClass.count`/`nanosPerCall`, `files`, `lines`, `allocRate`-sensitive GC time.
  With the parallelism sweep (below) you get several operating points per machine and can fit the weights
  instead of assuming them.

## 3. The parallelism = 1 experiment

Everything measured so far was at parallelism 8, which conflates per-core speed, scheduling, memory contention
and GC. Parallelism 1 removes all of it and is the configuration that is **directly comparable to a single-core
benchmark score** — the anchor that tells you whether the harness itself is losing the machine's advantage.

```bash
scripts/run-bench.sh --label m5max-falcon-on --serial --sample 0.05 --compile-repeat 3 --heap 4g --repeat 3
```

* `--serial` = `--cores 1 --parallelism 1`: one core visible to the JVM, so GC and JIT threads are counted too.
* `--sample 0.05` selects a deterministic 5% of the modules by a stable hash of the model file name — the *same*
  modules on every machine and in every repetition (`fir.bench.sample.fraction`/`seed`). The full suite at
  parallelism 1 takes about an hour; 5% takes ~15–25 minutes, which is what makes 3 alternating repetitions
  affordable.
* `--compile-repeat 3` compiles every selected module three times in a row inside the same test. Iteration 0 is
  cold, iterations 1–2 are warm; the measured cold/warm gap on one module was 8.40 s → 2.51 s, so mixing them
  would swamp a 2% effect. `compare-bench-runs.py` reports the warm records separately.
* the heap must be small at parallelism 1 (`4g`), otherwise the young generation is so large that the GC behaviour
  differs from the parallel runs for reasons unrelated to the machine.

What to conclude from it:

| observation | conclusion |
|---|---|
| serial wall ratio ≈ `cpuIntThroughput`/`mem_latency` ratio ≈ published single-core ratio | the harness is fine; the small ratio at parallelism 8 is a scaling/topology effect |
| serial wall ratio ≪ probe ratios | the workload, not the machine, is the limiter (pointer chasing, allocation, GC) — and no calibration will make the machines look different |
| serial system share still ~1.2x with agents on | the agent effect is real and independent of parallelism |

Run the whole matrix unattended with `scripts/run-experiments.sh --label <machine>-<agent state> --set all`
(`--dry-run` first to see the plan): a calibration-only cell first and last, then serial, then the scaling sweep,
then the pinned reference configuration.

## 4. Probe stability (measured, not assumed)

Every run takes the suite twice: `probesBefore` in `manifest.json` (before the first compilation) and
`probesAfter` in `summary.json` (after the last one). The two passes are the built-in self-check: if they disagree
by more than 15% the dimension is unusable and both scripts mark it as such.

The `probesAfter` pass initially disagreed badly, because it started on a heap full of garbage with the GC and JIT
threads still running. The harness now forces a collection and waits 3 s first (`settleBeforeProbes`), which is
the difference between a usable and a useless calibration:

| probe | spread before/after, without settling | with settling |
|---|---|---|
| `memLatency8m` | 59.6% | **1.1%** |
| `memLatency256m` | 15.5% | **0.2%** |
| `memBandwidthCopy` | 8.4% | **0.1%** |
| `fileCreateDelete` | 34.8% | **1.3%** |
| `fileStat` | 20.5% | **4.7%** |
| `dirScan` | 38.9% | **2.3%** |
| `diskReadSequential` | 21.3% | **5.9%** |
| `cpuIntThroughput` | 0.6% | 0.6% |

`diskWriteFsync` remains bimodal (the drive's write cache) even at 256 samples and `memLatency32k` is ~1 ns per
load, so both stay noisy; they are flagged automatically rather than silently averaged.

## 5. Acceptance thresholds

From the four valid runs already collected:

* within-machine repeatability of the paired per-module median: **±1% (M5), ±4% (M2)**;
* therefore a cross-machine or cross-configuration effect is only reportable above **±5%**;
* the probe suite must reproduce itself to within 15% between the before and after pass, otherwise the run is
  thrown away;
* full GC count and observed mean concurrency must match between the runs (they did: 59 GCs, concurrency 7.88).

## 6. What the calibration cannot fix

Two different laptops differ in RAM size, chassis, sustained power budget and network extension. Calibration
subtracts the *speed* difference, not the *behaviour* difference. Every conclusion of the form "the agents cost
X%" derived from a cross-machine comparison is therefore an upper bound with an unresolved confound, and the
harness prints it as such. The decisive experiment remains one machine, agents on vs. off, three alternating
repetitions — for which the ±1% repeatability measured on the M5 is more than sufficient.
