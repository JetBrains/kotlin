### TL;DR — what you need to know

**Yes, the final implementation makes the measured classpath snapshotting work
faster. The original broad rewrite did not convincingly do that; the later,
more targeted changes did.**

- **Measured result:** snapshotting the tested real library JARs took about
  **12–20% less time** and allocated **34–44% fewer temporary bytes**. For example,
  the Kotlin standard library went from about **41 ms to 33 ms per snapshot**.
- **What we kept:** calculate directory paths once, avoid oversized temporary
  buffers when reading JAR entries, decode Kotlin metadata once, and reuse the
  class-file reader while processing the same loaded class.
- **What we removed:** the broad reader/collection rewrite and a class-byte cache.
  Their demonstrated benefits did not justify their added complexity.
- **What this does NOT mean:** builds are not proven to be 12–20% faster. This
  measures one operation with warm filesystem caches. A reliable directory
  speedup and lower peak process memory were **not established**.
- **Correctness:** the investigation finished with **66 passing tests** and
  **48 matching serialized-snapshot cases** between baseline and final code.

**Bottom line:** keep the small changes that remove real repeated work. Describe
this as a measured snapshot-operation improvement, not a universal build-speed or
memory-usage claim.

### Scope and how to read this report

This is the consolidated result of the investigation on **2026-09-09**. It brings
together the original branch review, subsequent optimization experiments,
rejected approaches, correctness checks, memory checks, and reproduction details.
Writing this report did not rerun benchmarks or change the implementation.

Read the TL;DR for the decision, the confirmation table for the measured effect,
and the remaining sections for the reasoning and audit trail. Individual timing
samples and per-fork measurements remain in the linked evidence files rather than
being duplicated as hundreds of raw numbers here.

There are two distinct comparisons:

| Investigation | Baseline | Candidate | Conclusion |
|---|---|---|---|
| Initial branch review | Parent `9bc7d75a47bb` | Proposed rewrite `239cc7fd77ef` and smaller alternatives | Retain directory path reuse for allocation savings; runtime claim unproven. |
| Follow-up experiments | The retained **path-only implementation**, not the original parent | JAR-buffer, metadata, ASM, and byte-cache experiments | Keep the first three; remove the byte cache. Final combined runtime gains reproduced on tested real libraries. |

**Do not add percentages from these comparisons.** The second baseline already
contains the directory path improvement. The initial report's statement that only
one small production hunk remained described that stage, not the final code.

### What classpath snapshotting actually does

A classpath entry is a directory or JAR containing class files. Its snapshot
records information relevant to incremental compilation: changing implementation
details that cannot affect a consumer should not unnecessarily recompile it,
while ABI and relevant inline-code changes must be detected.

The operation proceeds roughly as follows:

1. Enumerate paths, exclude directories/resources, root `module-info.class`, and
   `META-INF` classes, with the existing case-insensitive filtering rules.
2. Sort accepted paths deterministically. The original JAR reader also
   deduplicates entry names using a sorted set.
3. Associate paths with lazy byte providers; do not eagerly load the whole entry.
4. Read class bytes and basic class information. Inaccessible classes can skip
   expensive ABI extraction.
5. Produce Java/Kotlin snapshots. Kotlin processing also needs metadata and
   relevant inline-function/accessor hashes. Inline-local-class support can
   require nonsequential access and rereading other classes.
6. Assemble an insertion-ordered result map matching the deterministic path order.

`CLASS_LEVEL` and `CLASS_MEMBER_LEVEL` select different snapshot granularities.
Inline-local-class parsing and expanded type aliases are separate settings.
Preserving their behavior, filtering, ordering, resource closing, and serialized
output mattered more than making a benchmark loop look simpler.

Unchanged artifacts already benefit from cacheable Gradle transforms. These
experiments speed up work when snapshotting actually runs; they do not establish
an improvement for a dependency whose cached snapshot is already reused.

### Initial review: why the original speedup claim was not accepted

The branch advertised approximately **13% directory / 33% JAR** runtime gains.
Several problems prevented treating those as reliable results:

- The original synthetic fixture copied the same **private, empty Kotlin class**
  under different entry names. It incurred parsing but skipped normal accessible
  class ABI hashing; it did not model distinct public classes.
- Reverse-numbered archive names favored a particular sorting scenario. Real JARs
  have different entry orderings and mixtures of classes and resources.
- One fork and one synthetic workload were insufficient to generalize.
- Rankings changed across runs, including differences between variants with
  **identical JAR code**. Narrow within-run error bars do not eliminate host drift.

#### Reader and collection decisions

| Proposed change | Finding | Decision |
|---|---|---|
| Compute directory-relative paths once | Previously computed in both filtering and mapping for accepted files. Consistent allocation reduction. | Keep without replacing the reader abstraction. |
| Construct providers during traversal and retain `File` objects | Avoids resolving names again; full rewrite saved roughly another 6 MiB on the directory fixture, but retained more traversal objects and had no reliable additional runtime win. | Revert. |
| Retain `ZipEntry` objects | Avoids reconstructing entries, but does not remove the supposed extra name lookup on OpenJDK 21; retains entry metadata throughout snapshotting. | Revert. |
| Replace ZIP sorting/deduplication | General complexity remains `O(n log n)`. Ordering influences results; capacity based on `ZipFile.size()` also includes excluded resources/directories. No robust real-JAR gain established. | Revert. |
| Pre-size the result map and use indexed insertion | Isolated screening saved only about 0.36 MiB, or 0.16%, on the public JAR fixture; no clear timing gain. | Revert the custom loop/capacity helper. |

On OpenJDK 21, `ZipFile.getInputStream(entry)` uses its cached central-directory
position only if the entry name matches `lastEntryName`; otherwise it calls
`getEntryPos(entry.name, false)`. The original preceding `getEntry(name)` populates
that cache. Enumerating and sorting `ZipEntry` objects first does not turn them
into direct position handles: opening streams generally still performs name
lookups. This is JDK-specific implementation evidence, not a ZIP API guarantee.
See the [OpenJDK 21 implementation](https://github.com/openjdk/jdk21u/blob/master/src/java.base/share/classes/java/util/zip/ZipFile.java).

Restoring the original reader also preserved its duplicate-entry behavior rather
than introducing a new assumption about which duplicate a JDK selects. No
snapshot-correctness regression was established in the proposal: rejection meant
**insufficient demonstrated value for complexity**, not that every removed line
had literally zero benefit.

#### Initial fresh measurement results

JMH 1.37, JBR/OpenJDK 21.0.8, one thread, G1, fixed 1 GiB heap, `-prof gc`.
Fixture generation was outside measurement. The screen used one fork and three
one-second warmup/measurement iterations; confirmation used three forks and five
one-second warmup/measurement iterations, with reversed implementation order.

The improved public fixture contains 10,000 distinct Java classes with matching
entry/internal names, a field, and a method. Synthetic runs use class-level
snapshots without inline-local parsing. Real stdlib runs enable inline-local
parsing and cover both granularities.

Time below is **ms/op, mean ± JMH's reported 99.9% error**, not a causal estimate:

| Workload | Original parent | Full proposal | Path-only |
|---|---:|---:|---:|
| Public directory | 260.311 ± 2.633 | 274.078 ± 12.135 | 280.052 ± 25.957 |
| Public JAR | 35.756 ± 0.692 | 34.725 ± 0.614 | 37.635 ± 0.337 |
| Stdlib, class level | 42.276 ± 0.338 | 41.918 ± 0.720 | 43.820 ± 0.233 |
| Stdlib, member level | 42.768 ± 0.995 | 41.757 ± 0.522 | 43.982 ± 0.301 |

Screening directory times were instead 290.605 / 264.337 / 270.472 ms, reversing
the original/proposal ranking. Original and path-only JAR implementations were
identical, yet confirmation timings differed by about 5%. The original private
fixture also failed to reproduce the advertised gain: one-fork original/proposal
results were 307.833 / 305.133 ms for directories and 97.508 / 97.264 ms for JARs,
with wide uncertainty. Neither equivalence nor a meaningful regression was proved.

Allocation was substantially more stable, in **MiB/op, not live heap**:

| Workload | Original parent | Full proposal | Path-only |
|---|---:|---:|---:|
| Public directory | 142.785 | 109.919 | 116.107 |
| Public JAR | 225.705 | 224.181 | 225.834 |

The retained directory change therefore saved **26.7 MiB / 18.7%** on that
fixture. Savings depend on class count/path structure; no matching build-speed or
whole-Gradle allocation percentage follows from this result.

#### Earlier exploratory evidence, kept separate

An already-present [older benchmark review](../benchmarks/classpath-snapshot-review.md)
and `benchmarks/build/performance-review/` artifacts were preserved, not treated
as fresh evidence. Their three-fork timing means were:

| Workload | Original | Rewrite | Path-only |
|---|---:|---:|---:|
| Public directory | 272.319 | 240.451 | 265.762 |
| Public JAR | 36.845 | 33.536 | 36.117 |
| Stdlib, class level | 43.358 | 41.350 | Not separately measured |
| Stdlib, member level | 43.891 | 41.460 | Not separately measured |

Those gains did not remain stable: a subsequent original/rewrite directory pair
was 251.386 / 251.489 ms, and stdlib was 40.761 / 40.497 ms. Later unchanged
workloads slowed several-fold, with load averages reaching 42.43 / 25.31 / 14.34.
These runs demonstrate interference, not a favorable speedup to select.
Directory allocation was 142.974 / 110.422 / 116.727 MiB; JAR allocation was
226.495 / 225.017 / 226.393 MiB, consistent with the allocation-only conclusion.

That earlier record also considered hash-based deduplication followed by sorting,
and sorting names followed by `distinct()`, without consistent real-JAR benefit.
Removing a known directory prefix instead of calling `relativeTo` reduced
allocation to about 90.6 MiB, but had no established additional runtime gain and
needed root/relative-path/symlink/Windows correctness coverage. It was not adopted.

### Follow-up: hypotheses, implementation, and acceptance decisions

#### 1. Bounded size-aware JAR reads — kept

Kotlin's `InputStream.readBytes()` uses a `ByteArrayOutputStream` backing array
of at least 8 KiB, an 8 KiB copying buffer, and the final byte array. For 10,000
small classes, the two temporary buffers alone imply roughly **156 MiB** of
allocation. That was initially a source-level calculation, not a measured saving.

The retained `readBytesWithExpectedSize` path uses the ZIP size to allocate the
anticipated result array only for expected sizes from **zero through 1 MiB**.
It handles partial reads, zero progress, early EOF, and excess data. Unknown or
larger expected sizes use bounded initial buffers, without trusting `available()`.
The bound limits **initial allocation**, not supported class size: larger valid
entries are still read fully. This is not a total decompression-memory limit.

A deliberately malformed deflated-ZIP test exposed a real defect in the first
prototype: its fallback still called `readBytes()`, whose use of an incorrect
available-size hint caused `OutOfMemoryError: Requested array size exceeds VM
limit`. The fallback was corrected to use explicitly bounded initial buffering;
the hostile-size test and complete targeted suite then passed.

The change retains `ZipFile`, name lookup, sorting, deduplication, and stream
closing. It does not trust ZIP metadata blindly or replace random-access reading.

**Why keep:** an isolated two-fork Java-JAR screen reduced allocation from
225.782 to 69.173 MiB/op and time from 36.213 to 31.071 ms/op. Stdlib allocation
fell by 17.871 MiB/op, although its isolated timing gain was not convincing.

#### 2. Decode Kotlin metadata once — kept

Previously, inline-member discovery decoded Kotlin metadata, and
`KotlinClassInfo.protoData` decoded it again when companion information, package
members, or type aliases were required. Even classes without inline functions
could incur the first parse to discover their absence.

The new reader-taking `KotlinClassInfo.createFrom` factory shares decoded
class/package protobuf data and its name resolver with inline-member discovery
and the snapshot's lazy `protoData`. Supporting overloads in
`ExtraClassInfoGenerator` and `inlineFunctionsAndAccessors` accept already
available inputs. Both snapshotter paths opt into reuse.

The old byte-array and explicit-extra-info factories retain their original
behavior, including laziness where applicable; ordinary incremental-cache callers
do not automatically opt in. Header/version fallback and serialization formats
remain unchanged. The decoded object lives with its `KotlinClassInfo`, so retained
memory required checking. Reuse assumes metadata arrays are not subsequently
mutated during normal processing; tests distinguish old lazy behavior from the
new retained-proto behavior using deliberate mutation.

**Why keep:** metadata-only screening reduced stdlib from 42.833 to 37.014 ms/op
and 204.034 to 155.876 MiB/op. Coroutines went from 25.482 to 23.663 ms/op and
73.243 to 59.156 MiB/op. No useful improvement appeared on the Java-only fixture,
as expected.

#### 3. Reuse the ASM class reader — kept in its full per-loaded-class form

`ClassFileWithContents` lazily owns one `ClassReader`. Basic-info extraction, Java
ABI snapshotting, Kotlin extra-info collection, and inline-local-class inspection
reuse it when processing that loaded instance. This avoids repeated constant-pool
setup and some string decoding.

Separate visitor passes remain: merging them could accidentally perform expensive
work for inaccessible classes that currently avoid it. No global reader cache or
eager all-class loading was introduced. Independently loaded instances still have
separate readers; this does not eliminate every reread.

A Java-only reuse prototype saved 4.617 MiB/op on the Java fixture but only about
0.245 MiB/op on stdlib. That alone was not a strong real-Kotlin-library argument.
Extending reuse to Kotlin and inline-local processing, **on top of JAR + metadata
reuse**, improved reflect from 32.395 to 29.032 ms/op and 95.582 to 85.831 MiB/op;
stdlib improved from 36.167 to 34.159 ms/op and 137.886 to 133.438 MiB/op.

**Why keep:** the full version removes repeated parsing setup on real dependencies
and contributes a material measured gain beyond JAR and metadata reuse.

#### 4. A bounded cache of class bytes — implemented, measured, removed

The prototype used an access-ordered `LinkedHashMap`, scoped to a single
inline-enabled snapshot, with a **1 MiB byte budget**. It cached eligible class
bytes and evicted least-recently-used entries; larger individual classes were not
retained. The aim was to avoid repeated reads/decompression for inline-local
classes without caching an entire archive.

On top of the three retained changes:

- Stdlib saved only about **0.04 MiB/op**, with no timing win.
- Reflect allocated about **0.12 MiB/op more**, with no timing win.
- Coroutines saved about **0.58 MiB/op** and roughly **3% time**, in only two
  screening forks.

**Why remove:** a narrow, weakly confirmed win did not justify retained bytes, map
bookkeeping, and eviction logic. The cache is absent from final production code.

#### 5. Incremental directory snapshots — deferred, not implemented or measured

Whole unchanged entries can already be reused through cacheable transforms, but a
changed directory is snapshotted as a whole; the operation does not receive prior
per-class state. Reusing unchanged classes could help a large module with only a
few changed files, but invalidation must include outer-class accessibility and
transitive inline-local-class dependencies, not merely filenames or timestamps.

This needs separate state/invalidation design and an incremental-build benchmark.
It is not represented as a completed optimization in the results below.

### Screening measurements: the full candidate comparison

Each row is the mean of **two fresh JVM fork means**; time is ms/op and allocation
is MiB/op. These runs selected candidates, not independently confirmed precise
individual effect sizes. Compare variants **within the same run family**, not
across different host-time windows.

| Run family / input | Variant | ms/op | MiB/op |
|---|---|---:|---:|
| `screen` / public Java JAR | `baseline` | 36.213 | 225.782 |
| | `jar` | 31.071 | 69.173 |
| | `metadata-prototype` | 35.878 | 225.901 |
| | `asm-java` | 34.969 | 221.165 |
| `screen` / stdlib | `baseline` | 42.833 | 204.034 |
| | `jar` | 42.283 | 186.163 |
| | `metadata-prototype` | 37.014 | 155.876 |
| | `asm-java` | 41.929 | 203.789 |
| `followup` / stdlib | `metadata-prototype+jar` | 36.167 | 137.886 |
| | `combined` | 34.159 | 133.438 |
| | `cache` | 34.356 | 133.396 |
| `coroutines-screen` / coroutines | `baseline` | 25.482 | 73.243 |
| | `jar` | 25.187 | 57.669 |
| | `metadata-prototype` | 23.663 | 59.156 |
| | `combined` | 22.152 | 41.245 |
| | `cache` | 21.427 | 40.670 |
| `reflect-screen` / reflect | `baseline` | 35.830 | 149.740 |
| | `metadata-prototype+jar` | 32.395 | 95.582 |
| | `combined` | 29.032 | 85.831 |
| | `cache` | 29.176 | 85.952 |

Variant interpretation:

- `baseline`: original one-file overlay representing the path-only baseline,
  with other original classes supplied by the benchmark JAR.
- `jar`, `metadata/prototype`, `asm-java`: isolated experimental overlays;
  filenames normalize the slash in `metadata/prototype` to a hyphen.
- `metadata/prototype+jar`: metadata and JAR overlays, without full ASM reuse.
- `combined`: all three retained ideas, before the hostile-size fallback fix.
- `cache`: `combined` plus the subsequently rejected byte cache.
- `baseline-all`: all nine baseline production sources recompiled for the final
  controlled comparison.
- `final`: all nine final sources, including the fallback fix and no byte cache.
- `reproduced`: baseline reconstructed from the saved inverse patch, checked
  byte-for-byte against `baseline-all`.

### Final confirmation: the strongest timing evidence

**Environment:** Apple M4 Max, macOS 26.6.2 (build 25G83), JBR/OpenJDK 21.0.8,
JMH 1.37, one thread, G1, fixed 1 GiB heap, `-prof gc`. Each fresh JVM fork used
five one-second warmups and five one-second measurements. Four forks per real
library/variant; three for synthetic workloads. Variant order reversed each round.
No builds or other experiments ran concurrently with measurement. Host load was
approximately 3–6; this was not a dedicated controlled machine.

The benchmark includes one complete in-memory snapshot operation, not fixture
creation or a whole build. Real-library workloads use inline-local-class parsing
and disable expanded type aliases. Synthetic public Java workloads disable both.
Filesystem caches are warm.

Values are **means of fork means**. Brackets show **ranges of fork means**, not
confidence intervals. Time reduction is `(baseline - final) / baseline`.

| Workload | Baseline ms/op [range] | Final ms/op [range] | Time reduction | Baseline → final MiB/op | Allocation reduction |
|---|---:|---:|---:|---:|---:|
| Stdlib, class level | 41.393 [41.053, 41.538] | 33.373 [33.258, 33.434] | 19.4% | 204.187 → 133.480 | 34.6% |
| Reflect, class level | 35.171 [34.995, 35.274] | 28.691 [28.618, 28.759] | 18.4% | 149.692 → 85.832 | 42.7% |
| Coroutines, class level | 24.899 [24.871, 24.925] | 21.973 [21.909, 22.140] | 11.8% | 73.259 → 41.254 | 43.7% |
| Stdlib, member level | 42.174 [41.727, 42.726] | 33.842 [33.684, 33.974] | 19.8% | 205.242 → 134.724 | 34.4% |
| 10,000 public Java classes, JAR | 35.260 [35.069, 35.390] | 29.792 [29.596, 29.923] | 15.5% | 225.779 → 64.477 | 71.4% |
| Same classes, directory | 259.255 [255.126, 263.720] | 255.124 [250.669, 257.749] | **Not established** | 115.540 → 111.122 | 3.8% |

The directory mean is numerically lower, but variation and I/O sensitivity are
large enough that it is not accepted as a meaningful speedup.

An unchanged-code control compared the one-file `baseline` with `baseline-all`,
compiled using the same compiler/flags as `final`. Reflect averaged **35.390 vs
35.171 ms/op**, a **0.6% difference**, well below the final gain. Final headline
comparisons use `baseline-all`, not the smaller overlay. All nine final production
source hashes matched the measured sources at the end of implementation.

This is stronger evidence than the original rewrite's unstable rankings: multiple
real libraries, repeated counterbalanced forks, an unchanged-code control, stable
allocation changes, and separate final confirmation. It still does not establish
universal percentages or isolate each optimization's contribution with four-fork
confirmation of every ablation.

### Memory results: fewer allocations are not lower peak usage

Three different quantities must not be confused:

- **Allocated bytes/op:** total objects/arrays created while doing the work,
  including garbage. This is where the strong reductions were measured.
- **Retained heap:** objects still alive after collection while snapshots remain
  reachable. It need not decrease in proportion to allocation.
- **Peak RSS:** the maximum resident memory of the entire JVM process, including
  native/JIT memory. It is not just snapshot objects or peak live Java heap.

A separate coarse helper warmed five snapshots, forced GC, retained ten snapshots,
and forced GC again. It reported the heap delta divided by ten; `/usr/bin/time -l`
measured process peak RSS. This was not a timing benchmark. Three fresh,
counterbalanced JVMs per variant used a 128 MiB initial / 512 MiB maximum heap.

Retained values below are means; RSS values are ranges, all in MiB:

| Input / granularity | Baseline retained/snapshot | Final retained/snapshot | Baseline peak RSS | Final peak RSS |
|---|---:|---:|---:|---:|
| Stdlib, class level | 0.782 | 0.782 | 377.6–383.8 | 344.3–387.9 |
| Stdlib, member level | 18.429 | 17.962 | 501.8–555.3 | 518.9–577.5 |
| Reflect, class level | 1.738 | 1.736 | 347.7–362.7 | 382.8–397.1 |
| Reflect, member level | 7.132 | 7.042 | 418.7–429.8 | 422.2–429.0 |

**No peak-RSS improvement was established.** Reflect class-level RSS increased in
these runs despite much lower allocation. A tighter fixed **128 MiB heap** was
also tested for reflect at class level, again with three forks:

| Variant | Retained MiB/snapshot | Peak RSS MiB [range] |
|---|---:|---:|
| `baseline-all` | 1.740 | 315.6–332.7 |
| `metadata-prototype+jar` | 1.737 | 327.2–337.6 |
| `final` | 1.733 | 325.7–331.7 |

All completed. No consistent retained-heap regression emerged, but forced-GC
heap deltas are coarse and whole-process RSS has other contributors. These checks
do **not** prove equal peak-memory behavior under real Gradle-daemon pressure or
pinpoint why RSS differed.

### Correctness, failures encountered, and final validation

The investigation ended with **66 tests, zero failures, zero errors, zero skips**
in the targeted classpath-diff suite. Coverage included:

- Ordering for directories/JARs, unsorted input, distinct public-class contents
  checked against independent snapshots, case-insensitive filtering/extensions,
  class-suffixed directories, and empty entries; both granularities and both
  inline-local parsing settings.
- Stream exact/short/long/unknown sizes, partial and zero-progress reads,
  misleading `available()`, read-failure propagation, stored ZIP entries, and
  deliberately incorrect deflated ZIP sizes. The hostile-size case initially
  failed with an oversized allocation and passed after the fallback fix.
- Nine metadata-reuse tests: classes/companions/constants, private-inline
  exclusion, package functions/getters/setters and Kotlin/JVM names, multifile
  parts/facades, incompatible-data fallback, malformed/missing metadata, old
  factory laziness, retained proto identity, and custom inline-hash generators.
  Assertions compare hashes, protos/resolved strings, serialized bytes, and
  deserialization. Multifile fixtures themselves contain no inline members;
  other fixtures cover inline behavior.

Serialized-snapshot SHA-256 output matched for **48 combinations**: stdlib,
reflect, coroutines, and three fixture directories, each using both granularities,
both inline-parsing settings, and both type-alias-expansion settings. The
reconstructed baseline also matched. This is finite regression coverage, not a
proof for every possible class file.

Production/unit-test inspections found no new actionable issues. The final IDE
build and Gradle benchmark-test compilation succeeded. The benchmark IDE dependency
model still displayed unresolved dependencies despite successful compilation and
actual JMH/helper execution; a Gradle reimport remained needed. No broad
compiler/KGP suite was run.

The final implementation validation command was:

```bash
./gradlew :compiler:incremental-compilation-impl:test --tests 'org.jetbrains.kotlin.incremental.classpathDiff.*' :benchmarks:compileTestKotlin -q
```

Earlier validation also assembled the JMH fat JAR and ran all ten smoke cases:
public/private directory/JAR inputs with both granularities at 100 classes, plus
stdlib at both granularities. Smoke success is correctness/execution evidence,
not performance evidence. The full dedicated benchmark matrix was not run;
selected direct JMH comparisons supplied the reported measurements.

### Source map

The final local optimizations are concentrated in these nine production files:

| File | Role |
|---|---|
| [`ClasspathEntrySnapshotter.kt`](../compiler/incremental-compilation-impl/src/org/jetbrains/kotlin/incremental/classpathDiff/ClasspathEntrySnapshotter.kt) | Directory path reuse and bounded size-aware JAR reads. |
| [`ClassFile.kt`](../compiler/incremental-compilation-impl/src/org/jetbrains/kotlin/incremental/classpathDiff/impl/ClassFile.kt) | Per-loaded-class lazy reader ownership. |
| [`BasicClassInfo.kt`](../compiler/incremental-compilation-impl/src/org/jetbrains/kotlin/incremental/classpathDiff/impl/BasicClassInfo.kt) | Basic-info extraction using the available reader. |
| [`SingleClassSnapshotter.kt`](../compiler/incremental-compilation-impl/src/org/jetbrains/kotlin/incremental/classpathDiff/impl/SingleClassSnapshotter.kt) | Java/Kotlin snapshot reader reuse. |
| [`ClassListSnapshotter.kt`](../compiler/incremental-compilation-impl/src/org/jetbrains/kotlin/incremental/classpathDiff/impl/ClassListSnapshotter.kt) | Integrates reuse in the class-list snapshotting paths. |
| [`InlinedClassSnapshotter.kt`](../compiler/incremental-compilation-impl/src/org/jetbrains/kotlin/incremental/classpathDiff/impl/InlinedClassSnapshotter.kt) | Inline-local-class inspection using the loaded reader. |
| [`KotlinClassInfo.kt`](../build-common/src/org/jetbrains/kotlin/incremental/KotlinClassInfo.kt) | Reader-taking factory and shared decoded metadata. |
| [`ExtraClassInfoGenerator.kt`](../build-common/src/org/jetbrains/kotlin/incremental/impl/ExtraClassInfoGenerator.kt) | Extra-info generation from reader/precomputed inline members. |
| [`inlineUtil.kt`](../compiler/frontend.java/src/org/jetbrains/kotlin/inline/inlineUtil.kt) | Inline-member discovery from decoded class/package metadata. |

Regression coverage is in `ClasspathSnapshotterTest.kt`,
`ClasspathEntryReadBytesTest.kt`, and `KotlinClassInfoReuseTest.kt` under
`compiler/incremental-compilation-impl/tests/org/jetbrains/kotlin/incremental/classpathDiff/`.
Benchmark/helper sources are `StdlibClasspathEntrySnapshotBenchmark.kt`,
`ClasspathSnapshotCompatibility.kt`, and `ClasspathSnapshotMemory.kt` under
`benchmarks/tests/org/jetbrains/kotlin/benchmarks/jmh/`.

### Evidence inventory and provenance

| Artifact | Purpose / limits |
|---|---|
| [Initial fresh review](classpath-entry-snapshot-performance-review.md) | First-stage decisions, fresh initial measurements, tests, and caveats. |
| [Follow-up experiment report](classpath-snapshot-followup-experiments.md) | Implementation-stage report and final measurements. |
| [Earlier exploratory review](../benchmarks/classpath-snapshot-review.md) | Older evidence, including severe host interference; not merged into fresh means. |
| [Exported measurements](../benchmarks/classpath-snapshot-experiments.csv) | **121 evidence rows:** 88 timing/allocation rows and 33 memory rows; includes per-fork scores and individual timing samples, including rejected candidates. |
| [Experiment script](../benchmarks/snapshot-experiments.py) | Capture/compile overlays, counterbalanced runs, compatibility/memory checks, summaries, and exports. |
| [Baseline reconstruction patch](../benchmarks/snapshot-experiments-baseline.patch) | Inverse patch from final sources to the measured path-only baseline. Apply via the helper to copies, not production. |
| `benchmarks/build/performance-review/` | Older local ignored sources, script, JSON/logs; `screen`, `confirm`, `paired1`–`paired3`. |
| `benchmarks/build/snapshot-review-20260909/` | Fresh initial review sources/script/JSON/logs; `screen-*`, `confirm-*`, `original-fixture-*`, and smoke output. |
| `benchmarks/build/snapshot-experiments-20260909/` | Follow-up captured sources/manifests, overlays, raw JSON/logs, compatibility output, and memory logs. |

Ignored build artifacts are local evidence, **not guaranteed to survive cleanup
or exist in another checkout**. The exported CSV preserves the follow-up numeric
samples; it does not embed every original log, source variant, or initial-review
raw result. Preserve the local directories separately if the full raw audit trail
is needed. Existing artifacts were not regenerated while preparing this report.

Measured artifact SHA-256 values:

| Artifact | SHA-256 |
|---|---|
| `kotlin-stdlib.jar` | `6fad04b73565748d3e724a2de8d2cd2351eefa70145ea22bdfdd7fa579a72955` |
| `kotlin-reflect.jar` | `49e163eb5a239bf87cb77ba40fbaa7c3a0d0a52850d765fd69e4d4cf77c2804e` |
| `kotlinx-coroutines-core-jvm.jar` | `9860906a1937490bf5f3b06d2f0e10ef451e65b95b269f22daf68a3d1f5065c5` |
| JMH fat JAR | `b37a0a04ad4043e321dfffb581bb2d3e1c226e66159757bd576c9032121ae812` |

### Reproducing the experiment

These are reproduction instructions, **not commands rerun for this documentation
request**. Run from the repository root. Prerequisites: macOS JDK discovery,
JDK 21, Python 3, the local `dist/kotlinc` compiler/library artifacts, and the JMH
fat JAR. The script hard-codes the benchmark JAR version/path and dated output
directory; another repository version or OS may require adapting those paths.

Use unused variant names/tags to avoid colliding with captured sources or
replacing existing result files. Source overlays are compiled with the same local
compiler, benchmark JAR, friend paths, and opt-in flags, then placed **before** the
fat JAR on each separate JVM's classpath.

```bash
./gradlew :benchmarks:testBenchmarkJar -q
python3 benchmarks/snapshot-experiments.py baseline-from-patch repeat-baseline
python3 benchmarks/snapshot-experiments.py compile repeat-baseline
python3 benchmarks/snapshot-experiments.py capture repeat-final
python3 benchmarks/snapshot-experiments.py compile repeat-final
python3 benchmarks/snapshot-experiments.py run repeat-baseline,repeat-final --tag repeat-stdlib --rounds 4 --warmups 5 --iterations 5 --cases stdlib
python3 benchmarks/snapshot-experiments.py run repeat-baseline,repeat-final --tag repeat-reflect --artifact dist/kotlinc/lib/kotlin-reflect.jar --rounds 4 --cases stdlib
python3 benchmarks/snapshot-experiments.py run repeat-baseline,repeat-final --tag repeat-coroutines --artifact dist/kotlinc/lib/kotlinx-coroutines-core-jvm.jar --rounds 4 --cases stdlib
python3 benchmarks/snapshot-experiments.py run repeat-baseline,repeat-final --tag repeat-member --rounds 4 --cases stdlib --granularity CLASS_MEMBER_LEVEL
python3 benchmarks/snapshot-experiments.py run repeat-baseline,repeat-final --tag repeat-synthetic --rounds 3 --cases jar,directory
```

`baseline-from-patch` applies the inverse patch **only to copied sources in the
experiment build directory**. Reconstruction was tested against the saved
baseline. Do not apply this inverse patch to the working production tree.
The historical benchmark/property names say `stdlib` even when `--artifact`
selects reflect or coroutines; the selected artifact, not that label, defines the
workload. Rebuilding artifacts can change their contents: compare hashes before
claiming exact reproduction of the recorded inputs.

Compile and run the correctness/memory helpers separately from timing:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) dist/kotlinc/bin/kotlinc \
  benchmarks/tests/org/jetbrains/kotlin/benchmarks/jmh/ClasspathSnapshotCompatibility.kt \
  benchmarks/tests/org/jetbrains/kotlin/benchmarks/jmh/ClasspathSnapshotMemory.kt \
  -classpath benchmarks/build/benchmarks/test/jars/benchmarks-test-jmh-2.5.255-SNAPSHOT-JMH.jar \
  -d benchmarks/build/snapshot-experiments-20260909/compatibility
python3 benchmarks/snapshot-experiments.py verify repeat-baseline,repeat-final
python3 benchmarks/snapshot-experiments.py memory repeat-baseline,repeat-final --tag repeat-stdlib-memory --rounds 3 --granularity CLASS_LEVEL,CLASS_MEMBER_LEVEL
python3 benchmarks/snapshot-experiments.py memory repeat-baseline,repeat-final --tag repeat-reflect-memory --artifact dist/kotlinc/lib/kotlin-reflect.jar --rounds 3 --granularity CLASS_LEVEL,CLASS_MEMBER_LEVEL
python3 benchmarks/snapshot-experiments.py memory repeat-baseline,repeat-final --tag repeat-reflect-small-heap --artifact dist/kotlinc/lib/kotlin-reflect.jar --rounds 3 --memory-heap 128m
python3 benchmarks/snapshot-experiments.py summarize repeat-baseline,repeat-final --tag repeat-stdlib
```

The initial review's full parameter-matrix task is
`./gradlew :benchmarks:testClasspathSnapshotBenchmark -q`; it covers multiple
class counts, visibility choices, formats, and granularities. It is not necessary
to run that entire matrix to reproduce a specific selected comparison above.

### Remaining limits and worthwhile next investigations

**Not measured:** end-to-end Gradle/KGP build time, cold-I/O behavior, Windows,
other supported JDKs, parallel daemon pressure, or an incremental per-class
snapshot implementation. Real-library timing confirmation covered three libraries
at class level, and stdlib at member level, not every setting on every dependency.

Prioritize further work as follows:

1. **Measure actual build impact.** Determine how much elapsed build time is spent
   snapshotting and how often whole-entry caching already avoids it. The measured
   operation saving applies only to that portion of work, not the whole build.
2. **Broaden environment and memory validation.** Repeat on quiet hosts, other
   supported JDKs/Windows, resource-heavy and differently ordered JARs, cold I/O,
   and constrained/concurrent Gradle daemons. Inspect peak live memory as well as
   allocation and process RSS.
3. **Profile phases before another rewrite.** Existing metrics include
   `LOAD_CLASSES_PATHS_ONLY`, `LOAD_CONTENTS_OF_CLASSES`, and `SNAPSHOT_CLASSES`.
   Find the next dominant cost rather than optimizing map bookkeeping by intuition.
4. **Treat incremental directory reuse as its own architectural experiment.**
   Define invalidation for outer/inline dependencies and benchmark small changes
   in large modules before accepting additional state and complexity.
5. **Revisit path shortcuts or byte caching only with new evidence.** Preserve
   filesystem semantics and streaming behavior; the rejected experiments do not
   currently justify adoption.

**Final decision:** the retained local optimizations are supported by the available
snapshot benchmarks and regression checks. Their measured benefits are real for
those workloads; general build-speed and peak-memory claims remain unproven.
