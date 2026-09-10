### TL;DR

For the complete investigation, including the original rewrite review, see the
[consolidated results and plain-English summary](classpath-snapshot-investigation-results.md).

**Keep three local optimizations:** bounded size-aware JAR reads, decoding Kotlin
metadata once, and reusing the ASM reader across a loaded class's visitor passes.
Against the [previously retained path-only implementation](classpath-entry-snapshot-performance-review.md),
four counterbalanced forks show **12–19% lower snapshot time** and **35–44% less
allocation** on three real libraries. These are **warm-cache snapshot-operation
measurements, not faster-build claims**. No reliable directory speedup is claimed.

The bounded inline-class byte-cache prototype was **removed**. Incremental
directory snapshots were **not implemented**: they require a separate state and
invalidation design, not another safe local optimization.

### What survived, and why

| Candidate | Independent evidence | Decision |
|---|---|---|
| Size-aware JAR reads | Two-fork screen: 10,000 public Java classes, 225.78 → 69.17 MiB/op and 36.21 → 31.07 ms/op. Stdlib loses 17.87 MiB/op but has no convincing isolated timing win. | Keep: large, predictable allocation saving, and synthetic runtime benefit. |
| Decode metadata once | Two-fork screen: stdlib 204.03 → 155.88 MiB/op, 42.83 → 37.01 ms/op; coroutines 73.24 → 59.16 MiB/op, about 25.48 → 23.66 ms/op. No useful change on the Java-only fixture, as expected. | Keep: removes actual repeated protobuf parsing on Kotlin dependencies. |
| Reuse ASM reader | Java-only prototype saved 4.62 MiB/op on the Java fixture but almost nothing on stdlib. Extending reuse to Kotlin extra-info and inline-local-class inspection, **on top of JAR + metadata reuse**, reduced reflect 95.58 → 85.83 MiB/op and 32.40 → 29.03 ms/op; stdlib 137.89 → 133.44 MiB/op and 36.17 → 34.16 ms/op (two forks each). | Keep the full per-loaded-class reuse, retaining separate visitor passes. |
| 1 MiB LRU of class bytes, scoped to one inline-enabled snapshot | On top of the three changes: stdlib saves only ~0.04 MiB/op; reflect allocates ~0.12 MiB/op more with no speedup. Coroutines saves ~0.58 MiB/op and ~3%, in just two screening forks. | Remove: too little demonstrated benefit for retained bytes, map bookkeeping, and eviction logic. |
| Incremental directory snapshots | Existing cacheable transforms reuse whole unchanged entries; the snapshotting operation does not receive prior per-class state. Correct reuse must invalidate changes to outer-class accessibility and transitive inline-local dependencies. | Deferred, not measured or represented as implemented. Requires a separate architectural experiment and incremental-build benchmark. |

Screening data selects candidates; it is not independent confirmation of their
precise individual percentages. The final combined implementation was rebuilt
and measured separately below. The rejected cache is absent from production.

### How the retained changes work

- `ClasspathEntrySnapshotter.kt`: for sizes from zero through 1 MiB, read into the
  anticipated result array, handling partial reads, early EOF, and excess data.
  Unknown or larger sizes use bounded initial buffers, not `available()` hints.
  A malformed-ZIP test caught an initial fallback that still called Kotlin's
  `readBytes()`: it attempted an enormous allocation from an incorrect ZIP size.
  That fallback was fixed, and the test now passes. The bound limits **initial
  allocation**, not total class size; large valid entries are still read fully.
  The existing `ZipFile`, lookup, sorting, duplicate handling, and closing remain.
- `KotlinClassInfo`'s new reader-taking factory shares decoded class/package
  metadata with inline-member discovery and the snapshot's lazy `protoData`.
  The old byte-array and explicit-extra-info factories retain their original
  behavior; ordinary incremental-cache callers do not opt into this change.
  Header-version fallback and existing serialized formats are preserved.
- `ClassFileWithContents` lazily owns one `ClassReader`. Basic info, Java ABI,
  Kotlin extra info, and inline-local-class inspection reuse it when operating
  on that loaded instance. This avoids repeated constant-pool setup and some
  string decoding. It does **not** load every class at once, merge visitors, or
  cache readers across independently loaded instances. Inaccessible classes still
  skip expensive ABI extraction.

### Confirmation results

2026-09-09, Apple M4 Max, macOS 26.6.2, JBR/OpenJDK 21.0.8, JMH 1.37,
one thread, G1, fixed 1 GiB heap, `-prof gc`. Five one-second warmup and five
one-second measurement iterations **per fresh JVM fork**. Four forks per real
library/variant; three for synthetic workloads. Variant order reverses each round.
No builds or other experiments were run concurrently with measurement.
Host load averages were approximately 3–6, not a controlled dedicated machine.

Numbers are means of fork means. Brackets give the **range of fork means**, not
confidence intervals. Real-library cases enable inline-local-class parsing and
disable expanded type aliases. Synthetic public Java cases disable both.

| Workload | Baseline ms/op [range] | Final ms/op [range] | Time reduction | Baseline → final MiB allocated/op |
|---|---:|---:|---:|---:|
| Stdlib, class level | 41.393 [41.053, 41.538] | 33.373 [33.258, 33.434] | 19.4% | 204.187 → 133.480 (34.6%) |
| Reflect, class level | 35.171 [34.995, 35.274] | 28.691 [28.618, 28.759] | 18.4% | 149.692 → 85.832 (42.7%) |
| Coroutines, class level | 24.899 [24.871, 24.925] | 21.973 [21.909, 22.140] | 11.8% | 73.259 → 41.254 (43.7%) |
| Stdlib, member level | 42.174 [41.727, 42.726] | 33.842 [33.684, 33.974] | 19.8% | 205.242 → 134.724 (34.4%) |
| 10,000 public Java classes, JAR | 35.260 [35.069, 35.390] | 29.792 [29.596, 29.923] | 15.5% | 225.779 → 64.477 (71.4%) |
| Same classes, directory | 259.255 [255.126, 263.720] | 255.124 [250.669, 257.749] | **Not established** | 115.540 → 111.122 (3.8%) |

An unchanged-code control compared the original one-file baseline overlay with
all nine baseline sources recompiled using the same compiler/flags as the final
overlay: reflect averaged 35.390 versus 35.171 ms/op (**0.6% difference**), well
below the retained gain. Final confirmation uses the all-source baseline.
The directory difference is too small relative to variation and I/O sensitivity
to count as a meaningful speedup. Allocation reductions are not additive to the
earlier path-only review's percentages: this experiment already includes that change.

### Memory: allocation is not peak usage

A separate coarse check warmed five snapshots, forced GC, retained ten snapshots,
then forced GC again. Three fresh counterbalanced JVMs per variant, both
granularities, 128 MiB initial / 512 MiB maximum heap:

- Stdlib retained heap per snapshot: about 0.78 MiB at class level for both;
  about 18.43 → 17.96 MiB at member level.
- Reflect: about 1.74 MiB at class level for both; 7.13 → 7.04 MiB at member level.
- **No peak-RSS improvement established.** OS peak measurements were mixed:
  reflect class-level RSS increased from roughly 348–363 to 383–397 MiB in these
  runs. This is whole-process RSS, including JVM native/JIT memory, not retained
  snapshot bytes. A tighter fixed 128 MiB heap completed successfully for all
  reflect class-level variants: baseline roughly 316–333 MiB RSS, final 326–332
  MiB, and JAR + metadata without full ASM reuse 327–338 MiB. No consistent
  retained-heap regression emerged; these coarse checks do not prove peak-memory
  equivalence under real Gradle-daemon pressure.

### Correctness and validation

- All **66 classpath-diff tests pass**, with no failures, errors, or skips,
  including new stream and metadata-reuse tests.
  Stream tests include exact/short/long/unknown sizes, partial/zero-progress reads,
  misleading `available()`, read failures, stored ZIP entries, and deliberately
  incorrect deflated ZIP headers. Metadata tests cover inline functions/accessors,
  companions/constants, multifile classes, version fallback, malformed/missing
  data, old-factory laziness, custom hash generators, and serialized equivalence.
- Baseline and final produce identical serialized-snapshot SHA-256 output for
  **48 combinations**: stdlib, reflect, coroutines, and three fixture directories,
  each with both granularities, both inline-parsing settings, and both
  type-alias-expansion settings. The independently reconstructed baseline also
  matches. This is finite regression coverage, not a proof for all class files.
- Production and unit-test inspections report no new actionable issues.
  The pre-existing benchmark IDE dependency model remains stale; standalone
  compilation and actual benchmark/helper execution succeed. The final IDE build
  and Gradle benchmark-test compilation also succeed. All nine final production
  sources were checked against the measured source hashes.

Final validation command:

```bash
./gradlew :compiler:incremental-compilation-impl:test --tests 'org.jetbrains.kotlin.incremental.classpathDiff.*' :benchmarks:compileTestKotlin -q
```

### Evidence and reproduction

Per-fork scores, individual timing samples, allocation, and memory readings are
preserved in [`classpath-snapshot-experiments.csv`](../benchmarks/classpath-snapshot-experiments.csv).
Raw JMH JSON/logs and captured baseline/candidate sources remain in the ignored
`benchmarks/build/snapshot-experiments-20260909/` directory. Screening names are
`screen`, `followup`, `coroutines-screen`, and `reflect-screen`; final evidence uses
`*-confirm`. `combined` predates the hostile-size fallback fix; `final` includes it.

The script requires macOS's JDK discovery, JDK 21, Python 3, `dist/kotlinc`, and the
JMH fat JAR. In a checkout of the final sources, use fresh variant names:

```bash
./gradlew :benchmarks:testBenchmarkJar -q
python3 benchmarks/snapshot-experiments.py baseline-from-patch repeat-baseline
python3 benchmarks/snapshot-experiments.py compile repeat-baseline
python3 benchmarks/snapshot-experiments.py capture repeat-final
python3 benchmarks/snapshot-experiments.py compile repeat-final
python3 benchmarks/snapshot-experiments.py run repeat-baseline,repeat-final --tag repeat --rounds 4 --cases stdlib
```

`baseline-from-patch` applies the saved inverse patch **only to copied sources in
the experiment build directory**, not to production. Reconstruction was tested.
Use `--artifact dist/kotlinc/lib/kotlin-reflect.jar` or
`--artifact dist/kotlinc/lib/kotlinx-coroutines-core-jvm.jar` for the other real
libraries; the historical benchmark/property names still say `stdlib`.
Use `--granularity CLASS_MEMBER_LEVEL` for member-level measurement.
The compatibility/memory helpers can be compiled against that same fat JAR into
`benchmarks/build/snapshot-experiments-20260909/compatibility`; then use the script's
`verify` or `memory` actions. Memory uses `--granularity CLASS_LEVEL,CLASS_MEMBER_LEVEL`
to test both settings, and is separate from timing measurement.

Measured artifact SHA-256 values:

| Artifact | SHA-256 |
|---|---|
| `kotlin-stdlib.jar` | `6fad04b73565748d3e724a2de8d2cd2351eefa70145ea22bdfdd7fa579a72955` |
| `kotlin-reflect.jar` | `49e163eb5a239bf87cb77ba40fbaa7c3a0d0a52850d765fd69e4d4cf77c2804e` |
| `kotlinx-coroutines-core-jvm.jar` | `9860906a1937490bf5f3b06d2f0e10ef451e65b95b269f22daf68a3d1f5065c5` |
| JMH fat JAR | `b37a0a04ad4043e321dfffb581bb2d3e1c226e66159757bd576c9032121ae812` |

**Limits:** no cold-I/O, Windows, other-JDK, end-to-end Gradle/KGP build, or parallel
daemon-pressure benchmark. The result supports keeping these local optimizations,
not a universal percentage or a claim that unchanged dependencies rebuild faster.
