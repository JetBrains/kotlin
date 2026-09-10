# Classpath snapshot performance review

Reviewed `239cc7fd77ef` against master at `9bc7d75a47bb` on 2026-09-09.

## Decision

Keep only the small `DirectoryReader` change that computes each relative path once.
Restore the reader interface, JAR implementation and result-map construction to master.
The retained change reliably reduces allocation; the measurements do **not** establish
an equivalent wall-clock or end-to-end build improvement.

## Why the original evidence was insufficient

- The original synthetic input is a private, empty Kotlin class copied under different
  entry names. It exercises metadata parsing, but is classified as inaccessible and
  does not exercise ABI hashing. Keep it as an explicitly selectable overhead case,
  not as the sole representative workload.
- On OpenJDK 21, `ZipFile.getInputStream(entry)` still looks up the entry by name unless
  it matches the single cached last entry. The preceding `getEntry(name)` in the
  original implementation populates that cache. Retaining every `ZipEntry` therefore
  does not remove a name lookup per class; it avoids reconstructing an entry, while
  retaining entry metadata throughout snapshotting.
  See [OpenJDK 21 ZipFile](https://github.com/openjdk/jdk21u/blob/master/src/java.base/share/classes/java/util/zip/ZipFile.java).
- Enumeration order matters to a sorting benchmark. The synthetic JAR uses reversed
  numbered names; it does not cover all orderings encountered in real archives.
- One fork and one synthetic workload are insufficient to generalize a speedup to
  real dependency snapshotting, let alone an incremental build.

## Experiments

JMH 1.37, JBR/OpenJDK 21.0.8, one thread, G1, fixed 1 GiB heap, `-prof gc`.
The screening runs used one fork and 3 x 1 s warmup/measurement iterations.
The confirmation runs used three forks and 5 x 1 s warmup/measurement iterations.
Further comparisons changed variant order across three one-fork rounds, each with
5 x 1 s warmup and measurement iterations. Setup and input generation were outside
the timed region; these are warm-filesystem-cache benchmarks.

Each source variant was compiled with the same local Kotlin compiler, against the
same benchmark JAR. Its classes were prepended to the classpath of separate JMH
forks. Only `ClasspathEntrySnapshotter.kt` differed between implementations; no
working-tree source switching or dependency rebuild was needed between variants.

Workloads:

- 10,000 private Kotlin classes: the original overhead-oriented fixture.
- 10,000 distinct public Java classes, with matching entry/internal names, a field
  and a method; class-level snapshots, inline-local parsing disabled.
- The built Kotlin standard-library JAR, both snapshot granularities, inline-local
  parsing enabled.

Three-fork confirmation results (mean milliseconds/operation):

| Workload | Master | Proposed rewrite | Retained path-only change |
|---|---:|---:|---:|
| Public classes, directory | 272.319 | 240.451 | 265.762 |
| Public classes, JAR | 36.845 | 33.536 | 36.117 |
| Stdlib, class level | 43.358 | 41.350 | Not separately measured |
| Stdlib, member level | 43.891 | 41.460 | Not separately measured |

**These timings did not remain stable across repeats.** In the first subsequent
comparison round, master/rewrite directory times were 251.386/251.489 ms, and stdlib
class-level times were 40.761/40.497 ms. Later rounds suffered severe system
interference: unchanged workloads slowed several-fold, and the host's reported
load averages reached 42.43 / 25.31 / 14.34. Those runs are retained as evidence of
measurement instability, not discarded to select a favorable speedup. There is no
reliable confirmation of the advertised 13% directory / 33% JAR speedups here.
The retained variant's JAR code is byte-for-byte master source; differences between
its JAR timing and master's are another control demonstrating run-to-run variation.

Allocation was considerably more stable (MiB allocated per operation, **not retained
heap**; three-fork public-class results):

| Input | Master | Proposed rewrite | Retained path-only change |
|---|---:|---:|---:|
| Directory | 142.974 | 110.422 | 116.727 |
| JAR | 226.495 | 225.017 | 226.393 |

The retained change saves about **26 MiB / 18%** on this directory workload with
only one small production-code hunk. The extra complexity of the proposed rewrite
is not justified by a robust real-JAR improvement in these experiments.

Also evaluated, but not adopted:

- Hash-based deduplication followed by sorting, and sorting names followed by
  `distinct()`: no consistent real-JAR benefit.
- Removing the known directory path prefix instead of calling `relativeTo`:
  allocation fell further to about 90.6 MiB, but timing did not establish an
  additional win. This needs dedicated root/relative-path/symlink and Windows
  coverage before replacing the standard path semantics.

Raw local JMH JSON/logs and the source-variant comparison script are in
`benchmarks/build/performance-review/` (ignored build artifacts). Prefixes `screen`,
`confirm`, and `paired1` through `paired3` distinguish the experiments; `paths` is
the retained implementation.

## Running the improved benchmarks

```bash
./gradlew :benchmarks:testClasspathSnapshotBenchmark -q
```

The dedicated suite covers 100/1,000/10,000 classes, private/public inputs, both
snapshot granularities, directory/JAR inputs, and the stdlib JAR. Its defaults are
three forks and five one-second warmup and measurement iterations. The build passes
the stdlib JAR through `classpathSnapshot.stdlib` rather than accidentally measuring
an assembled JMH JAR containing bundled dependencies.

For a targeted run, assemble `:benchmarks:testBenchmarkJar`, invoke
`org.openjdk.jmh.Main` with that JAR on the classpath, select
`ClasspathEntrySnapshotBenchmark`, and set `classCount`, `publicClasses`, and
`granularity`. Stdlib runs also require `-DclasspathSnapshot.stdlib=<stdlib-jar>` in
the forked JVM. Use `-jvmArgsAppend` rather than replacing inherited JVM arguments,
so this property reaches the fork. Repeat A/B runs in a quiet environment before
making throughput claims, and measure real incremental builds separately.

## Correctness coverage

The expanded snapshotter tests cover both granularities and both inline-local
parsing settings: 16 invocations across four scenarios. They check ordered keys for
both directories and JARs, distinct class contents against independent snapshots,
empty inputs, mixed-case filtering, and duplicate ZIP entries with differing contents
where the last entry wins.

## TL;DR

The broad speedup claim is not reliably reproduced. Keep the small, proven allocation
reduction, restore the rest to master, and retain stronger benchmarks and regression
tests. A quiet, counterbalanced benchmark run is needed before claiming a runtime win.
