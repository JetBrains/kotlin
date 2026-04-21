# Java-Direct: Cross-Reviewed Refactoring Plan

**Date**: 2026-04-21
**Inputs**: Reviews `implDocs/reviews/r1.md`, `r2.md`, `r3.md`, `r4.md`.
**Baseline**: 1168/1168 box + 1454/1456 phased (2 known won't-fix).

This plan consolidates findings across all four reviews, marks where they agree, and orders
the follow-up work so that low-risk wins land first, risky/uncertain items wait for
measurement, and readability cleanup comes last.

---

## 1. Combined, Deduplicated Problem List

Numbering is stable so later sections can refer to items by ID. The "Found in" column lists
the reviews that raised each point; items agreed upon by 3+ reviewers are highlighted with
**(consensus)**.

### 1.1 Correctness issues

| # | Problem | Files | Found in |
|---|---------|-------|----------|
| **C1** | `isInitializerPotentiallyConstant` compares `tree.getType(it).toString() != "NULL_LITERAL"` — the actual token is `JavaSyntaxTokenType.NULL_KEYWORD`, so the check likely always returns `true` for literal nulls. Same call site also allocates a string per comparison. | `JavaMemberOverAst.kt:203-206` | r2, r4 |
| **C2** | `aggregatedInheritedInnerClassesHolder: Array<Map<..>?>(1)` is read/written without `@Volatile` semantics on the slot. Under concurrent FIR resolution, visibility of `holder[0] = result` is not guaranteed; callers may re-compute or see a stale map. | `JavaResolutionContext.kt:46-53` | r2, r4 |
| **C3** | `findPackage(fqName)` returns `null` for packages that exist only via `package-info.java` (because `ensurePackageIndexed(fqName).isEmpty()` triggers the early return) even though their annotations are tracked. Loss of package-level annotations. | `JavaClassFinderOverAstImpl.kt:280-287` | r1, r4 |
| **C4** | `ConstantEvaluator.findLocalClass` constructs a fresh `JavaClassOverAst` from an AST node instead of going through the cached `resolutionContext.findLocalClass(...)`. Breaks type-parameter object identity used by FIR, and defeats the class cache. | `ConstantEvaluator.kt:256-272` | r3, r4 |
| **C5** | `JavaSourceIndex.DECLARATION_REGEX.find(effective)` picks up only the first declaration per line. Rare in real code (multi-declaration on one line), but an edge-case correctness gap. | `JavaSourceIndex.kt:103` | r3 |

### 1.2 Overengineering / dead code

| # | Problem | Files | Found in |
|---|---------|-------|----------|
| **O1** | `startForDone: IntArray` is populated in Pass 1 of `buildJavaLightTree` but annotated `@Suppress("unused")` and never read. Pure waste of allocation + Pass-1 writes. | `JavaLightTree.kt:56, 264` | r3, r4 |
| **O2** | `distinctStarImports: List<FqName>` eagerly computed and threaded through every `withTypeParameters` / `withInheritedTypeParameters` / `withContainingClass` copy for a deduplication that matters only at one call site (step 5 of `resolveSimpleNameToClassIdImpl`). | `JavaResolutionContext.kt:33` | r3, r4 |
| **O3** | `JavaImportResolver.importCache` (synchronized WeakHashMap) — hit rate in practice is suspected near-zero because `JavaResolutionContext.create(tree, ...)` is normally invoked once per new `tree` instance. Paying wrapper overhead for no measured benefit. | `JavaImportResolver.kt:36-37, 60-65` | r1, r3, r4 **(consensus)** |
| **O4** | `negativeClassCache` uses the outdated `Collections.newSetFromMap(ConcurrentHashMap())` idiom; should be `ConcurrentHashMap.newKeySet()`. Also, its narrow scope (only inner-class misses on top-levels already in index) may not justify the cache at all. | `JavaClassFinderOverAstImpl.kt:94-95` | r2, r3, r4 **(consensus)** |
| **O5** | `@Suppress("ArrayInDataClass")` is applied to `JavaResolutionContext`, which is not a `data class`. Useless suppression. | `JavaResolutionContext.kt:45` | r2, r4 |
| **O6** | `SMALL_FILE_SIZE_THRESHOLD = 4096` is an unvalidated magic number that controls which of two indexing code paths applies. `ITERATION_RESULTS.md` already lists it as unprofiled. | `JavaClassFinderOverAstImpl.kt:25-32` | r1 |
| **O7** | `JavaAnnotationOverAst` carries a second mini constant-expression evaluator (`evaluateConstantExpression` / `evaluateBinaryExpression` / `numericBinaryOp`) that duplicates `ConstantEvaluator` logic. Contains two TODOs expressing uncertainty about spec coverage and consolidation. | `JavaAnnotationOverAst.kt:163-213` | r1, r3, r4 **(consensus)** |
| **O8** | `JavaMethodOverAst` declares `_methodModifierList` that duplicates the base class's `_baseModifierList`. Two cache slots and two `@Volatile` fields cache the same AST lookup per instance. | `JavaMemberOverAst.kt:34-37, 291-295` | r4 |
| **O9** | `findChildByType(node, typeName: String)` / `getChildrenByType(node, typeName: String)` String overloads exist only because `isDeprecatedInJavaDoc` compares against `"DOC_COMMENT"`. `SyntaxElementType.toString()` allocates a String per child. Replace the single call site with a typed constant and delete the overloads. | `JavaLightTree.kt:181-188, 201-210` | r3, r4 |
| **O10** | `JavaResolutionContext.resolveSimpleNameToClassIdImpl` / `resolveNestedClassToClassIdFromParts` each take a `checkInheritance: Boolean` that gates several unrelated branches. Collapsing was the right fix (prevents divergence) but the resulting dual-mode body is now dense; split again once safety is guaranteed by tests. | `JavaResolutionContext.kt:309-410` | r4 |

### 1.3 Performance issues

| # | Problem | Files | Found in |
|---|---------|-------|----------|
| **P1** | `JavaTypeOverAst.annotations` (cached) and `filterTypeUseAnnotations()` (uncached) both walk the AST for modifier-list + direct annotations, producing duplicate `JavaAnnotationOverAst` wrappers. FIR calls both on the same type. | `JavaTypeOverAst.kt:33-76` | r1, r4 |
| **P2** | `JavaMemberOverAst.computeLeadingFieldNode` uses `(myIndex - 1 downTo 0).map { siblings[it] }.firstOrNull { ... }` — allocates an intermediate list. Replace with a backward `for` loop. | `JavaMemberOverAst.kt:104-113` | r2, r4 |
| **P3** | `JavaResolutionContext.resolveAsClassId` calls `parts.subList(...).map { it.asString() }` twice per loop iteration on a hot resolution path. Pre-map `parts` to `List<String>` once before the loop. | `JavaResolutionContext.kt:462-474` | r2, r3, r4 |
| **P4** | `JavaMemberOverAst.initializerNode` is uncached but accessed at least twice (from `hasConstantNotNullInitializer` and from `initializerValue` / `resolveInitializerValue`). | `JavaMemberOverAst.kt:167-175` | r3, r4 |
| **P5** | `JavaMemberOverAst.hasAnnotationParameterDefaultValue` is implemented as `annotationParameterDefaultValue != null`, which fully constructs the default value just to check existence. Cheap replacement: check for `DEFAULT_KEYWORD`. | `JavaMemberOverAst.kt:353` | r3, r4 |
| **P6** | `JavaClassifierTypeOverAst.isResolved` recomputes `findTypeParameter` / `getSimpleImport` even after `classifier` (which already performed those lookups) has been computed. Simplify to `classifier != null \|\| getSimpleImport(parts[0]) != null`. | `JavaTypeOverAst.kt:327-340` | r3, r4 |
| **P7** | `createJavaType` has two nearly identical array/wildcard handling paths (for TYPE input vs. derived typeNode); both scan children independently. Normalize to a TYPE node once, then handle all cases in a single pass. | `JavaTypeOverAst.kt:455-553` | r3, r4 |
| **P8** | `JavaClassOverAst.deriveImplicitPermittedTypes` returns a lazy `Sequence` that recomputes the filter + child walk on every consumer. Cache the result and extract the duplicated `substringBefore('<').trim()` helper. | `JavaClassOverAst.kt:316-346` | r4 |
| **P9** | `JavaFieldOverAst.isSimpleNamePotentiallyConstant` and `ConstantEvaluator.resolveFieldValue` use O(N) `fields.find { ... }` scans; quadratic in constant evaluation over classes with many fields. Cache fields by simple name. | `JavaMemberOverAst.kt:238-243`, `ConstantEvaluator.kt:290-293` | r4 |
| **P10** | `JavaSupertypeGraph.getInnerClassNames` re-builds `Set<String>` from the cached class's `innerClassNames` via `.map {...}.toSet()` on every call — but `innerClassNames` is already a `Collection<Name>`. | `JavaSupertypeGraph.kt:142-145` | r4 |
| **P11** | `JavaClassFinderOverAstImpl.knownClassNamesInPackage` builds a list then calls `.toSet()`. Could build the set directly with `buildSet { ... }`. Small win. | `JavaClassFinderOverAstImpl.kt:296-306` | r4 |
| **P12** | `rawTypeNameParts` is cached separately from `rawTypeName`, so both are held per type even for simple single-segment names where the split yields a single-element list. Potentially store parts and derive the joined name on demand. | `JavaTypeOverAst.kt:94-96` | r3 |
| **P13** | `findPackageDirectories` allocates `pathSegments().map { it.asString() }` before the `computeIfAbsent` check; inside the lambda it's fine, but the outer allocation happens on every call. | `JavaClassFinderOverAstImpl.kt:151` | r3 |

### 1.4 Readability issues

| # | Problem | Files | Found in |
|---|---------|-------|----------|
| **R1** | Redundant "Synthetic root wraps all top-level production markers" / "Returns a view over source without copying" / "class cache for already created JavaClassOverAst" / etc. — comments that restate self-evident code. | `JavaLightTree.kt:95, 98, 128`, `JavaClassFinderOverAstImpl.kt:76, 87, 99` | r1, r3, r4 |
| **R2** | `// computeIfAbsent (not getOrPut) so concurrent callers share a single ... instance` rationale duplicated at 4+ sites. Keep one canonical note in `CacheHelpers.kt` (or a module-level doc) and delete the rest. | `JavaClassFinderOverAstImpl.kt:285`, `JavaClassOverAst.kt:133`, `JavaScopeResolver.kt:50-54`, `JavaSupertypeGraph.kt` | r1, r3, r4 |
| **R3** | Javadoc `@deprecated` detection duplicated verbatim across `JavaMemberOverAst`, `JavaClassOverAst`, `JavaValueParameterOverAst`, record components — 4 copies of the same 3-line pattern + identical comment. Extract to `utils.kt`. | `JavaMemberOverAst.kt:70-72, 434-436`, `JavaClassOverAst.kt:381-383` | r1, r4 |
| **R4** | Interface-fields-implicit-modifiers comment repeated 3× in `JavaMemberOverAst`. Basic JLS trivia. | `JavaMemberOverAst.kt:271-273` | r3 |
| **R5** | `JavaLightTree.buildJavaLightTree` is a 235-line function with 3 passes, 8 nested local functions, and duplicated "build root children" logic. Split into `pass1Indices`, `pass2TokenParents`, `pass3Children`; generalize `buildChildrenFor` for the root case. | `JavaLightTree.kt:241-465` | r3, r4 |
| **R6** | `createJavaType` is ~100 lines with deeply nested branches. Extract `createArrayType`, `createWildcardType`, `createClassifierOrPrimitive`. Addresses P7 in the process. | `JavaTypeOverAst.kt:455-553` | r3, r4 |
| **R7** | `JavaInheritedMemberResolver.resolveInheritedInnerClassToClassId` (~100 lines) interleaves Phase 1 / Phase 2 BFS with `maxDepth` checks, ambiguity detection, and queue management. Extract phases into separate methods. | `JavaInheritedMemberResolver.kt:171-269` | r3 |
| **R8** | `JavaImportResolver.extractFragmentedImports` is deeply nested with lookahead indices; needs a named helper for the "find TYPE node and optional star" scan. | `JavaImportResolver.kt:~170-220` | r4 |
| **R9** | `JavaResolutionContext` constructor has 10 parameters including implementation details (`aggregatedInheritedInnerClassesHolder`, `distinctStarImports`). Group related ones or remove them (see O2, C2). | `JavaResolutionContext.kt:29-47` | r3 |
| **R10** | Cryptic names: `fri` (should be `fileRootIndexBuilder`); `byName` (should be `entriesByClassName`); `n` in `extractTypeName`/`collectIdentifiers` (should be `refNode`); `top2/push2/pop2` (the `2` is Pass-2 accidentalism — rename once the Pass is extracted). | Various | r3, r4 |
| **R11** | TODOs in `JavaAnnotationOverAst.kt` — `// TODO: check against specs` and `// TODO: check if it needs to be replaced with ConstantEvaluator` — signal unfinished thought to reviewers. Resolve along with O7. | `JavaAnnotationOverAst.kt:163, 174` | r4 |
| **R12** | `JavaResolutionContext.resolveSimpleNameToClassIdImpl` has 5 numbered steps with embedded multi-line comments. Split into `tryImport`, `tryLocalAndInherited`, `trySamePackage`, `tryJavaLang`, `tryStarImports`. Addresses O10. | `JavaResolutionContext.kt:346-410` | r3, r4 |
| **R13** | `aggregatedInheritedInnerClassesHolder` as `Array<...>(1)` is opaque; replace with `class Holder<T>(@Volatile var value: T?)`. Addresses C2 simultaneously. | `JavaResolutionContext.kt:46` | r1, r2, r3, r4 **(consensus)** |

---

## 2. Execution Plan

The plan groups work into five phases. Each phase ends with a full `:kotlin-java-direct:test`
run and, where a perf claim is made, an isolated pipeline measurement (see §2.3).

### 2.1 Phase A — Simple fixes (1–2 iterations, no measurements needed)

Low-risk, high-ROI, each change is a few lines and purely local. Grouped into two commits.

**Iteration A.1 — correctness + dead code:**
1. **C1** — fix the `"NULL_LITERAL"` string comparison (use `JavaSyntaxTokenType.NULL_KEYWORD`;
   verify the exact token name via `feedback_java_syntax_tokens.md` guidance).
2. **C2 + O5 + R13** — replace `Array<...>(1)` with a small internal `class Holder<T>(@Volatile var value: T?)`
   in `JavaResolutionContext`. Delete the inapplicable `@Suppress("ArrayInDataClass")`.
3. **C3** — in `findPackage`, also consult `packageAnnotationNodes` before returning `null`.
4. **C4** — route `ConstantEvaluator.findLocalClass`'s same-file fallback through
   `resolutionContext.findLocalClass` (or the class finder) instead of constructing a fresh
   `JavaClassOverAst`.
5. **O1** — delete `startForDone: IntArray` and its Pass-1 writes; drop the `@Suppress("unused")`.
6. **O4** — replace `Collections.newSetFromMap(ConcurrentHashMap())` with
   `ConcurrentHashMap.newKeySet()`.

**Iteration A.2 — trivial perf cleanups:**
7. **P2** — rewrite `computeLeadingFieldNode` with a reverse `for` loop.
8. **P3** — map `parts` to `List<String>` once before the loop in `resolveAsClassId`.
9. **P4** — add `cachedNullable` around `initializerNode`.
10. **P5** — reimplement `hasAnnotationParameterDefaultValue` as a keyword presence check.
11. **P6** — simplify `JavaClassifierTypeOverAst.isResolved`.
12. **P10** — in `JavaSupertypeGraph.getInnerClassNames`, keep the cached-class fast path as a
    `Set<String>` in a tiny memoized map, or return a lazy view over the class's
    `innerClassNames`.
13. **P11** — `knownClassNamesInPackage`: `buildSet { ... }` pass.
14. **C5** — `DECLARATION_REGEX.findAll` instead of `.find` (low priority; piggy-back here).

Risk: low — all changes are local and test-covered.
Gate: `./gradlew :kotlin-java-direct:test --tests JavaUsingAstPhasedTestGenerated --tests JavaUsingAstBoxTestGenerated --tests JavaParsingTest --rerun-tasks --no-build-cache`.

### 2.2 Phase B — Complex refactorings without measurement

These are larger restructurings whose benefit is code-quality rather than speed; no perf claim
needs verification. Best run after Phase A so the diffs don't overlap.

**Iteration B.1 — deduplicate walks and wrappers:**

15. **P1** — restructure `JavaTypeOverAst.annotations` vs. `filterTypeUseAnnotations`: cache the
    "type-position" annotation slice once and let `filterTypeUseAnnotations` reuse it, applying
    callback filtering only to `memberAnnotations`.
16. **P7 + R6** — consolidate `createJavaType`'s duplicated array/wildcard handling; extract
    `createArrayType`, `createWildcardType`, `createClassifierOrPrimitive`. Benchmark-neutral
    (same number of tree walks); primarily a readability win.
17. **O8** — drop the duplicate `_methodModifierList` in `JavaMethodOverAst`; reuse the base
    class's cache (already accessed via `hasModifier`).
18. **O9** — delete the String overloads of `findChildByType` / `getChildrenByType`. Replace the
    single `"DOC_COMMENT"` call site with the typed constant (or a `@JvmField` constant
    resolved lazily via `SyntaxElementType` registry — verify the name at the AST level
    following `feedback_java_syntax_tokens.md`).
19. **R3** — extract `isDeprecatedInJavaDoc(tree, node)` helper in `utils.kt` and delete the 4
    copies.

**Iteration B.2 — consolidate annotation-arg evaluation:**

20. **O7 + R11** — share `evaluateLiteral` + binary-op logic between `ConstantEvaluator` and
    `JavaAnnotationOverAst`. Either call `ConstantEvaluator` from the annotation path, or
    extract the shared core into `JavaLiteralParser` (which is the natural home given it
    already hosts literal parsing). Resolve the two TODOs.

**Iteration B.3 — split large functions:**

21. **R5** — split `buildJavaLightTree` into three passes plus a generalized `buildChildrenFor`
    that covers the root case (`startIdx = -1`).
22. **R10** — rename `fri` / `byName` / `n` / `top2/push2/pop2`.
23. **R7** — split `JavaInheritedMemberResolver.resolveInheritedInnerClassToClassId` into
    `findInPhase1JavaModel` and `findInPhase2ClassIdWalk`.
24. **R8** — extract the "find TYPE node and optional star" scan in
    `JavaImportResolver.extractFragmentedImports`.
25. **R12 + O10** — once test-stability is re-confirmed, split
    `resolveSimpleNameToClassIdImpl` into `tryImport` / `tryLocalAndInherited` /
    `trySamePackage` / `tryJavaLang` / `tryStarImports`, and split the
    `checkInheritance=false` flavor back out into its own small workhorse (keeps the
    false-branch footprint under 20 lines without reintroducing drift).

Risk: medium — test corpus catches regressions, but some of these touch the hot
resolution path. Run full `:kotlin-java-direct:test` after each sub-iteration.

### 2.3 Phase C — Measurements for remaining items

Items with a speed claim but no guarantee that the change moves the needle. These need data
before code. The measurement recipe below applies to all of them.

**Measurement setup (derived from `ITERATION_RESULTS.md`):**

- Benchmark target: `KotlinFullPipelineTestsGenerated` (414 modules, smaller, lower noise).
- **Sequential execution** is required — the April-20 LightTree benchmark showed concurrent
  mode has ~10% variance that hides 5% improvements. Edit
  `compiler/fir/modularized-tests/src/org/jetbrains/kotlin/fir/generators/tests/GenerateModularizedIsolatedTests.kt:27`
  from `ExecutionMode.CONCURRENT` to `ExecutionMode.SAME_THREAD`, then run
  `./gradlew :compiler:fir:modularized-tests:generateTests` to regenerate the driver.
- Before each benchmark run that measures a java-direct change, run
  `./gradlew dist` so the modularized tests pick up the new jar.
- Profiling method: the April-20 iteration used `ThreadMXBean.currentThreadCpuTime` counters
  inserted at the entry/exit of the methods of interest; `System.nanoTime()` is unreliable
  inside Gradle. Add counters on the specific methods relevant to each hypothesis (see per-item
  notes below).
- Each hypothesis: compare median of 3 runs before vs. after. Reject changes where the
  improvement is within the measurement floor (±2% on the full 414-module run).
- **Use `IntelliJFullPipelineTestsGenerated` sparingly.** Only to re-confirm a change that
  showed a clear signal on Kotlin-full-pipeline and that we want to verify scales to a larger
  workload. Run once, sequential, save output.

**Items to measure:**

| # | Hypothesis to confirm/reject | Counters to add |
|---|------------------------------|-----------------|
| **M-O3** | `JavaImportResolver.importCache` has near-zero hit rate; deleting it is neutral or positive. | Increment hit/miss counters on `extractImports`; log totals at the end of a run. |
| **M-O6** | `SMALL_FILE_SIZE_THRESHOLD = 4096` matters. Compare 1024 / 4096 / 16384 / ∞ (no lightweight path). | CPU time in `tryBuildFileEntryWithFullParse` vs. `tryBuildFileEntryLightweight` per file size bucket. |
| **M-O4b** | `negativeClassCache` prevents a measurable amount of re-parsing. | Count `parseTopLevelClassFromFile` invocations from `findClasses` with and without the cache. |
| **M-O2** | `distinctStarImports` is worth dedicating a field. Measure: does `starImports.distinct()` inline at the one use site produce a measurable slowdown? | CPU time in `resolveSimpleNameToClassIdImpl` before and after. |
| **M-P8** | `deriveImplicitPermittedTypes` is called often enough for a missing cache to matter. | Count invocations per sealed class; time spent. |
| **M-P9** | `isSimpleNamePotentiallyConstant` / `resolveFieldValue` O(N) field scans show up. | Measure `ConstantEvaluator.evaluate` CPU share. |
| **M-P12** | Single-segment `rawTypeNameParts` split is hot enough to matter. | Count ratio of single-segment to multi-segment type names + time in `rawTypeNameParts` getter. |
| **M-P13** | `findPackageDirectories` `pathSegments().map` pre-cache allocation shows in a profile. | Count invocations vs. cache hits. |

**Gate**: none of Phase D items proceed until the relevant counter data is in. An item that
fails its hypothesis (no measurable improvement or regression) is dropped; record the outcome
in `ITERATION_RESULTS.md` with the counter totals.

### 2.4 Phase D — Performance changes conditional on measurements

Implementations to land only for items whose Phase-C measurement confirmed the hypothesis.
Each lands in its own commit with the measurement data embedded in the commit message and
appended to `ITERATION_RESULTS.md`.

- **O3** (delete `importCache`) if M-O3 confirms ≤0.1% hit rate.
- **O6** (tune `SMALL_FILE_SIZE_THRESHOLD` or drop the dual path) if M-O6 shows a clear winner.
- **O4b** (delete `negativeClassCache`) if M-O4b shows it prevents <100 re-parses per run.
- **O2** (inline `distinctStarImports` / drop the field) if M-O2 is neutral.
- **P8** (cache `deriveImplicitPermittedTypes`) if M-P8 shows repeated invocations per sealed
  class.
- **P9** (field-name index for constant evaluation) if M-P9 shows >1% time in the scans.
- **P12** (store parts, derive name on demand) if M-P12 shows enough single-segment traffic.
- **P13** (move `pathSegments` pre-map inside the lambda) — small, near-certain win, but still
  worth verifying it's not a net loss from a different branch getting skipped.

### 2.5 Phase E — Remaining readability and simplification

Residual cleanup that was not part of Phase A/B. Safe to do in a single commit after
everything above.

- **R1** — delete the redundant comments listed in §1.4 R1.
- **R2** — collapse the `computeIfAbsent vs getOrPut` explanations; keep one canonical note
  in `CacheHelpers.kt` and remove the duplicates.
- **R4** — trim the interface-fields JLS trivia comments.
- **R9** — post-O2/C2, the context constructor will drop 2 parameters. If it's still >7,
  group related ones into a small `Imports(simple, star)` value class.

---

## 3. Dependencies and ordering

```
Phase A (simple fixes)
   │
   ├─ Phase C (measurements) ──┐
   │                            │
   └─ Phase B (refactorings)    └─ Phase D (measured perf changes)
           │                            │
           └──────────────┬─────────────┘
                          │
                      Phase E (comment / readability cleanup)
```

- Phase B's function-splits (R5/R6/R12) touch code that Phase D may also edit; keep them on
  separate commits. If Phase D lands a change in `buildJavaLightTree`, rebase Phase B over it
  rather than the other way round.
- Phase C's measurement instrumentation is temporary code — delete the counters at the end of
  the measurement iteration so the main branch stays clean.

---

## 4. Expected outcomes

- **After Phase A**: 1 correctness fix (C1), 1 thread-safety fix (C2), 1 coverage fix (C3),
  1 identity fix (C4), dead code removed (O1), several small perf wins (P2-P6, P10-P11, C5).
  Test count unchanged; diff: ~150 lines.
- **After Phase B**: ~500 lines of code removed or de-duplicated across `JavaLightTree`,
  `JavaTypeOverAst`, `JavaAnnotationOverAst`/`ConstantEvaluator`,
  `JavaResolutionContext`, `JavaInheritedMemberResolver`, `JavaImportResolver`. No perf
  regression expected, but benchmark once to confirm.
- **After Phase C/D**: potentially small (~1-3%) pipeline improvement from the items that
  measure positive, plus deletions of dead/outdated code for the items that measure neutral.
- **After Phase E**: review-ready code with uniform commenting style and no stray TODOs.

---

## 5. Non-goals / deferred

- **§2 #6 context-level `tryResolve` cache** — already deferred in `ITERATION_RESULTS.md`
  with a correctness argument; nothing in r1-r4 overturns that.
- **Closing the 20-25% gap vs. PSI** — `ITERATION_RESULTS.md` records that this gap is
  dominated by `buildIndex` (PSI has a pre-built IDE index) and per-file parsing cost, not the
  items in this plan. Do not conflate this plan with that larger goal.
