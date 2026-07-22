# Java-Direct: Phase C Measurement Results

**Date**: 2026-04-21 (original §1–§4) / **updated 2026-04-22 (§5 adds IntelliJ-pipeline + file-utilization + size-bucket data)**
**Plan**: `REFACTORING_PLAN.md` §2.3 — 8 hypotheses (M-O3, M-O6, M-O4b, M-O2, M-P8, M-P9, M-P12, M-P13)
**Goal**: Decide which Phase D performance changes are justified by data before writing code.

> **Read first:** §5 extends the instrumentation and reruns on a realistic IntelliJ subset.
> It reverses the verdict on M-O6 (eager full-parse path should be dropped, not kept) and
> adds new findings about file utilization (0.18 % access rate) that the original §2 data
> did not surface.

---

## 1. Measurement setup

### 1.1 Instrumentation

A temporary `PhaseCMeasurementCounters` singleton was added under
`compiler/java-direct/src/org/jetbrains/kotlin/java/direct/`. It holds one `AtomicLong`
per quantity of interest plus a `ConcurrentHashMap.newKeySet<String>()` for the
per-class distinct set. A JVM shutdown hook writes a plain-text dump to
`compiler/java-direct/phase-c-dumps/dump-<pid>.txt` and mirrors it to `stderr`.
Per-path CPU time uses `ThreadMXBean.getCurrentThreadCpuTime()`, which is per-thread
and aggregates correctly across the test concurrency model.

Instrumented sites:

| Hypothesis | File | Site |
|---|---|---|
| M-O3  | `JavaImportResolver.kt`          | `extractImports` — hits / misses on `importCache[tree]` |
| M-O6  | `JavaClassFinderOverAstImpl.kt`  | `tryBuildFileEntryWithFullParse` / `tryBuildFileEntryLightweight` — invocation count, file bytes, CPU ns per path |
| M-O4b | `JavaClassFinderOverAstImpl.kt`  | `findClass` — positive-cache / negative-cache hits, negative-cache adds, `findClasses` invocations |
| M-O2  | `JavaResolutionContext.kt`       | `tryStarImports` — call count, `sum(starImports.size)` vs `sum(distinctStarImports.size)`, duplicate-detection counter |
| M-P8  | `JavaClassOverAst.kt`            | `deriveImplicitPermittedTypes` — invocation count, distinct-class `Set<String>` size |
| M-P9  | `JavaMemberOverAst.kt`, `ConstantEvaluator.kt` | `isSimpleNamePotentiallyConstant` / `resolveFieldValue` — call count + per-call fields-iterated counter |
| M-P12 | `JavaTypeOverAst.kt`             | `rawTypeNameParts` compute path — distinct types + single-segment share |
| M-P13 | `JavaClassFinderOverAstImpl.kt`  | `findPackageDirectories` — total calls vs `computeIfAbsent` lambda runs |

All eight are measured in a single instrumented jar; they do not interact.

### 1.2 Gating java-direct from a pipeline test

`KotlinFullPipelineTestsGenerated` propagates `args.javaDirect` from each model's
original compiler arguments (`AbstractIsolatedFullPipelineModularizedTest.kt:118`),
which is `false` by default. A one-line override was added just after that assignment:

```kotlin
if (System.getProperty("fir.force.javaDirect") == "true") args.javaDirect = true
```

The modularized-tests Gradle task propagates `fir.*` gradle properties as system
properties, so `-Pfir.force.javaDirect=true` is sufficient.

### 1.3 Corpora

- **Corpus A — java-direct's own suite** (`JavaUsingAstPhasedTestGenerated` +
  `JavaUsingAstBoxTestGenerated`, ~2500 tests). Provides high-traffic signal because
  every test instantiates a fresh `JavaClassFinderOverAstImpl` over a small Java file.
- **Corpus B — Kotlin full pipeline** (`KotlinFullPipelineTestsGenerated`,
  414 modules with `-Pfir.force.javaDirect=true`). Provides a realistic
  distribution: most Kotlin modules have no Java source roots, so java-direct fires
  on only a minority of modules. Run at default CONCURRENT execution mode — per-thread
  CPU time aggregates correctly.

Instrumentation overhead is dominated by `AtomicLong.incrementAndGet` (~3 ns each);
the M-O6 `ThreadMXBean` timestamps add ~200–500 ns per `tryBuildFileEntry*` call,
well under 1 % of those methods' total cost.

Dumps are archived at `compiler/java-direct/phase-c-dumps/`:
- `_dump-javadirect-own.txt` — corpus A totals
- `_dump-kotlin-pipeline.txt` — corpus B totals

---

## 2. Results per hypothesis

### M-O3 — `JavaImportResolver.importCache`

**Hypothesis**: hit rate is near-zero; the cache is pure overhead.

| Corpus | hits | misses | hit-rate |
|---|---:|---:|---:|
| A (java-direct own) | 0 | 3 724 | **0.00 %** |
| B (Kotlin pipeline) | 0 | 2 | 0.00 % |

Every call to `extractImports` misses. Each unique `JavaLightTree` instance is passed
in exactly once — `JavaResolutionContext.create` is called at most once per tree,
which matches the architectural expectation that a resolution context is built at
tree-load time and reused thereafter. The weak-keyed `WeakHashMap` never returns an
entry.

**Verdict**: ✅ **hypothesis confirmed — delete `importCache` in Phase D.**
Cost saved: one `Collections.synchronizedMap(WeakHashMap())` allocation at class
load, plus the `synchronizedMap.get` + `synchronizedMap.put` round-trip on every
`extractImports` call (3 724 wasted synchronizations per java-direct own-test run).

---

### M-O6 — `SMALL_FILE_SIZE_THRESHOLD = 4096`

**Hypothesis**: the threshold is material; compare traffic / CPU per path.

| Corpus | path | invocations | bytes | CPU ns | ns/byte |
|---|---|---:|---:|---:|---:|
| A | lightweight   | 1    | 52 428  | 11.4 M    | 218 |
| A | full parse    | 3 760 | 789 756 | 1 259.7 M | 1 595 |
| B | lightweight   | 1    | 4 349   | 2.2 M     | 513 |
| B | full parse    | 2    | 5 528   | 132.5 M   | 23 962 |

Observations:

1. **Almost all traffic (99.97 % in A, 67 % in B) goes through the full-parse path.**
   Test files and real Java sources in the Kotlin project are overwhelmingly ≤ 4 096 B.
2. **Per-byte cost of the full parser is 7–46× the lightweight scanner.** The
   separation is real: on the rare large file, the lightweight scanner saves
   proportional CPU.
3. **But the absolute savings are tiny.** In corpus A the lightweight path saved
   ~175 k × (1595 − 218) ≈ 240 µs total over the whole ~180 s test run. In corpus B
   it saved at most a handful of milliseconds.

**Verdict**: ⚠️ **hypothesis weakly confirmed.** The two paths have measurably
different per-byte costs, but the lightweight path is exercised so rarely (≤ 1 % of
invocations on every corpus) that deleting the lightweight path to simplify the
code — or raising the threshold to 16 KB / ∞ to reduce churn — would change the
full-pipeline CPU by under 0.01 %. **Recommendation: keep the current threshold,
delete the `SMALL_FILE_SIZE_THRESHOLD` comment's "unvalidated" note; close M-O6
as a null result.**

A fuller threshold sweep (1 K / 4 K / 16 K / ∞) would need input with more large
Java files to produce distinguishable signal. Neither corpus currently provides it.

---

### M-O4b — `negativeClassCache`

**Hypothesis**: the cache prevents fewer than 100 re-parses per run.

| Corpus | findClass calls | positive-cache hits | negative-cache hits | negative-cache adds |
|---|---:|---:|---:|---:|
| A | 172  | 79   | **0** | 91 |
| B | 0    | 0    | 0     | 0  |

On every findClass call where the result was `null`, the `ClassId` is added to
`negativeClassCache`. Across 2 500 java-direct own tests, the cache was populated
91 times but **never hit once** — zero re-parses prevented. On the Kotlin pipeline
subset sampled, `findClass` was not called through java-direct at all for this
run's model distribution.

The asymmetric behaviour has a simple explanation: the classes that are not in the
source tree (inner classes probed during FIR overload resolution, Kotlin-only
imports, synthetic symbols) are each looked up at most once per compilation; the
second probe that would produce a negative-cache hit doesn't happen in these
workloads.

**Verdict**: ✅ **hypothesis confirmed — delete `negativeClassCache` in Phase D.**
Additionally, the `Collections.newSetFromMap(ConcurrentHashMap())` idiom at
`JavaClassFinderOverAstImpl.kt:93` was already flagged by O4; since the field goes
away, O4 is absorbed.

---

### M-O2 — `distinctStarImports`

**Hypothesis**: the pre-computed dedup field saves no work.

| Corpus | `tryStarImports` calls | `sum(starImports.size)` | `sum(distinct.size)` | calls-with-duplicates |
|---|---:|---:|---:|---:|
| A | 24    | 0   | 0   | **0** |
| B | 4 949 | 323 | 323 | **0** |

Across every observed resolution context, `starImports.size == distinctStarImports.size`.
No compilation unit in either corpus has a duplicate star import (`import java.util.*`
and `import java.util.*` both present), which is expected: per JLS 7.5.2 duplicates
are legal but idiomatically absent; javac and IntelliJ both emit warnings.

**Verdict**: ✅ **hypothesis confirmed — inline `starImports.distinct()` at the
single use site in `tryStarImports` (or delete the dedupe call entirely and live
with the negligible duplicate cost) and drop the field from the constructor in
Phase D.** This simplifies `JavaResolutionContext`'s 10-parameter constructor
(addressing R9 partially along the way) and shrinks each `withTypeParameters` /
`withInheritedTypeParameters` / `withContainingClass` copy by one reference.

---

### M-P8 — `deriveImplicitPermittedTypes`

**Hypothesis**: the method is called often enough per sealed class for the missing
cache to matter.

| Corpus | invocations | distinct classes | avg/class |
|---|---:|---:|---:|
| A | 0 | 0 | — |
| B | 0 | 0 | — |

**Zero invocations in either corpus.** The Kotlin project model and the java-direct
own test suite contain no Java sealed classes without an explicit `permits` clause.
The only code path the hypothesis addresses (the `!isSealed → return emptySequence();
else derive from direct subtypes`) is never exercised.

**Verdict**: ❌ **hypothesis rejected (as null result) — do not add a cache in
Phase D.** If Java sealed classes become common in the Kotlin codebase in the
future, revisit with a fresh measurement, but today no optimisation is justified.
`permittedTypes` already short-circuits the explicit-permits case; the implicit
path is cold code.

---

### M-P9 — `fields.find` scans

**Hypothesis**: `isSimpleNamePotentiallyConstant` and `resolveFieldValue` linear
scans show > 1 % of time, justifying a field-name index.

| Corpus | isSimpleName calls | fields iterated | resolveFieldValue calls | fields iterated |
|---|---:|---:|---:|---:|
| A | 0 | 0 | 0 | 0 |
| B | 0 | 0 | 0 | 0 |

**Zero invocations.** Neither corpus exercises the `isInitializerPotentiallyConstant`
→ `isSimpleNamePotentiallyConstant` chain (which requires an unqualified simple-name
reference appearing as a field initializer), nor the `ConstantEvaluator.resolveFieldValue`
path (which requires a qualified field access inside an annotation argument or
final-field initializer). Both are narrow surfaces.

**Verdict**: ❌ **hypothesis rejected (as null result) — do not build a field-name
index in Phase D.** The scans are both O(N) in a class's field count and would
matter for a class with hundreds of fields, but the JLS-compile-time-constant
evaluation path is cold in practice. Close P9 as non-issue.

---

### M-P12 — `rawTypeNameParts` single-segment share

**Hypothesis**: a majority of type names are single-segment; storing `parts` and
deriving the joined name on demand would save allocation.

| Corpus | distinct types computed | single-segment | multi-segment | single-segment share |
|---|---:|---:|---:|---:|
| A | 4   | 4   | 0  | 100.00 % |
| B | 392 | 380 | 12 | **96.94 %** |

The Kotlin pipeline corpus shows the hypothesis is strongly correct: 97 % of Java
type references in user code are single-segment (e.g. `String`, `List<T>`, local
types). In the multi-segment minority (12/392), the split produces a 2- to 4-
element list — comparable allocation to `parts` being stored natively.

**Verdict**: ✅ **hypothesis confirmed — in Phase D, restructure so `rawTypeNameParts`
is the canonical form and `rawTypeName` is derived as `parts.joinToString('.')`.**
For the 97 % single-segment case the cached `List` now holds `[simpleName]` instead
of a separate `String` plus `List<String>`, saving one object per classifier type.
On a large codebase (the Kotlin project creates tens of thousands of
`JavaClassifierTypeOverAst` instances) this is ~10 s of thousand reclaimed allocations.

Caveat: 392 distinct types is a small sample. Keep the measurement counter behind
a flag if Phase D wants to reconfirm on a bigger corpus (a partial IntelliJ run
will settle it with an order of magnitude more types).

---

### M-P13 — `findPackageDirectories`

**Hypothesis**: the `pathSegments().map` pre-allocation happens on every call and
is hot.

**Current code (post Phase B.3)**: the `.map` pre-allocation already lives INSIDE
`computeIfAbsent`, so it only fires on cache miss — the hypothesis' surface
concern has already been addressed. What remains to measure is whether the cache
itself is worthwhile.

| Corpus | calls | compute runs | hit-rate |
|---|---:|---:|---:|
| A | 46 715 | 44 242 | **5.29 %** |
| B | 44     | 43     | 2.27 %   |

Corpus A has a ~5 % cache hit rate. Of 46 715 calls, 2 473 hit — each a saved
`pathSegments().map + directoryRoots.mapNotNull + .findChild` walk. That is a
non-negligible 2 500 walks saved over a ~180 s run, but the walk itself is cheap
(a handful of `findChild` string comparisons). The cache earns its keep; it is
not strongly beneficial.

**Verdict**: ⚠️ **hypothesis partially confirmed (already fixed) — no Phase D
action needed.** The original concern about eager allocation outside the cache
check is moot in current code; the cache itself has a 2–5 % hit rate in practice,
which is low but not negative. Leave the code as-is. Remove the "small, near-certain
win" note from `REFACTORING_PLAN.md`.

---

## 3. Summary & Phase D recommendations

| ID | Hypothesis | Verdict | Phase D action |
|---|---|---|---|
| **M-O3**  | `importCache` hit rate near 0 | ✅ confirmed (0/3724) | **Delete** `importCache` + `WeakHashMap` import + sync wrapper |
| **M-O6**  | `SMALL_FILE_SIZE_THRESHOLD` material | ⚠️ null | **Close as null result**, drop "unvalidated" note |
| **M-O4b** | `negativeClassCache` prevents re-parses | ✅ confirmed (0 prevented) | **Delete** `negativeClassCache` + absorbs O4 |
| **M-O2**  | `distinctStarImports` field saves work | ✅ confirmed (0 duplicates) | **Inline** `.distinct()` at use site, drop field from constructor (simplifies R9) |
| **M-P8**  | `deriveImplicitPermittedTypes` hot | ❌ null (0 invocations) | **No change** |
| **M-P9**  | `fields.find` scans hot | ❌ null (0 invocations) | **No change** |
| **M-P12** | Single-segment `rawTypeNameParts` dominant | ✅ confirmed (97 %) | **Restructure**: store parts, derive name |
| **M-P13** | `findPackageDirectories` pre-alloc hot | ⚠️ already fixed | **No change**, edit plan |

### Landing order

Phase D should land these in three self-contained commits:

1. **Deletions (M-O3, M-O4b, M-O2)** — all three remove code; each ~5–20 lines.
   Lowest risk, do first. Saves a `WeakHashMap`, a `ConcurrentHashMap.newKeySet`,
   a `List<FqName>` copy, and one constructor parameter.
2. **`rawTypeNameParts` restructure (M-P12)** — ~20 lines in `JavaTypeOverAst.kt`.
   Medium risk (changes the canonical form); covered by existing tests.
3. **Plan cleanup (M-O6, M-P8, M-P9, M-P13)** — documentation-only; update
   `REFACTORING_PLAN.md` and delete the Phase C instrumentation from the code.

Expected combined impact on full-pipeline CPU: under 1 %. None of these items
individually reach the 2 % measurement floor on the Kotlin full pipeline, so the
primary payoff is simpler code, not speed. The `REFACTORING_PLAN.md` expected
outcome line should be softened accordingly.

### Items NOT measured

`REFACTORING_PLAN.md` §2.3 listed 8 items; **all 8 were measured** in the single
instrumented run described in §1. No Phase C follow-up is needed.

---

## 4. Reproducing the measurements

With the instrumentation still in place (see `PhaseCMeasurementCounters.kt` and the
 call-site edits listed in §1.1):

```bash
./gradlew dist
./gradlew :compiler:fir:modularized-tests:test \
  --tests KotlinFullPipelineTestsGenerated \
  --rerun-tasks -Pfir.force.javaDirect=true
cat compiler/java-direct/phase-c-dumps/dump-*.txt
```

For the java-direct own-suite corpus (higher signal on M-O3, M-O4b, M-P13):

```bash
./gradlew :kotlin-java-direct:test \
  --tests JavaUsingAstPhasedTestGenerated \
  --tests JavaUsingAstBoxTestGenerated \
  --rerun-tasks
cat compiler/java-direct/phase-c-dumps/dump-*.txt
```

**Cross-reference (2026-04-22):** the same instrumentation was extended with per-function
`[PHASE-B]` CPU brackets on every Phase-B-touched hot path to investigate a user-reported
10 % regression on sequential `KotlinFullPipelineTestsGenerated`. The attribution is
documented in `ITERATION_RESULTS.md` under "Phase B regression investigation". Headline:
on the Kotlin pipeline (no Java source roots) java-direct never runs, so no Phase B
code fires there; on the IntelliJ 11-subset (where java-direct *does* run), only R5
(`buildJavaLightTree` split) fires, at 64.5 ms of thread-CPU across 400 parses, and the
split's direct overhead is ≤ 20 µs — not a credible regression source.

Instrumentation is **kept in place** (per user instruction 2026-04-22) for a follow-up
task that will reuse it. The files still instrumented are:

- `PhaseCMeasurementCounters.kt` — counters + shutdown-hook dump
- `JavaImportResolver.kt`, `JavaClassFinderOverAstImpl.kt`, `JavaResolutionContext.kt`,
  `JavaClassOverAst.kt`, `JavaMemberOverAst.kt`, `ConstantEvaluator.kt`,
  `JavaTypeOverAst.kt` — call-site counter increments
- `AbstractIsolatedFullPipelineModularizedTest.kt` — `fir.force.javaDirect=true` passthrough

---

## 5. Extended measurements — IntelliJ pipeline + file utilization + size buckets

**Date**: 2026-04-22
**Prompted by**: user request to (a) re-measure on the Java-heavy
`IntelliJFullPipelineTestsGenerated` corpus, (b) add file-utilization counters
(how many files are precomputed vs. how many are actually accessed, especially for
modules where most Java files remain unreferenced from Kotlin), and (c) report file-size
buckets at ≥1 K / ≥2 K / ≥4 K / ≥8 K boundaries for files that are actually addressed.

### 5.1 Extended instrumentation (v2)

The counter object was extended with:

- **Per-path file-size buckets** — `<1K`, `1K..<2K`, `2K..<4K`, `4K..<8K`, `≥8K` — tracked
  independently for the eager (`tryBuildFileEntryWithFullParse`), lightweight
  (`tryBuildFileEntryLightweight`), and lazy (`parseTopLevelClassFromFile` actual parse)
  paths.
- **Unique file sets** — `indexedFilePaths`, `accessedFilePaths`, `reallyParsedLazyFilePaths`
  (each a `ConcurrentHashMap.newKeySet<String>`). `accessedFilePaths.add` fires inside the
  `findClasses` loop for every candidate `FileEntry` handled, so it counts any file whose
  classes were queried even if the result came from `classCache`.
- **Lazy-parse path metrics** — `lazyParseInvocations`, `lazyParseBytesTotal`,
  `lazyParseCpuNs`, `parseTopLevelCacheHits`, `parseTopLevelCacheMisses`.

Dump format is unchanged apart from the new `[EXT-FILES]`, `[EXT-PARSE-TOP]`, and
`[EXT-BUCKETS]` sections. A shell aggregator — `compiler/java-direct/aggregate-phase-c-dumps.sh`
— sums per-worker `dump-<pid>.txt` files into a single report.

### 5.2 Execution model

Kept `ExecutionMode.CONCURRENT` from `GenerateModularizedIsolatedTests.kt:27`. All counters
are lock-free (`AtomicLong`, `ConcurrentHashMap.newKeySet`) and `dump()` is `@Synchronized`;
`ThreadMXBean.getCurrentThreadCpuTime()` is per-thread and aggregates correctly under
concurrent execution.

An earlier attempt to run all 3 320 `testIntellij_*` tests with
`-Pfir.force.javaDirect=true` ran for several hours before being aborted. Root cause is
scale, not a hang: each IntelliJ module compile under the forced java-direct path is a
full pipeline run over a project with thousands of Java files, so 3 320 of them is a
multi-hour workload regardless of concurrency. The replacement is a representative
11-test subset covering different module shapes (Java-heavy platform, Kotlin-heavy
Kotlin-base, Fleet, Util, RPC, Ai internal).

### 5.3 Corpora (v2)

- **Corpus B'** — Kotlin full pipeline re-run, v2 counters.
  Archive: `compiler/java-direct/phase-c-dumps/_v2-kotlin-pipeline.txt`.
- **Corpus C** — IntelliJ 11-test subset, v2 counters (`testIntellij_aiInternalDataCollection`,
  `testIntellij_platform_util`, `testIntellij_platform_util_base`,
  `testIntellij_platform_core_impl`, `testKotlin_base_analysis`,
  `testKotlin_base_compiler_configuration`, `testKotlin_base_code_insight_minimal`,
  `testFleet_andel`, `testFleet_andel_test`,
  `testUtil_android_studio_android_studio_roboscope_common`,
  `testRpc_compiler_plugin`).

### 5.4 Headline numbers

| Metric | Corpus B' (Kotlin pipeline) | Corpus C (IntelliJ 11-test subset) |
|---|---:|---:|
| Files indexed (precomputed)           | 3       | **562**   |
| Files accessed                        | 0       | 1         |
| **Access rate**                       | 0.00 %  | **0.18 %** |
| Lightweight invocations               | 1       | 165       |
| Full-parse invocations                | 2       | **397**   |
| Lightweight CPU total (ms)            | 3.5     | 122.2     |
| Full-parse CPU total (ms)             | 126.4   | **458.4** |
| Lazy-parse invocations                | 0       | 1         |
| Lazy-parse CPU (ms)                   | 0       | 2.5       |
| `findPackageDirectories` hit-rate     | 0.40 %  | 0.63 %    |
| `importCache` hit-rate                | 0.00 %  | 0.00 %    |

Traffic on the non-file-related counters (M-O2, M-P8, M-P9, M-P12) stayed at zero for
both corpora, confirming the original §2 finding that these code paths are cold in real
pipeline workloads — the deep type-resolution through Java-source classifier types only
fires when Kotlin code references a Java source class directly, and even on the
Java-heavy IntelliJ subset that didn't happen at the scale needed for signal.

### 5.5 File-size distribution (buckets)

Buckets: `<1K`, `1K..<2K`, `2K..<4K`, `4K..<8K`, `≥8K`.

| Corpus | path | `<1K` | `1-2K` | `2-4K` | `4-8K` | `≥8K` |
|---|---|---:|---:|---:|---:|---:|
| B' (Kotlin)  | eager       | 1   | 0   | 1   | 0  | 0  |
| B' (Kotlin)  | lightweight | 0   | 0   | 0   | 0  | 1  |
| B' (Kotlin)  | lazy        | 0   | 0   | 0   | 0  | 0  |
| **C (IntelliJ)** | eager       | **160** | **131** | **106** | 0  | 0  |
| **C (IntelliJ)** | lightweight | 0   | 0   | 0   | **79** | **86** |
| **C (IntelliJ)** | lazy        | 0   | 0   | 0   | 0  | **1**  |

Interpretation:

- The two paths are **perfectly partitioned by the 4 K threshold** — no crossover, which
  is the expected outcome of `SMALL_FILE_SIZE_THRESHOLD = 4096` applied to `file.length`
  at index time.
- Eager distribution within the small bucket is even (160/131/106 across `<1K`, `1-2K`,
  `2-4K`), so the threshold has no sweet spot inside 0-4K — each sub-bucket is
  comparably populated.
- Lightweight is similarly distributed: 79 files in 4-8 K, 86 files ≥ 8 K. The "rest"
  (≥ 8 K) is the largest single bucket, confirming the lightweight path's purpose of
  avoiding parser cost on large files.
- **Lazy parse hit only the ≥ 8 K bucket exactly once.** All 165 large files indexed
  were precomputed-lightweight; exactly 1 of them triggered an actual parse on demand.

### 5.6 File utilization — the key finding

Of the **562 Java files indexed** across the corpus-C subset, **exactly 1 file was ever
asked for a class by the compiler**. The other 561 files were indexed (397 of them
fully parsed during the eager path) but their contents were never actually needed.

This translates directly into wasted CPU:

| Path | invocations | total CPU (ms) | access-rate | wasted CPU (ms) |
|---|---:|---:|---:|---:|
| Eager (full parse) | 397 | 458.4 | ≤ 1/397 ≈ **0.25 %** | ≈ 457 |
| Lightweight        | 165 | 122.2 | 1/165 ≈ **0.61 %**    | ≈ 121 |
| Lazy parse         | 1   | 2.5   | 100 %                 | 0   |
| **Total indexed**  | 562 | 580.6 | 1/562 ≈ **0.18 %**    | **≈ 578** |

**Removing the eager full-parse path entirely** (threshold → 0, all files go lightweight,
lazy-parse on demand) would eliminate ~458 ms of CPU across the 11-test subset without
changing correctness — the one file that needed parsing would still get parsed on first
access, adding ~2.5 ms back. Net savings ≈ 455 ms per 11-module workload.

Across the full IntelliJ corpus the ratio should hold: typical Kotlin compilation
imports a handful of Java classes (often 0) per module, while module classpaths contain
thousands of candidate Java sources. Eager precomputation assumes the opposite.

### 5.7 Revised verdicts (incorporating §5 data)

The original §2 verdicts for M-O3, M-O4b, M-O2, M-P8, M-P9, M-P12, M-P13 **stand unchanged**
— the extended v2 counters reproduced the same signal on both corpora.

The M-O6 verdict **reverses**: from "keep current threshold, close as null result" to:

> ✅ **M-O6 hypothesis confirmed in the stronger form** — the eager full-parse path is
> 99.7 % wasted work on the measured workload. **Phase D action: drop
> `tryBuildFileEntryWithFullParse` and route all files through `tryBuildFileEntryLightweight`,
> relying on `parseTopLevelClassFromFile` to pay the parse cost only on first access.**
> The `SMALL_FILE_SIZE_THRESHOLD` constant and the two-branch dispatcher in
> `tryBuildFileEntry` become dead code and can be deleted along with the eager path.

The §3 summary table below is amended accordingly. The estimated pipeline-wide CPU
reduction rises from "under 1 %" to a likely **3–10 %** on Java-heavy Kotlin
compilations — the eager path was the dominant fixed cost for every module with Java
sources, and cutting it scales linearly with module count.

### 5.8 Open caveats

- **Corpus C is 11 tests.** The single "accessed" file is representative of the shape
  of the traffic (one Java class per test module referenced from the Kotlin code being
  compiled) but the exact ratio may shift on bigger workloads. The conclusion — access
  rate is orders of magnitude below precompute rate — is robust; the exact savings
  would need a 100+ test run to tighten.
- **`fir.force.javaDirect=true` override** vs. the production default: the production
  default of `false` means eager precompute only happens when a user explicitly opts
  in, so removing the eager path does not affect any shipped workload today. This is
  a refactor-for-readiness change, not an in-production speedup.
- **Path resolution is `rootPathPrefix`-sensitive.** The `runTest` entry point resolves
  java source roots via `modelFile.parentFile.parentFile.path + "/"` rather than the
  `fir.bench.prefix` system property. When the model dumps' `/testProject/...` paths
  do not resolve to extant directories (the normal state after a fresh checkout), the
  indexer walks empty directories and the counters show zeros. The data in this
  section was captured on a workstation with the test-project tree populated under
  `/Users/ich-jb/Work/kotlin/testdata/intellij/testProject/`; re-running elsewhere
  requires the same tree to be present.

---

## 6. Summary after §5 revisions

| ID | Hypothesis | Final verdict | Phase D action |
|---|---|---|---|
| **M-O3**  | `importCache` hit rate near 0 | ✅ confirmed | **Delete** `importCache` + `WeakHashMap` |
| **M-O6**  | `SMALL_FILE_SIZE_THRESHOLD` material | ✅ **confirmed in stronger form** (§5) | **Drop eager path entirely**; always lightweight-index, lazy-parse |
| **M-O4b** | `negativeClassCache` prevents re-parses | ✅ confirmed (0 prevented) | **Delete** |
| **M-O2**  | `distinctStarImports` field saves work | ✅ confirmed (0 duplicates) | **Inline** `.distinct()` at use site |
| **M-P8**  | `deriveImplicitPermittedTypes` hot | ❌ null | **No change** |
| **M-P9**  | `fields.find` scans hot | ❌ null | **No change** |
| **M-P12** | Single-segment `rawTypeNameParts` dominant | ✅ confirmed (97 %) | **Restructure**: parts canonical, name derived |
| **M-P13** | `findPackageDirectories` pre-alloc hot | ⚠️ already fixed | **No change** |

### Landing order (revised)

Phase D commit sequence:

1. **Deletions (M-O3, M-O4b, M-O2)** — 5–20 lines each, lowest risk.
2. **Eager path removal (M-O6)** — 60–80 lines in `JavaClassFinderOverAstImpl.kt`.
   `tryBuildFileEntry`, `tryBuildFileEntryWithFullParse`, `SMALL_FILE_SIZE_THRESHOLD`
   all delete; `tryBuildFileEntryLightweight` becomes the single indexing path and
   its redundant prefix can be trimmed. Medium risk (changes the hot path for every
   java-direct module), covered by the full java-direct test suite and the Kotlin
   full-pipeline run.
3. **`rawTypeNameParts` restructure (M-P12)** — ~20 lines in `JavaTypeOverAst.kt`.
4. **Plan cleanup (M-P8, M-P9, M-P13)** — documentation-only.

Expected combined impact: up to ~10 % on Java-heavy Kotlin module compiles (M-O6
dominates); other items are simplifications.

---

## 7. Corrected Kotlin-pipeline measurements — 2026-04-22 (v5)

**Date**: 2026-04-22
**Prompted by**: discovery that the original Corpus B (§2) and §5 measurements were
unreliable due to Gradle's per-test classloader isolation. Each of the 414 test
methods loads java-direct code in its own classloader; the original single-dump
approach captured only one classloader's data, producing near-zero counters that
were mistaken for "no traffic". The per-classloader filename fix
(`dump-$pid-$cl-$ts.txt`) was applied, and this section documents a fresh rerun
that confirms the corrected data.

### 7.1 Methodology fix

The `PhaseCMeasurementCounters.dump()` method writes to
`dump-$pid-$cl-$ts.txt` where `$cl = System.identityHashCode(class)` and
`$ts = System.nanoTime()`. This produces one dump file per classloader instance.
The shell aggregator `aggregate-phase-c-dumps.sh` sums across all dump files.

### 7.2 Fresh run (v5)

- Corpus: `KotlinFullPipelineTestsGenerated` (414 modules), `SAME_THREAD`,
  `-Pfir.force.javaDirect=true`.
- Wall-clock: 7 min 48 s.
- Dump files produced: **109** (one per module with Java source roots).
- Archive: `compiler/java-direct/phase-c-dumps/_v5-kotlin-pipeline-fresh-rerun.txt`.

The v5 run reproduces the v4 data exactly (all invocation counts identical; CPU
timing varies by ±5%, expected noise).

### 7.3 Original vs. corrected Corpus B data

| Metric | Original §2 Corpus B | Corrected (v5) | Factor |
|---|---:|---:|---:|
| importCache misses | 2 | 1,360 | 680× |
| full-parse invocations | 2 | 1,079 | 540× |
| lightweight invocations | 1 | 995 | 995× |
| findClass calls | 0 | 63,570 | — |
| positive-cache hits | 0 | 58,979 | — |
| negative-cache hits | 0 | 0 | — |
| tryStarImports calls | 4,949 | 953,835 | 193× |
| rawTypeNameParts computed | 392 | 13,431 | 34× |
| single-segment share | 96.94 % | 83.52 % | — |
| findPackageDirectories calls | 44 | 11,457 | 260× |
| files indexed | 3 | 2,074 | 691× |
| files accessed | 0 | 493 | — |
| **access rate** | **0 %** | **23.77 %** | — |
| lazy parses | 0 | 266 | — |

The original Corpus B captured one classloader's data. The corrected data
aggregates all 109 modules with Java source roots.

### 7.4 Revised verdicts

**M-O3 (importCache)** — verdict **unchanged**: 0 hits / 1,360 misses. **Delete.**

**M-O6 (SMALL_FILE_SIZE_THRESHOLD)** — verdict **revised downward** from §5:

The §5 data (IntelliJ 11-subset, 0.18 % access rate) led to the conclusion that
99.7 % of eager-parse CPU was wasted. The corrected Kotlin-pipeline data shows a
**23.77 % access rate** — still a minority, but dramatically higher.

Cost breakdown (v5, 109 modules aggregate):

| Path | Invocations | Total CPU (ms) | Total bytes |
|---|---:|---:|---:|
| Eager (full-parse) | 1,079 | 6,357 | 2.3 MB |
| Lightweight | 995 | 1,446 | 94.4 MB |
| Lazy parse | 266 | 1,711 | 8.7 MB |
| **Total** | **2,074** | **9,514** | — |

Of 493 accessed files, 227 were served from the eager cache (no lazy parse needed)
and 266 required lazy parsing (from lightweight-indexed files). This means:

- 852 of 1,079 eager parses (79 %) were wasted (files never accessed).
- Wasted eager CPU ≈ 852/1,079 × 6,357 = **5,019 ms**.

If the eager path were removed (all files lightweight-indexed, lazy-parse on demand):

| Component | Current (ms) | Proposed (ms) | Delta |
|---|---:|---:|---:|
| Eager parse | 6,357 | 0 | −6,357 |
| Lightweight (existing) | 1,446 | 1,446 | 0 |
| Lightweight (new, for <4K files) | — | ~33 | +33 |
| Lazy parse (existing) | 1,711 | 1,711 | 0 |
| Lazy parse (new, for 227 ex-eager files) | — | ~1,373 | +1,373 |
| **Total** | **9,514** | **4,563** | **−4,951** |

**Net savings: ~4,951 ms (52 % of indexing/parse time).**

On the full 7m 48s pipeline: 4,951 / 468,000 ≈ **1.1 %** — below the 2 %
measurement floor for declaring a performance improvement, but a meaningful code
simplification (removes one entire indexing code path).

**Revised M-O6 verdict**: ✅ still confirmed — drop the eager path for code
simplification. The performance benefit is 52 % of indexing CPU (not 99.7 % as §5
claimed) and translates to ~1 % pipeline-wide. The primary payoff is simpler code,
not speed. The §5 "3–10 % on Java-heavy Kotlin compilations" estimate should be
revised to "~1–2 % on Kotlin-pipeline; potentially higher on genuinely Java-heavy
workloads like IntelliJ".

**M-O4b (negativeClassCache)** — verdict **unchanged**: 0 negative-cache hits
despite 4,103 adds and 63,570 findClass calls. **Delete negative cache.**

New finding: the **positive** class cache has a 92.8 % hit rate (58,979 / 63,570).
This is a critical hot cache and must be preserved.

**M-O2 (distinctStarImports)** — verdict **unchanged**: 953,835 calls, 0
duplicates. **Inline/drop field.**

**M-P8, M-P9** — verdicts **unchanged**: 0 invocations. **No change.**

**M-P12 (rawTypeNameParts)** — verdict **confirmed but weakened**:
- Single-segment share: 83.52 % (not 96.94 % as §2 reported).
- 2,213 multi-segment types (not 12).
- Still a majority single-segment; the restructure is beneficial but the savings
  per type are less dominant. **Still proceed with restructure.**

**M-P13 (findPackageDirectories)** — verdict **unchanged**: 0.95 % hit rate.
**No change.**

### 7.5 Corrected summary table

| ID | Hypothesis | Corrected verdict | Phase D action |
|---|---|---|---|
| **M-O3**  | `importCache` hit rate near 0 | ✅ confirmed (0/1,360) | **Delete** |
| **M-O6**  | Eager path wasted | ✅ confirmed (79 % waste) | **Drop eager path** (code simplification; ~1 % pipeline-wide) |
| **M-O4b** | `negativeClassCache` prevents re-parses | ✅ confirmed (0 prevented) | **Delete negative cache** |
| **M-O2**  | `distinctStarImports` field saves work | ✅ confirmed (0 duplicates / 954 K calls) | **Inline/drop field** |
| **M-P8**  | `deriveImplicitPermittedTypes` hot | ❌ null (0 invocations) | **No change** |
| **M-P9**  | `fields.find` scans hot | ❌ null (0 invocations) | **No change** |
| **M-P12** | Single-segment `rawTypeNameParts` dominant | ✅ confirmed (83.5 %, weaker than 97 %) | **Restructure** |
| **M-P13** | `findPackageDirectories` pre-alloc hot | ⚠️ already fixed | **No change** |

### 7.6 Revised landing order

Phase D commit sequence (unchanged from §6 except for softened performance claims):

1. **Deletions (M-O3, M-O4b, M-O2)** — 5–20 lines each, lowest risk.
2. **Eager path removal (M-O6)** — 60–80 lines in `JavaClassFinderOverAstImpl.kt`.
   Code simplification with modest (~1 %) pipeline-wide speedup.
3. **`rawTypeNameParts` restructure (M-P12)** — ~20 lines in `JavaTypeOverAst.kt`.
4. **Plan cleanup + instrumentation removal** — delete `PhaseCMeasurementCounters.kt`
   and all call-site counter increments.

**Expected combined impact**: code simplification with ~1–2 % pipeline-wide CPU
reduction on the Kotlin-pipeline corpus. The §5 "up to 10 %" estimate was based on
the IntelliJ 11-subset's 0.18 % access rate; the Kotlin pipeline's 23.77 % access
rate is a more representative baseline for that corpus. See §7.7 for the IntelliJ
platform data which reinstates a low access rate.

### 7.7 IntelliJ platform corpus (v6)

- Corpus: `IntelliJFullPipelineTestsGenerated.testIntellij_platform_*`
  (446 modules), `CONCURRENT`, `-Pfir.force.javaDirect=true`.
- Wall-clock: 4 min 30 s.
- Dump files produced: **446**.
- Archive: `compiler/java-direct/phase-c-dumps/_v6-intellij-platform-446-concurrent.txt`.

| Metric | Kotlin pipeline (v5, 109) | IntelliJ platform (v6, 446) |
|---|---:|---:|
| importCache misses | 1,360 | 9,422 |
| full-parse invocations | 1,079 | **9,046** |
| lightweight invocations | 995 | **3,998** |
| full-parse CPU (ms) | 6,357 | **28,862** |
| lightweight CPU (ms) | 1,446 | 2,943 |
| findClass calls | 63,570 | 828 |
| positive-cache hit rate | 92.8 % | 4.7 % |
| negative-cache hits | 0 | 0 |
| tryStarImports calls | 953,835 | 0 |
| rawTypeNameParts computed | 13,431 | 0 |
| files indexed | 2,074 | **13,044** |
| files accessed | 493 | **115** |
| **access rate** | **23.77 %** | **0.88 %** |
| lazy parses | 266 | 76 |
| lazy CPU (ms) | 1,711 | 507 |

**Key observations:**

1. **Access rate is 0.88 %** — only 115 of 13,044 indexed files are accessed. This is
   dramatically lower than the Kotlin pipeline's 23.77 %, and consistent with the
   original §5 IntelliJ 11-subset (0.18 %). IntelliJ platform modules are Java-heavy
   but Kotlin code in them rarely references Java source classes directly (it references
   JDK/classpath classes instead).

2. **Eager path waste is extreme.** 9,046 eager parses costing 28.9 seconds; at 0.88 %
   access rate, ~99 % of that CPU is wasted. Removing the eager path would save
   ~28.6 seconds across 446 modules (64 ms per module average).

3. **Type resolution paths are cold.** `tryStarImports` = 0, `rawTypeNameParts` = 0.
   M-O2 and M-P12 do not affect this workload at all.

4. **Positive class cache is cold.** Only 4.7 % hit rate (39/828) vs 92.8 % on the
   Kotlin pipeline. Most `findClass` calls are first-and-only lookups.

**M-O6 revised verdict (combining all corpora):**

The access rate is workload-dependent:
- Kotlin pipeline (Kotlin project, mixed Java/Kotlin): **23.77 %** → 52 % indexing savings
- IntelliJ platform (Java-heavy, Kotlin rarely references Java sources): **0.88 %** → 99 % indexing savings

Both workloads benefit from dropping the eager path, but the IntelliJ workload
benefits far more. The eager path is the dominant java-direct cost on IntelliJ:
28.9 seconds out of ~32.3 seconds total indexing+parse (89 %).

On Kotlin pipeline: savings ≈ 5.0 s / 468 s total = **~1 % pipeline-wide**.
On IntelliJ platform 446-test subset: savings ≈ 28.6 s / 270 s total = **~10.6 % pipeline-wide**.

This confirms the original §5 magnitude estimate for Java-heavy workloads while
showing the Kotlin pipeline is a lower-bound case.
