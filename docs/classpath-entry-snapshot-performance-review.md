### TL;DR

For the complete investigation and a plain-English summary, see the
[consolidated results](classpath-snapshot-investigation-results.md).

This is the **initial reader-rewrite review**. The later implemented experiments,
current keep/reject decisions, and new measurements are in
[the follow-up report](classpath-snapshot-followup-experiments.md).

Reviewed `239cc7fd77ef` against its parent, `9bc7d75a47bb`, on 2026-09-09.

- **Keep only computing each directory-relative path once.** Production code is now
  one six-line diff hunk from the original implementation.
- This saves **26.7 MiB / 18.7% allocated per snapshot** of 10,000 small public
  classes in a directory. This is allocation volume, not retained heap or a build
  speedup. Runtime improvement was **not reliably established**.
- Restore the original reader interface, JAR reader, and result-map construction.
  The advertised 13% directory / 33% JAR speedups are not supported by these runs.
- Keep stronger tests and benchmarks. Investigate stream-buffer allocation next,
  rather than retaining the reader rewrite on the strength of synthetic timings.

### Findings and decisions

| Change | Finding | Decision |
|---|---|---|
| Compute directory-relative paths once | The original computes the same path in both `filter` and `map` for accepted files. Eliminating the second computation consistently reduces allocation. | Keep, without changing the reader abstraction. |
| Construct providers during traversal | Retaining `File` objects also avoids resolving names again when reading. The full rewrite saves about another 6 MiB on the directory fixture, but retains more traversal objects and has no reliable additional runtime win. | Revert in favor of the much smaller path-only change. |
| Retain `ZipEntry` objects | Does **not** eliminate one name lookup per class on OpenJDK 21. It avoids reconstructing entries, but retains their metadata throughout snapshotting. | Revert. |
| Sort and deduplicate ZIP entries | Both implementations are `O(n log n)` in the general case. Sorting can benefit from archive ordering, but the test archive's reverse-numbered names are not representative of every dependency. Preallocating from `ZipFile.size()` also reserves space for excluded resources and directories. | Revert; no robust real-JAR gain established for this bundled rewrite. |
| Pre-sized result map and indexed insertion | Removes temporary path/pair lists and map resizes, but an isolated screening run saved only about 0.36 MiB (0.16%) on the public JAR fixture, with no clear runtime gain. | Revert the loop and custom capacity helper. This is a real local allocation reduction, not a demonstrated material whole-operation improvement. |

No snapshot correctness regression was established in the proposal. Reversion is
based on insufficient benefit for the added complexity, not a claim that every
removed optimization does literally nothing.

### How snapshotting works

`ClasspathEntrySnapshotter` enumerates directory/JAR entries, excludes directories,
resources, root `module-info.class`, and `META-INF` classes (case-insensitively),
then orders the accepted paths deterministically. The original JAR reader also
deduplicates names with a sorted set.

Each path gets a lazy contents provider. `PlainClassListSnapshotter`, or
`ClassListSnapshotterWithInlinedClassSupport`, loads bytes and extracts class
information. Accessible Java/Kotlin classes get ABI snapshots; inaccessible
classes can avoid that work. Inline-local-class support can require nonsequential
access and rereading. Results are assembled into an insertion-ordered map matching
the path order. The retained change does not alter any of these behaviors, nor
eagerly load class bytes.

On [OpenJDK 21](https://github.com/openjdk/jdk21u/blob/master/src/java.base/share/classes/java/util/zip/ZipFile.java),
`getInputStream(entry)` uses `lastEntryPos` only when `entry.name` matches
`lastEntryName`; otherwise it calls `getEntryPos(entry.name, false)`.
The original `getEntry(name)` populates that cache immediately before opening the
stream. The rewrite enumerates and sorts first, so retained entries generally
still trigger a name lookup at read time. They are not direct central-directory
position handles. This analysis is JDK-specific, not a guarantee of the ZIP API.
Restoring the original reader also restores its duplicate-entry behavior without
introducing a new assumption about which duplicate a particular JDK selects.

### Fresh measurements

JMH 1.37, JBR/OpenJDK 21.0.8, one thread, G1, fixed 1 GiB heap, `-prof gc`.
Setup and fixture creation are outside the measured region. These are warm-cache
snapshot operations, not cold-I/O or incremental-build benchmarks.

The original fixture copies the same private empty Kotlin class under different
entry names. It still incurs parsing, but skips normal ABI hashing and does not
model distinct public classes. It remains available as `publicClasses=false` for
overhead comparisons, not as the sole evidence of a speedup.

The new public fixture uses distinct Java classes with matching entry/internal
names, a field and a method. Real-JAR measurements use the built Kotlin stdlib,
both granularities, and inline-local-class parsing enabled. Synthetic runs below
use class-level granularity and disable inline-local parsing.

The first screen used one fork and 3 x 1 s warmup/measurement iterations, including
an isolated result-map variant. Confirmation used three forks and 5 x 1 s
warmup/measurement iterations, reversing implementation order. Each source variant
was compiled with the same compiler against the same freshly assembled benchmark
JAR and prepended to the classpath in separate JMH JVMs. The final production file
was checked byte-for-byte against the measured path-only source.

Confirmation time, **ms/operation, mean ± JMH's reported 99.9% error**:

| Workload | Original | Full proposal | Retained path-only |
|---|---:|---:|---:|
| 10,000 public classes, directory | 260.311 ± 2.633 | 274.078 ± 12.135 | 280.052 ± 25.957 |
| 10,000 public classes, JAR | 35.756 ± 0.692 | 34.725 ± 0.614 | 37.635 ± 0.337 |
| Stdlib, class level | 42.276 ± 0.338 | 41.918 ± 0.720 | 43.820 ± 0.233 |
| Stdlib, member level | 42.768 ± 0.995 | 41.757 ± 0.522 | 43.982 ± 0.301 |

These are **not stable causal speedup estimates**. The screening directory times
were 290.605 / 264.337 / 270.472 ms, reversing the original/proposal ranking.
Original and path-only variants have identical JAR code, yet their confirmation
JAR times differ by about 5%. Process confidence intervals do not remove such
between-run drift. Load averages also varied during the experiment. Neither the
apparent directory regression nor the smaller JAR gains justify a runtime claim.

A fresh screen of the original 10,000-private-class fixture (one fork, three
one-second warmup/measurement iterations) likewise measured original/proposal
directory times of 307.833 / 305.133 ms and JAR times of 97.508 / 97.264 ms, with
wide uncertainty. This does not prove equivalence, but does not reproduce the
commit's advertised gains even on its original kind of input.

Confirmation allocation, **MiB/operation (not live heap)**:

| Workload | Original | Full proposal | Retained path-only |
|---|---:|---:|---:|
| Public directory | 142.785 | 109.919 | 116.107 |
| Public JAR | 225.705 | 224.181 | 225.834 |

The directory allocation saving persisted across screening and confirmation.
It will depend on class count and path structure; it is not an 18.7% improvement
to total Gradle allocation or elapsed build time. JAR allocation is effectively
unchanged by the retained code, as expected.

Fresh sources, comparison script, JSON results, and logs are local ignored
artifacts in `benchmarks/build/snapshot-review-20260909/` (`screen-*`, `confirm-*`).
`original-fixture-*` contains the private-class screen; `smoke.*` contains the
final benchmark smoke test, not performance evidence.
The pre-existing untracked `benchmarks/classpath-snapshot-review.md` and its older
artifacts were preserved; they are not the source of the numbers above.

### Validation and reproduction

```bash
./gradlew :compiler:incremental-compilation-impl:test --tests 'org.jetbrains.kotlin.incremental.classpathDiff.*' -q
./gradlew :benchmarks:testBenchmarkJar -q
./gradlew :benchmarks:testClasspathSnapshotBenchmark -q
```

The first two commands were run successfully. The expanded entry test covers four
granularity/inline-parsing combinations, distinct contents checked against
independent class snapshots, unsorted JAR input, ordering for both formats,
case-insensitive exclusions/extensions, and class-suffixed directories. A fifth
invocation covers empty directory/JAR inputs. No test-data generation is required.
All ten final JMH smoke cases also passed: public/private directory/JAR inputs
with both granularities at 100 classes, and stdlib with both granularities.

The dedicated benchmark task above runs the full parameter matrix, with three
forks and five one-second warmup/measurement iterations. For this review, selected
JMH comparisons were run directly instead of executing that entire matrix. The
task passes the actual stdlib artifact via `classpathSnapshot.stdlib`; it does not
accidentally benchmark the dependency-bundling JMH JAR as the stdlib input.

IDE builds succeeded. Inspections were clean for production code; the benchmark
IDE model still reported unresolved dependencies despite successful Gradle
compilation and actual JMH execution. A Gradle reimport is needed to reconcile
that IDE state. No full compiler/KGP suite or end-to-end build benchmark was run.

### Further improvements to investigate, not implemented

1. **Reduce temporary buffers while reading JAR classes.** In
   `libraries/stdlib/jvm/src/kotlin/io/IOStreams.kt`, `InputStream.readBytes()`
   allocates a `ByteArrayOutputStream`, `copyTo`'s buffer, and the final byte array.
   For small classes this can dwarf entry/map bookkeeping. Profile allocations
   first; evaluate a bounded size-aware reader or reusable copy buffer. Do not
   blindly trust ZIP sizes, retain all class bytes, or weaken truncated/oversized
   input handling. Keep random access for inline/outer-class processing.
2. **Measure phases and representative workloads.** Use the existing
   `LOAD_CLASSES_PATHS_ONLY`, `LOAD_CONTENTS_OF_CLASSES`, and `SNAPSHOT_CLASSES`
   metrics. Repeat counterbalanced comparisons on a quiet host with multiple real
   dependencies, resource-heavy and differently ordered archives, supported JDKs,
   and Windows paths; then measure actual build impact and peak retained memory.
3. **Revisit directory path processing only with evidence.** Avoiding `resolve`
   or repeated path normalization may save more allocation. Prefer a local change
   over restructuring both readers, and cover relative roots, filesystem roots,
   symlinks, and Windows separators before replacing standard path semantics.
