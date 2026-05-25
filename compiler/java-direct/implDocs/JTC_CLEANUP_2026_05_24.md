# `JavaTypeConversion.kt` — Pre-java-direct vs HEAD (2026-05-24)

> **Update — empirical sub-block probe (2026-05-24, second pass).** Earlier
> conclusions about "required" sub-blocks were mostly static code reading.
> A second probe instrumented 16 distinct sub-block markers across the
> file, ran the full java-direct suite (2793 tests, `BUILD SUCCESSFUL`),
> and tallied per-marker hit counts. Findings substantially change the
> "still needed" verdict for several sub-blocks. The empirical table is
> in the **Sub-block hit table** section below; categorical verdicts in
> the rest of the doc are updated accordingly.

> **Update — Category γ TYPE_USE refactoring landed (2026-05-25).**
> The cleanup proposed in the "Critical analysis (2026-05-25)" section
> below has been applied. `filterTypeUseAnnotationsIfNeeded`,
> `isTypeUseAnnotationClass`, `hasTypeUseTarget`, `isTypeUseElement`,
> the `additionalTypeUseAnnotations` filter, and the
> `JavaTypeWithExternalAnnotationFiltering` interface have all been
> removed; TYPE_USE filtering now lives in `JavaTypeOverAst.annotations`
> driven by `JavaResolutionContext.isTypeUseAnnotationClass` (session-
> scoped `ConcurrentHashMap<ClassId, Boolean>` cache via
> `JavaModelTypeUseClassIdCache : FirSessionComponent`, registered in
> `JavaClassFinderOverAstImpl.init`). The full java-direct suite passed
> 2793/2793 and the PSI regression slice
> (`PhasedJvmDiagnosticLightTreeTestGenerated.*`) showed no new
> failures. See the **Post-cleanup section (2026-05-25)** for the
> refreshed diff regions / floor estimate.

## Baseline

- **Pre-java-direct revision**: `892a89748136` ("[FIR] Remove redundant arguments in constructClassLikeType() calls").
- **HEAD (post-γ-cleanup, 2026-05-25)**: includes D1 (implicit-permits sealed), D2-A (synth-supertype resolution moved into the model), and the TYPE_USE filter relocation. D1 committed at `ae2efba88c50`; D2-A + TYPE_USE cleanup uncommitted.
- **Line count**: pre 284 / HEAD-pre-γ-cleanup 707 / **HEAD-post-γ-cleanup 546** / net vs pre **+262** (was +423 before γ cleanup).

## Diff regions (by line range in pre)

| Region | Pre range | Net delta | Purpose |
|---|---|---|---|
| Imports | 8-24 | +12 | Imports for new helpers |
| `toFirJavaTypeRef` | 64-69 | +18 | TYPE_USE filter wrapper |
| `toConeTypeProjection` attrs | 96-112 | +17 | TYPE_USE filter on annotations |
| `JavaClassifierType` is_raw block | 117-130 | +31 | Raw detection when classifier null |
| `toConeKotlinTypeForFlexibleBound` `JavaClass` branch | 188-235 | +47 | Outer-args recovery, helper extraction, JavaTypeParameter adapter check |
| `null ->` branch | 248-251 | +66 | Full resolution machinery for B/C cases |
| New helpers (5 functions) | (none) | +247 | `resolveTypeName`, `findOuterTypeArgsFromHierarchy`, `findTypeArgsForClassInHierarchy`, `substituteTypeArgs`, `findClassIdByFqNameString` |
| TYPE_USE helpers | (none) | +63 | `isTypeUseAnnotationClass`, `hasTypeUseTarget`, `isTypeUseElement` |

## Sub-block hit table (full java-direct suite, 2793 tests, post-D2-A)

Each instrumented sub-block emitted a `JTC_<name>_HIT` line to `System.err`.
Counts are total hits across all test XMLs (deduplication is per-line in
the table, not per-test, because Gradle fork stderr is per-fork).

| Sub-block | Marker | Hits | Liveness |
|---|---|---:|---|
| TYPE_USE opt-in (`needsTypeUseAnnotationFiltering=true` in `filterTypeUseAnnotationsIfNeeded`) | `JTC_TYPEUSE_OPT_HIT` | **11841** | HOT — live |
| Empty-attrs short-circuit (post-TYPE_USE filter yields empty) | `JTC_EMPTY_ATTRS_HIT` | **2837** | LIVE (real saving) |
| Raw detection on `JavaClassifierType` block when `classifier == null` (`hasTypeParams=true`) | `JTC_RAW_DETECT_HIT` | **0** | **DEAD** |
| Raw-detection outer-args save (`findOuterTypeArgsFromHierarchy != null` inside raw-detection) | `JTC_RAW_OUTER_SAVE_HIT` | **0** | **DEAD** (consequence of `JTC_RAW_DETECT_HIT=0`) |
| Wrong-arity truncation in `buildTypeProjections` | `JTC_TRUNC_HIT` | **4** | near-dead defensive |
| `JavaClass`-branch outer-args recovery (`findOuterTypeArgsFromHierarchy` on missing-tail) | `JTC_JC_OUTER_HIT` | **2** | live, very narrow |
| `JavaTypeParameterWithFirSymbol` shortcut on `JavaTypeParameter ->` branch | `JTC_JTP_FIRSYM_HIT` | **0** | **DEAD in test suite** (instances exist; never reach here) |
| Stack lookup (fallback for `JavaTypeParameter ->` branch) | `JTC_JTP_STACK_HIT` | **47253** | HOT — dominant |
| Null-branch `JavaToKotlinClassMap.mapJavaToKotlin{,IncludingClassMapping}` changes classId | `JTC_NULL_MAP_HIT` | **0** | **DEAD** |
| Null-branch `readOnlyToMutable` changes classId | `JTC_NULL_ROM_HIT` | **0** | **DEAD** |
| Null-branch `findOuterTypeArgsFromHierarchy` returns non-null | `JTC_NULL_OUTER_HIT` | **0** | **DEAD** |
| Null-branch `isRawType=true` | `JTC_NULL_RAW_HIT` | **0** | **DEAD** |
| Null-branch `outerTypeArgs` projection branch taken | `JTC_NULL_PROJ_OUTER` | **0** | **DEAD** |
| Null-branch raw projection branch taken | `JTC_NULL_PROJ_RAW` | **0** | **DEAD** |
| Null-branch `buildTypeProjections(lookupTag)` branch taken | `JTC_NULL_PROJ_BUILD` | **5** | rare, live |
| Null-branch `lowerBound?.typeArguments` branch taken | `JTC_NULL_PROJ_LOWER` | **155** | dominant null-branch projection |

### Key takeaways

1. **The expanded `null ->` branch (lines 352-421) is ~90% dead code.**
   Of seven sub-blocks (`mapJavaToKotlin`, `readOnlyToMutable`,
   `typeParameterSymbols`-driven raw detection, `outerTypeArgs` recovery,
   `isRawType=true`, RAW projection, OUTER projection), **all seven
   fire zero times** in the post-D2-A suite. Only the trivial path
   (`resolveTypeName(qualifiedName)` → `lookupTag.constructClassType(...)`
   with either `buildTypeProjections` or `lowerBound?.typeArguments` as
   args) is live: 5 + 155 = 160 hits.

2. **Raw-type detection in the `JavaClassifierType` block (lines 167-184)
   is fully dead.** The 17-line `else` clause that fires only when
   `classifier == null && typeArguments.isEmpty() && mode != …FIRST_ROUND`
   never produces `hasTypeParams=true` in the suite — `JTC_RAW_DETECT_HIT = 0`.

3. **`JavaTypeParameterWithFirSymbol` shortcut never fires** (0 hits)
   despite `FirBackedJavaTypeParameter` being a real, in-production
   implementer (`FirBackedJavaClassAdapter.kt:185-205`). All 47,253
   `JavaTypeParameter`-branch lookups go through the stack-lookup
   fallback. Either the cross-file-inner-class-with-outer-typeparam
   scenarios are not exercised by `JavaUsingAst*`, or
   `FirBackedJavaTypeParameter` instances reach FIR through a different
   code path. Step 4.5c's `findOuterTypeArgsFromHierarchy` in the
   JavaClass branch (2 hits) handles the cross-file outer-args case;
   the shortcut may be defensive code for a scenario the suite doesn't
   reproduce. Verify against IJ FP / modularized-tests corpora before
   considering deletion.

4. **TYPE_USE filtering opt-in dominates (11,841 hits).** Category γ is
   load-bearing.

5. **`JavaClass`-branch outer-args recovery (2 hits, narrow but live).**
   The missing-tail recovery path
   (`typeArguments.size < tps.size && findOuterTypeArgsFromHierarchy != null`)
   fires twice across 2793 tests. Without the suite test names per hit,
   can't yet identify the exact scenarios — likely
   `BoxJvm$JvmInlineMultiFieldValueClasses$JavaInterop` or the
   `Tests$Multiplatform$DirectJavaActualization` cluster.

## Change categories

### Category α — required by Step 4.5c (`FirBackedJavaClassAdapter`) — empirically refined

These exist because cross-file references in java-direct return a synthetic
adapter from `JavaClassifierTypeOverAst.computeClassifier()` instead of a
fully-shaped structural `JavaClass`. The adapter's outer-class chain
carries placeholder type-parameter wrappers (`FirBackedJavaTypeParameter`)
that don't live in any `MutableJavaTypeParameterStack`. The FIR side
fills the gaps:

| Code | Lines (HEAD) | Empirical hits | Revised verdict |
|---|---|---|---|
| `JavaTypeParameter` branch — `JavaTypeParameterWithFirSymbol` lookup | 339-348 | **0** | **Dead in `JavaUsingAst*` suite.** May still be defensive for IJ FP / modularized scenarios. Static claim "required" was wrong for this corpus. |
| `findOuterTypeArgsFromHierarchy` from `JavaClass` branch (missing-tail recovery) | 288-307 | **2** | Live but narrow; ~1 hit per ~1400 tests. Static claim "required" confirmed. |
| `findOuterTypeArgsFromHierarchy` from `null ->` branch (inherited-inner recovery) | 388-393 | **0** | **Dead.** Static claim was wrong — post-D2-A, no null-classifier reference triggers this. |
| `findOuterTypeArgsFromHierarchy` from raw-type detection (inside `JavaClassifierType` block) | 179-181 | **0** | **Dead.** Static claim was wrong. |
| `findTypeArgsForClassInHierarchy` + `substituteTypeArgs` helpers | 395-446 | indirect — only via the live JavaClass-branch caller | Live transitively (2 hits) |

**Can these go away?**

- `JavaTypeParameterWithFirSymbol` shortcut: zero hits in our suite —
  **deletion candidate** with the caveat that IJ FP / modularized
  scenarios should be probed first. The shortcut was added during Step
  4.5c as defensive code; if `FirBackedJavaTypeParameter` truly never
  flows through `is JavaTypeParameter ->` in any production corpus, it
  can be inlined back to `javaTypeParameterStack[classifier]`.
- `findOuterTypeArgsFromHierarchy` from JavaClass branch (2 hits): keep.
- `findOuterTypeArgsFromHierarchy` calls from `null ->` branch and
  raw-detection block: **deletable post-D2-A** (zero hits). These two
  call sites + the surrounding `outerTypeArgs` plumbing are dead.
- Helpers (`findTypeArgsForClassInHierarchy`, `substituteTypeArgs`): keep
  — still needed for the live JavaClass-branch caller.

> The Step 4.5b prototype regressions (`testJ_k_complex`,
> `testKJKComplexHierarchyWithNested`,
> `testGenericBoundInnerConstructorRef`) flagged the
> outer-args-substitution machinery as required. The current empirical
> data shows that requirement is satisfied entirely by the
> **JavaClass**-branch call site (2 hits) — the other two call sites
> were defensive duplicates.

### Category β — required by null-classifier flow (path B + path C) — empirically refined

Even after D2-A, `JavaClassifierType.classifier` is null for:

- **Path B**: `JavaClassifierTypeOverAst` whose JLS five-step + `tryResolve`
  probe missed (bare class refs like `ArrayDeque`, `List`, `Bar`, etc.;
  type-parameter-shaped names like `T`, `F`, `O` when the resolution context
  wasn't enriched — see `ITERATION_RESULTS.md` 2026-05-24 D2-A entry).
- **Path C**: binary `PlainJavaClassifierType`
  (`compiler/frontend.common.jvm/.../structure/impl/classFiles/Types.kt`)
  whose binary classifier resolver gave up.

The pre-java-direct null branch (4 lines) handled almost-never-reached
cases by building `ClassId.topLevel(FqName(qualifiedName))` blindly. With
B+C live, the branch needs the full machinery from the `JavaClass` branch:

| Code | Lines (HEAD) | Empirical hits | Revised verdict |
|---|---|---|---|
| `resolveTypeName` helper — first arm `(javaType.classifier as? JavaClass)?.classId` | 439 | — (not probed directly; subsumed by null-branch hits) | Live |
| `resolveTypeName` helper — `findClassIdByFqNameString` fallback | 441 | — | Live (used by all 160 null-branch projections + TYPE_USE detection) |
| `resolveTypeName` helper — `ClassId.topLevel(FqName(name))` final fallback | 443 | — | Live (defensive bottom) |
| `findClassIdByFqNameString` | 563-615 | indirect (via `resolveTypeName` and `isTypeUseAnnotationClass`) | Live |
| Null-branch `JavaToKotlinClassMap.mapJavaToKotlin` / `…IncludingClassMapping` | 361-365 | **0** | **Dead** — `JTC_NULL_MAP_HIT = 0`. The resolved classId is never a Java collection mapped to a Kotlin collection on the null path. |
| Null-branch `readOnlyToMutable` | 367-369 | **0** | **Dead** — `JTC_NULL_ROM_HIT = 0`. |
| Null-branch `typeParameterSymbols` lookup | 381-383 | indirect | dead (drives only the dead `outerTypeArgs` and dead `isRawType` paths) |
| Null-branch `outerTypeArgs` recovery | 388-393 | **0** | **Dead** — `JTC_NULL_OUTER_HIT = 0`. |
| Null-branch `isRawType` | 395-396 | **0** | **Dead** — `JTC_NULL_RAW_HIT = 0`. |
| Null-branch `outerTypeArgs` projection case | 399 | **0** | **Dead**. |
| Null-branch raw projection case | 400-411 | **0** | **Dead**. |
| Null-branch `buildTypeProjections(lookupTag)` case | 412 | **5** | live |
| Null-branch `lowerBound?.typeArguments` case | 413 | **155** | dominant null-branch path |
| Raw-type detection in `JavaClassifierType` block (lines 159-184) | 167-183 | **0** (`JTC_RAW_DETECT_HIT = 0`) | **Dead** |

**Revised "can these go away?":**

The expanded null-branch is **overwhelmingly dead post-D2-A**. The
following sub-blocks can be deleted with no behaviour change:

1. **`JavaToKotlinClassMap.mapJavaToKotlin{,IncludingClassMapping}`
   block in null-branch** (lines 361-365): 4 lines. Dead.
2. **`readOnlyToMutable` block in null-branch** (lines 367-369): 3 lines. Dead.
3. **`typeParameterSymbols` load + `outerTypeArgs` + `isRawType` +
   RAW projection case + OUTER projection case** (lines 381-411): ~30
   lines. Dead.
4. **Raw-type detection `else` clause in `JavaClassifierType` block**
   (lines 167-184): 17 lines. Dead.

The minimal live null-branch after deletion:

```kotlin
null -> {
    val classId = resolveTypeName(this.classifierQualifiedName, this, session, mode)
    val lookupTag = classId.toLookupTag()
    val mappedTypeArguments = when {
        lookupTag != lowerBound?.lookupTag && typeArguments.isNotEmpty() -> buildTypeProjections(lookupTag)
        else -> lowerBound?.typeArguments
    }
    lookupTag.constructClassType(
        mappedTypeArguments ?: ConeTypeProjection.EMPTY_ARRAY,
        isMarkedNullable = lowerBound != null,
        attributes
    )
}
```

That's ~10 lines vs the current ~70. Total reduction in category β:
**~57 lines deletable**.

**Path B / path C remain** for the live trivial path. The branch still
fires 160 times per full-suite run — but every fire goes through
`resolveTypeName(...) → constructClassType(...)`, never through the
deletable machinery. The static claim "required for path B/C raw types
+ readOnly mapping + outer-args recovery" was wrong: B/C scenarios in
this corpus never have those properties.

**Caveat — outside-suite scenarios.** The deletable sub-blocks may fire
in IJ FP / modularized corpora that exercise different JDK call sites
(e.g., raw bare `List` refs without imports, where `findClassIdByFqNameString`
resolves to `java.util.List` and the result is read-only-mapped to
`kotlin.collections.List`). The current suite doesn't hit this.
**Validate against `KotlinFullPipelineTestsGenerated` and
`IntelliJFullPipelineTestsGenerated` before deleting.**

### Category γ — TYPE_USE annotation filtering — empirically confirmed

PSI's javac-wrapper pre-filters TYPE_USE annotations at structure-build
time (`TreeBasedAnnotationOwner` knows the target ElementType). Binary
class loading similarly. java-direct's AST classifier cannot — it has no
classloader at parse time and would have to defer to FIR.

| Code | Lines (HEAD) | Empirical hits | Verdict |
|---|---|---|---|
| `filterTypeUseAnnotationsIfNeeded` opt-in fire | 80-89 | **11841** | HOT — confirms java-direct's `JavaTypeOverAst` opts in heavily. |
| Empty-attrs short-circuit after TYPE_USE filter | 147-150 | **2837** | Real saving — 24% of TYPE_USE invocations produce empty attribute sets. |
| `isTypeUseAnnotationClass` / `hasTypeUseTarget` / `isTypeUseElement` | 650-707 | indirect (called per annotation FQN inside `filterTypeUseAnnotations`) | Live |

**Can these go away?** Only by pre-filtering inside java-direct. Would
require the AST classifier to consult `FirSession.symbolProvider` per
annotation at structure build time — pushing FIR-side resolution into a
hot path the model deliberately keeps lazy. Not recommended.

### Category δ — refactor / extraction

These are improvements over the pre-java-direct shape that aren't tied
to a specific java-direct requirement:

| Code | Lines (HEAD) | Empirical hits | Verdict |
|---|---|---|---|
| `buildTypeProjections` extraction | 263-279 | indirect (called from JavaClass branch and null-branch BUILD case) | Live |
| Empty-annotations `ConeAttributes.Empty` short-circuit | 138-142 | **2837** | HOT — confirmed real saving (γ above) |
| Wrong-arity truncation in `buildTypeProjections` | 271-273 | **4** | near-dead defensive; 4 hits in 2793 tests |

## Net assessment per category — revised with empirical data

| Category | Lines added | Required after D1 + D2-A (empirical)? | Deletable lines (this corpus) |
|---|---|---|---|
| α (Step 4.5c adapter) | ~120 | **Partially.** JavaClass-branch outer-args recovery + `findOuterTypeArgsFromHierarchy` + 2 helpers (~95 lines) live (2 hits). `JavaTypeParameterWithFirSymbol` shortcut (~2 lines) + null-branch + raw-detection block invocations of `findOuterTypeArgsFromHierarchy` (~3 call-site lines) DEAD in suite. | ~5 lines deletable + condition wider audit |
| β (null-classifier flow) | ~145 | **Mostly dead** in this corpus. Live: `resolveTypeName`, `findClassIdByFqNameString`, trivial null-branch path (~10 lines). Dead: mapJavaToKotlin, readOnlyToMutable, typeParameterSymbols load, outerTypeArgs, isRawType, RAW projection, OUTER projection (~57 lines). | **~57 lines deletable** |
| γ (TYPE_USE filtering) | ~85 | **Yes** — 11,841 opt-in hits, 2,837 empty-attrs shortcuts. | None |
| δ (refactor / extraction) | ~30 | Yes (improvement) | None (wrong-arity truncation is 4-hit defensive, keep) |
| New imports | ~12 | **Yes** | None |
| TYPE_USE detection helpers | ~63 | **Yes** — used by `filterTypeUseAnnotationsIfNeeded` callbacks | None |
| `findOuterTypeArgsFromHierarchy` + helpers | ~90 | Live transitively (2 hits via JavaClass branch); other call sites dead | None (helpers shared) |

**Total deletable in this corpus: ~62 lines** (~57 in β + ~5 in α). Cuts
file from 707 to ~645 lines. Floor estimate revised upward from 545 to
~645 due to TYPE_USE filtering being load-bearing (was correctly
identified as load-bearing pre-probe; the +545 estimate had assumed
~85 γ lines were extractable, but only ~63 of them — the helpers — are
file-organisable; the opt-in machinery stays).

## Cleanup opportunities — revised with empirical data

The probe redirects priorities. Earlier "refactor only" opportunities
remain but are dwarfed by the new "delete dead code" opportunities.

### Opportunity D1 — delete the dead null-branch sub-blocks (NEW, high priority)

Sub-blocks empirically dead in the full java-direct suite:

- `JTC_NULL_MAP_HIT = 0` — delete the `JavaToKotlinClassMap.mapJavaToKotlin{,IncludingClassMapping}` block on lines 361-365 (~4 lines).
- `JTC_NULL_ROM_HIT = 0` — delete the `readOnlyToMutable` block on lines 367-369 (~3 lines).
- `JTC_NULL_OUTER_HIT = 0` — delete the `outerTypeArgs` recovery on lines 388-393 (~6 lines).
- `JTC_NULL_RAW_HIT = 0` — delete the `isRawType` computation on lines 395-396 (~2 lines).
- `JTC_NULL_PROJ_OUTER = 0` + `JTC_NULL_PROJ_RAW = 0` — delete the two unreachable `when` arms in `mappedTypeArguments` (lines 399-411, ~13 lines).
- `JTC_NULL_PROJ_BUILD = 5`, `JTC_NULL_PROJ_LOWER = 155` — keep these two arms.
- Also delete the `typeParameterSymbols` lookup (lines 381-383) since its only consumers are dead.

**Total reduction: ~37 lines from the null branch alone.** Minimal live
shape is the 10-line `resolveTypeName(...) → constructClassType(...)`
already shown above.

**Validation required** (before committing): run
`KotlinFullPipelineTestsGenerated` + `IntelliJFullPipelineTestsGenerated`
(or at minimum the modularized-tests subset) with the same probe
instrumentation. If any of `JTC_NULL_MAP_HIT` / `JTC_NULL_ROM_HIT` /
`JTC_NULL_OUTER_HIT` / `JTC_NULL_RAW_HIT` becomes non-zero on those
corpora, the sub-block stays.

### Opportunity D2 — delete the dead raw-detection clause in `JavaClassifierType` block (NEW, high priority)

`JTC_RAW_DETECT_HIT = 0` and `JTC_RAW_OUTER_SAVE_HIT = 0`. The `else`
clause on lines 170-183 (~14 lines including the `resolveTypeName` /
`mappedClassId` / `findOuterTypeArgsFromHierarchy` chain) never produces
`hasTypeParams=true`. Simplify the `isRawType` initializer to just
`isRaw` (lines 167-184 collapse to one line).

**Reduction: ~18 lines.** Same validation caveat as D1.

### Opportunity D3 — `JavaTypeParameterWithFirSymbol` shortcut (NEW, conditional)

`JTC_JTP_FIRSYM_HIT = 0` despite `FirBackedJavaTypeParameter` being a
real implementer. Either:

- Truly dead in production — inline the shortcut: replace
  `(classifier as? JavaTypeParameterWithFirSymbol)?.firTypeParameterSymbol ?: javaTypeParameterStack[classifier]`
  with `javaTypeParameterStack[classifier]`. ~2 lines saved.
- Defensive for IJ FP scenarios — keep.

**Verify against IJ FP corpus** before deleting.

### Opportunity D4 (was Opportunity 1) — fold raw-type detection blocks

Now mostly subsumed by D1 + D2 above. If D1/D2 land, the duplication
disappears entirely (both call sites of raw-type detection are deleted).
**Skip if D1+D2 apply.**

### Opportunity D5 (was Opportunity 2) — TYPE_USE detection extraction

Still applies. Move `isTypeUseAnnotationClass` / `hasTypeUseTarget` /
`isTypeUseElement` to a separate file. `findClassIdByFqNameString` stays
in `JavaTypeConversion.kt` (called from `resolveTypeName`, which is live).
Pure file-organisation change. **~63 lines moved out, not deleted.**

### Opportunity D6 (was Opportunity 3) — fix path B1

Same as before — would shrink the residual ~155 lower-bound null-branch
hits, but doesn't delete code. The null-branch trivial path is needed
for path C (binary `PlainJavaClassifierType`) regardless. Lower priority
than D1 + D2.

## Items that should NOT be reverted — revised

Earlier this section claimed the expanded `null ->` branch was load-bearing
for path B/C raw-type / mapping / outer-args scenarios. **Empirical data
contradicts this** for the `JavaUsingAst*` suite — those sub-blocks are
all dead post-D2-A. The section below remains valid only as a hypothesis
for **non-suite corpora** (IJ FP / KotlinFullPipelineTestsGenerated /
modularized-tests).

If broader-corpus probing confirms the suite findings, the inventory
doc's revert-to-1-line wish is closer to feasibility than thought. The
revised live floor of the null-branch is ~10 lines, not 1 — but down
from 70.

Several reverted-on-paper changes in the inventory doc
(`INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md`) wanted to restore the
pre-java-direct `null ->` branch to its 4-line shape:
> `JavaTypeConversion.resolveTypeName` reverts to its pre-`java-direct`
> body (`(javaType.classifier as? JavaClass)?.classId ?: findClassIdByFqNameString(name, session) ?: ClassId.topLevel(FqName(name))`).

That doc was written *assuming* `JavaClassifierTypeOverAst.classifier`
would be reliably non-null for every cross-file reference (Step 4.5b
prototype). Empirically D2-A leaves ~178 hits per full-suite run, of
which ~50 are binary-classpath `PlainJavaClassifierType` (path C, not
java-direct's fault). The expanded null branch is the production code
path for those — reverting it would regress raw-type detection for
binary references with type parameters.

## Recommended cleanup sequence — revised

1. **Broader-corpus validation** — re-run the same sub-block probe
   against `KotlinFullPipelineTestsGenerated` (or the SAME_THREAD
   subset) and at least one `IntelliJFullPipelineTestsGenerated`
   slice. If `JTC_NULL_MAP_HIT` / `JTC_NULL_ROM_HIT` / `JTC_NULL_OUTER_HIT`
   / `JTC_NULL_RAW_HIT` / `JTC_RAW_DETECT_HIT` / `JTC_JTP_FIRSYM_HIT`
   all stay zero, the dead-code claim generalises. **Required before D1/D2/D3.**
2. **D1 — delete dead null-branch sub-blocks** (~37 lines).
3. **D2 — delete dead raw-detection clause in `JavaClassifierType`
   block** (~18 lines).
4. **D3 — `JavaTypeParameterWithFirSymbol` shortcut**: inline if
   broader-corpus probe confirms 0 hits; ~2 lines saved.
5. **D5 — TYPE_USE detection helpers extraction** (file split, not deletion).
6. **D6 — B1 path investigation** (lowest priority; doesn't delete code).

## What stays — revised floor estimate

After D1 + D2 + D3 (+ D5 if pursued), `JavaTypeConversion.kt` retains:

- Category α (~115 lines — minus 5 for D3 + null-branch outer-args call site) — Step 4.5c outer-args recovery via `findOuterTypeArgsFromHierarchy` + helpers, JavaClass-branch missing-tail recovery.
- Category β (~85 lines — minus ~60 dead) — `resolveTypeName`, `findClassIdByFqNameString`, trivial null-branch path, raw-type detection's `isRaw` short-circuit.
- Category γ (~85 lines) — TYPE_USE filtering (untouched).
- Category δ (~30 lines) — refactor extractions.
- Original PSI-era code (~280 lines) — unchanged shape.

Total floor ≈ **~645 lines** (was 707). Reduction: ~62 lines, ~9% of file.

If D5 file-split is also applied: JavaTypeConversion.kt ≈ **~582 lines**,
TYPE_USE helpers ≈ 63 lines in a new file.

Net debt vs pre-java-direct 284: still ~+300-360 intrinsic to
multi-classifier support. Empirically lower than the original ~545
floor estimate would have implied (which over-counted dead null-branch
code as required).

## Probe methodology (for reproduction)

Two probe passes were used:

**Pass 1 (top-level):** Earlier in 2026-05-24, one
`System.err.println("JTC2_NULL_BRANCH_HIT: …")` at the start of the
`null ->` branch (~line 354), to characterise the classifier-type
distribution of null-branch entrants. Documented in
`ITERATION_RESULTS.md` D2-A entry.

**Pass 2 (sub-blocks, this analysis):** 16 distinct markers inserted at
representative points in `JavaTypeConversion.kt`:

| Marker | Location | Condition |
|---|---|---|
| `JTC_TYPEUSE_OPT_HIT` | `filterTypeUseAnnotationsIfNeeded` | `ext.needsTypeUseAnnotationFiltering == true` |
| `JTC_EMPTY_ATTRS_HIT` | `toConeTypeProjection` attrs builder | `convertedAnnotations.isEmpty()` |
| `JTC_RAW_DETECT_HIT` | raw-detection inside `JavaClassifierType` block | `hasTypeParams == true` |
| `JTC_RAW_OUTER_SAVE_HIT` | raw-detection outer-args save | `findOuterTypeArgsFromHierarchy != null` |
| `JTC_TRUNC_HIT` | `buildTypeProjections` | `typeArguments.size > tps.size` |
| `JTC_JC_OUTER_HIT` | `JavaClass` branch outer-args recovery | `findOuterTypeArgsFromHierarchy != null` |
| `JTC_JTP_FIRSYM_HIT` | `JavaTypeParameter` branch shortcut | `firTypeParameterSymbol != null` |
| `JTC_JTP_STACK_HIT` | `JavaTypeParameter` branch fallback | `javaTypeParameterStack[classifier] != null` (and shortcut was null) |
| `JTC_NULL_MAP_HIT` | null-branch mapJavaToKotlin | `mapped != null && mapped != classId` |
| `JTC_NULL_ROM_HIT` | null-branch readOnlyToMutable | `classId != beforeROM` |
| `JTC_NULL_OUTER_HIT` | null-branch outer-args | `findOuterTypeArgsFromHierarchy != null` |
| `JTC_NULL_RAW_HIT` | null-branch isRawType | `isRawType == true` |
| `JTC_NULL_PROJ_OUTER` | null-branch projection arm | `outerTypeArgs != null` |
| `JTC_NULL_PROJ_RAW` | null-branch projection arm | `isRawType == true` |
| `JTC_NULL_PROJ_BUILD` | null-branch projection arm | `lookupTag != lowerBound?.lookupTag && typeArguments.isNotEmpty()` |
| `JTC_NULL_PROJ_LOWER` | null-branch projection arm | else (`lowerBound?.typeArguments`) |

Test command:

```bash
./gradlew :compiler:java-direct:test --tests "JavaUsingAstPhasedTestGenerated" --tests "JavaUsingAstBoxTestGenerated" --stacktrace 2>&1 | tee "$JD_TMP/jtc_subprobe.txt"
```

Aggregation:

```bash
for marker in JTC_TYPEUSE_OPT_HIT JTC_EMPTY_ATTRS_HIT JTC_RAW_DETECT_HIT …; do
  count=$(grep -h "$marker" .../build/test-results/test/*.xml | wc -l)
  echo "  $marker: $count"
done
```

All probes reverted clean (`git diff` empty for `JavaTypeConversion.kt`).
Suite was green (`BUILD SUCCESSFUL in 44s`, 0 failures) with all 16
markers active — sub-block instrumentation does not perturb semantics.

## Cross-reference

- `INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md` §3 Step 4.5b — the
  rollback plan that wanted to delete much of this. Empirically the
  delete-on-paper was incomplete; B + C survive.
- `FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` §11-12 — context for
  `LazySessionAccess` and `FirBackedJavaClassAdapter`.
- `ITERATION_RESULTS_2026_05_11.md:2113-2178` — reverted-prototype
  findings that established Step 4.5c's outer-args requirement.
- `ITERATION_RESULTS.md` 2026-05-24 (D2-A entry) — empirical traffic
  numbers used in this analysis.

---

## Critical analysis (2026-05-25) — can category γ go away?

> **Trigger.** The doc above (Category γ "Can these go away?" paragraph
> + Opportunity D5) takes for granted the verdict in
> `TYPE_USE_ANNOTATION_HANDLING_2026_05_04.md` §5 that pre-filtering
> TYPE_USE annotations inside `JavaTypeOverAst` is "not a localised
> cleanup" because it would require breaching **Architecture Decision
> #1** (no `FirSession` in the Java Model). This section re-examines
> that verdict against the post-D2-A code shape and proposes a fix that
> removes the FIR-side helpers entirely.

### What the existing docs claim

1. `JTC_CLEANUP_2026_05_24.md` Category γ — *"Can these go away? Only by
   pre-filtering inside java-direct. Would require the AST classifier to
   consult `FirSession.symbolProvider` per annotation at structure build
   time — pushing FIR-side resolution into a hot path the model
   deliberately keeps lazy. Not recommended."*
2. `TYPE_USE_ANNOTATION_HANDLING_2026_05_04.md` §4(1) and §5 —
   *"Architecture Decision #1 (`ARCHITECTURE.md` line 11): Java Model
   provides names, FIR resolves them via `session.symbolProvider`. **No
   `FirSession` access in Java Model**. The predicate cannot move into
   the model layer without breaching this rule. The model layer is in
   `core/compiler.common.jvm`, has zero FIR dependencies …"*
3. §5 lists three "alternatives that look attractive but actually
   regress something", including *"Push the predicate into
   `JavaResolutionContext`. `JavaResolutionContext` is intentionally
   FIR-agnostic …"*

### Both load-bearing premises are stale

The **scope** of Architecture Decision #1 is the public Java-model
interface surface in `core/compiler.common.jvm/src/.../load/java/structure/`
— the `JavaType`, `JavaClass`, `JavaAnnotation`, … interfaces that K1
descriptors and other clients share. That module still has zero FIR
dependencies; that part of the rule is unchanged.

The **java-direct implementations** of those interfaces are in
`compiler/java-direct/.../model/` and use a `JavaResolutionContext`
that is **already coupled to `FirSession`** end-to-end:

- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/resolution/CompilationUnitContext.kt:21`
  declares `val session: FirSession` as a constructor parameter.
- `JavaResolutionContext.tryResolve(classId)` calls
  `unitContext.session.cycleSafeTryResolveClass(classId)`
  (`JavaResolutionContext.kt:144-145`).
- `JavaResolutionContext.directSupertypeClassIds(classId)` calls
  `unitContext.session.cycleSafeClassLikeSymbol(classId)`
  (`JavaResolutionContext.kt:187`).
- `JavaResolutionContext.classifierAdapterFor` constructs a
  `FirBackedJavaClassAdapter(classId, session)`
  (`JavaResolutionContext.kt:153-156`).
- `FirBackedJavaClassAdapter` itself stores a `private val session:
  FirSession` and reads `firRegularClass` via
  `session.cycleSafeClassLikeSymbol(resolvedClassId)`
  (`FirBackedJavaClassAdapter.kt:43-71`).

`JavaAnnotationOverAst.classId` (the value the FIR-side predicate keys
off) is already computed by routing through
`resolutionContext.resolve(reference)`
(`JavaAnnotationOverAst.kt:62`), which fans into
`session.cycleSafeTryResolveClass` for every probe. The "AST classifier
would have to consult `FirSession.symbolProvider` per annotation at
structure build time" cost is **already paid** — once, per annotation
occurrence, when its `classId` is computed.

So claim #2 of the existing docs is no longer true for the java-direct
half of the world: a `JavaResolutionContext` *is* `FirSession`-aware,
and has been since `LazySessionAccess` / Step 4.5a landed. Claim #1
inherits the same defect (it merely cites the same Decision #1).

The remaining objection in §5 — *"Pre-filter at structure-build time
inside `JavaTypeOverAst` … would require resolving annotation classes
during model construction, which means option (a) or (b)"* — also no
longer applies. `JavaTypeOverAst.annotations` does not have to filter
at construction time; it can filter at first read, which is the same
laziness boundary the FIR-side `filterTypeUseAnnotationsIfNeeded` lives
behind today. (FIR's `toFirJavaTypeRef` defers the filter through an
`annotationBuilder = { … }` lambda; `toConeTypeProjection` fires the
filter only when `annotations.isNotEmpty() || additionalAnnotations !=
null`.)

### Verifying the user-stated assumption empirically

> *"External annotations should already be resolved at the moment when
> the TYPE_USE flag is required."*

By construction, **yes**:

1. `JavaTypeOverAst.filterTypeUseAnnotations` is invoked from
   `JavaTypeConversion.kt:81` and `:130`. Both call sites are inside
   the FIR-side conversion of a Java type — which only fires after the
   model has produced the `JavaType`, which in turn only happens after
   `JavaAnnotationOverAst.classId` resolved every annotation it owns
   through `JavaResolutionContext.resolve()`.
2. `JavaResolutionContext.resolve()` is `cycleSafeTryResolveClass`
   followed by `cycleSafeClassLikeSymbol` — the latter is the same
   symbol-provider call the FIR-side `isTypeUseAnnotationClass` makes
   to read `@Target`. If the annotation classId is resolvable at all,
   its `FirRegularClass` is reachable through the same session at the
   same point in time.
3. The FIR-side `isTypeUseAnnotationClass` reads
   `annotationClass.annotations` without `lazyResolveToPhase` —
   evidence that whatever phase invariant makes this work today applies
   uniformly regardless of which module the call sits in. The phase
   guarantee is established by the caller path
   (`FirSignatureEnhancement` → `toFirJavaTypeRef` → annotationBuilder),
   not by the filter's location.

So the assumption holds in the strong sense the user described it.

### What the fix removes from `JavaTypeConversion.kt`

If filtering moves into `JavaTypeOverAst.annotations` (driven by
`JavaResolutionContext`), the following pieces of `JavaTypeConversion.kt`
become dead and deletable:

| Code | Lines (HEAD) | Why deletable |
|---|---|---|
| `toFirJavaTypeRef`'s `filterTypeUseAnnotationsIfNeeded(session)` call | 80-83 | `type.annotations` is already TYPE_USE-filtered |
| `toConeTypeProjection`'s `val typeUseAnnotations = filterTypeUseAnnotationsIfNeeded(session)` and the `additionalTypeUseAnnotations` filter | 129-145 | annotations + RxJava3 additional annotations are already TYPE_USE |
| `filterTypeUseAnnotationsIfNeeded` helper | 88-100 | unused |
| `isTypeUseAnnotationClass` | 579-597 | unused — folded into the model side |
| `hasTypeUseTarget` | 599-614 | unused |
| `isTypeUseElement` | 616-636 | unused |

Total: ≈ **85 lines** removed from `JavaTypeConversion.kt` (Category γ
in the table above), plus the `JavaTypeWithExternalAnnotationFiltering`
interface in `JavaModelExtensions.kt:17-36` (≈20 lines) — net ≈100 LoC
deleted from the FIR-jvm module.

The `additionalTypeUseAnnotations` simplification is independently
load-bearing: today's filter on
`additionalAnnotations.filter { isTypeUseAnnotationClass(...) }` is
defensive against a non-TYPE_USE annotation slipping in, but the only
caller is
`extractNullabilityAnnotationOnBoundedWildcard` (`JavaUtils.kt:142`),
which already restricts the result to `RXJAVA3_ANNOTATIONS` —
`io.reactivex.rxjava3.annotations.Nullable` and `.NonNull`, both of
which carry `@Target(TYPE_USE)`. The filter cannot drop anything in
practice.

### What the fix adds on the java-direct side

A single member on `JavaResolutionContext` that owns the predicate
(plus a session-scoped cache that section 6 of
`TYPE_USE_ANNOTATION_HANDLING_2026_05_04.md` already advocates):

```kotlin
// JavaResolutionContext.kt
fun isTypeUseAnnotationClass(classId: ClassId): Boolean =
    unitContext.session.typeUseClassIdCache.computeIfAbsent(classId) {
        unitContext.session.computeIsTypeUseAnnotationClass(classId)
    }
```

`computeIsTypeUseAnnotationClass` is `isTypeUseAnnotationClass` /
`hasTypeUseTarget` / `isTypeUseElement` from `JavaTypeConversion.kt`,
relocated and refactored to take a `ClassId` rather than an FQN string
(the existing FQN→`ClassId` re-probe via `findClassIdByFqNameString`
becomes unnecessary because the model already has the `ClassId` from
`JavaAnnotationOverAst.classId`). The cross-package guard
(`symbol.classId != classId` defending against PSI's simple-name match
— see §4(5) of the design doc) also drops because we no longer
round-trip through an FQN.

`typeUseClassIdCache` is a `ConcurrentHashMap<ClassId, Boolean>`
hung off the `FirSession` as a `FirSessionComponent`. The cache makes
the `getClassLikeSymbolByClassId(classId)` call fire **once per
distinct annotation class**, rather than once per occurrence — which
in `KotlinFullPipelineTestsGenerated` reduces a
per-`@NotNull`-occurrence symbol lookup to ~one symbol lookup total
for the entire build. This directly addresses the perf regression that
motivated the `needsTypeUseAnnotationFiltering` gate
(`ITERATION_RESULTS.md` lines 565-700): the gate exists to avoid a
per-type-ref closure allocation; the cache makes that closure cheap
enough that the gate's purpose disappears.

`JavaTypeOverAst.annotations` becomes:

```kotlin
override val annotations: Collection<JavaAnnotation>
    get() = filteredMemberAnnotations + typePositionAnnotations

private val filteredMemberAnnotations: Collection<JavaAnnotation> by
    lazy(LazyThreadSafetyMode.PUBLICATION) {
        memberAnnotations.filter { annotation ->
            val classId = annotation.classId ?: return@filter false
            resolutionContext.isTypeUseAnnotationClass(classId)
        }
    }
```

The `JavaTypeWithExternalAnnotationFiltering` interface, the
`needsTypeUseAnnotationFiltering` gate, and the
`filterTypeUseAnnotations(isTypeUse)` callback all evaporate.
PSI/javac-wrapper `JavaType` impls (which pre-filter at structure-build
time inside their own machinery) continue to expose the
already-pre-filtered set via the inherited default
`JavaType.annotations` — no behavioural change there, no double-filter
risk (since the filter only runs inside `JavaTypeOverAst`).

### Where does it leave the Iter 22 regression that originally forbade this?

`TYPE_USE_ANNOTATION_HANDLING_2026_05_04.md` §3(2) cites the failed
"Iter 22" attempt: *"Adding filtering directly in shared FIR code
(`JavaTypeConversion.kt`) caused regressions in PSI-based tests
because: javac-wrapper already filters at Java structure level; adding
FIR-level filtering double-filtered and broke expected behavior."*

Iter 22's attempt added filtering inside the **shared FIR path** —
i.e. on the `JavaType` *value*, regardless of provenance. The proposal
here is the opposite: filter inside the **java-direct-specific impl**,
so PSI/javac-wrapper `JavaType` values reach FIR pre-filtered (as
they do today) and java-direct `JavaType` values reach FIR also
pre-filtered (the new shape). No FIR-side re-filter exists, so no
double-filter is structurally possible.

The current opt-in interface
(`JavaTypeWithExternalAnnotationFiltering`) was the previous attempt
at the same correctness invariant; the proposed fix retires that
opt-in by collapsing the two filter sites into one (the java-direct
impl's `annotations` getter). The PSI hot path remains filter-free at
the FIR layer for the same reason today's
`needsTypeUseAnnotationFiltering = false` keeps it so.

### Perf gate (`needsTypeUseAnnotationFiltering`) — does it survive?

It is **safe to delete** under the proposed change, because:

1. The gate's purpose is to avoid a per-type-ref closure allocation on
   the PSI hot path (`ITERATION_RESULTS.md:565-700`). With filtering
   moved into `JavaTypeOverAst.annotations`, **no closure exists on any
   PSI code path** — `JavaTypeConversion.kt:81/:130` simply reads
   `type.annotations`, which for PSI's `JavaTypeImpl` is the legacy
   in-place getter and for java-direct's `JavaTypeOverAst` is the new
   lazy-filtered getter. There is nothing for the gate to gate.
2. The closure-cost evidence is from a *call site* that ceases to
   exist. Re-measuring after the change would be on a different code
   shape; the original regression cannot re-fire because the original
   code is gone.

A small follow-up measurement on
`KotlinFullPipelineTestsGenerated` is still warranted to confirm the
**cache** keeps the per-occurrence symbol lookup amortised — but it is
qualitatively the same scenario the cache was designed for.

### Proposed change set

(All deltas measured against HEAD as of 2026-05-25.)

1. **`compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt`** — delete
   `filterTypeUseAnnotationsIfNeeded`, `isTypeUseAnnotationClass`,
   `hasTypeUseTarget`, `isTypeUseElement`; simplify
   `toFirJavaTypeRef` to `annotationBuilder = { type.annotations.convertAnnotationsToFir(...) }`;
   simplify `toConeTypeProjection`'s attribute construction to drop
   the `typeUseAnnotations` and `additionalTypeUseAnnotations` locals.
   Estimated reduction: **~85 lines**.

2. **`compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaModelExtensions.kt`** — delete
   the `JavaTypeWithExternalAnnotationFiltering` interface.
   Estimated reduction: **~20 lines**.

3. **`compiler/java-direct/src/org/jetbrains/kotlin/java/direct/model/JavaTypeOverAst.kt`** — drop
   the `JavaTypeWithExternalAnnotationFiltering` supertype, drop
   `needsTypeUseAnnotationFiltering`, replace
   `filterTypeUseAnnotations` with the `annotations` getter shape
   shown above. Net delta around +5 lines (cache-aware filter is
   slightly longer than the old callback recipient).

4. **`compiler/java-direct/src/org/jetbrains/kotlin/java/direct/resolution/JavaResolutionContext.kt`** — add
   `isTypeUseAnnotationClass(classId: ClassId): Boolean` and the
   private `computeIsTypeUseAnnotationClass(...)` helper (≈30 lines
   relocated from `JavaTypeConversion.kt`). Plus a small
   `FirSessionComponent` holding the
   `ConcurrentHashMap<ClassId, Boolean>` cache (≈10 lines).

5. **No change to `core/compiler.common.jvm/.../structure/*`** — the
   shared interface surface is untouched, honouring rule 7 in
   `AGENT_INSTRUCTIONS.md` (no new public Java-model interface
   members). `JavaTypeWithExternalAnnotationFiltering` lived in
   `compiler/fir/fir-jvm/...`, so its deletion is also internal.

Net file-size effects on the FIR-jvm module: ≈**−100 LoC**.
Net file-size effects on the java-direct module: ≈**+45 LoC**.
**Net codebase delta: ≈−55 LoC**, plus the conceptual win of removing
one model→FIR callback that the design doc lists alongside three
others as the canonical pattern.

### Validation plan

The behavioural change is "filtering site moves from FIR to
java-direct". Behaviour should be identical, so:

1. Full `:compiler:java-direct:test` suite must stay at 2793/2793
   pass (same as Category γ baseline).
2. `:compiler:fir:analysis-tests:test --tests
   "PhasedJvmDiagnosticLightTreeTestGenerated.*"` (the PSI regression
   slice) must show no new failures — proves the
   `JavaTypeWithExternalAnnotationFiltering` deletion does not break
   the PSI default path (the corresponding code path is "read
   `type.annotations` directly", unchanged in behaviour).
3. `KotlinFullPipelineTestsGenerated` (perf-canary) — confirm the
   `ConcurrentHashMap` cache amortises the per-occurrence symbol
   lookups, so the ≈+5% / +11s regression that motivated the
   `needsTypeUseAnnotationFiltering` gate does not re-fire under the
   new shape. If it does, the cache implementation is the part to
   fix, not the architecture.

### Bottom line

The user's hypothesis is correct as stated: by the time TYPE_USE
filtering is required, the annotation `ClassId` has already been
resolved through the very `FirSession` we would need to consult to
read `@Target`, and the `JavaResolutionContext` carrying that session
is the natural owner of the predicate. The doc-level argument that
prevented the move — Architecture Decision #1 — applies to the
**shared model interface in `core/compiler.common.jvm`**, not to
java-direct's `JavaTypeOverAst` implementation. The proposed change
removes ~100 LoC from `JavaTypeConversion.kt` + `JavaModelExtensions.kt`
without any hard-coded annotation-name list, without breaching the
shared-interface rule, and without re-introducing the Iter 22
double-filter failure mode (the filter is *moved*, not *added on top
of* PSI's pre-filter).

---

## Post-cleanup section (2026-05-25)

> The proposal in the "Critical analysis (2026-05-25)" section above
> was implemented in a single uncommitted patch. This section refreshes
> the diff regions table, the per-category assessment, and the floor
> estimate against the new file shape.

### What was changed (concrete patch)

| Touched file | Δ vs pre-γ-cleanup HEAD | Purpose |
|---|---|---|
| `compiler/fir/fir-jvm/src/.../JavaTypeConversion.kt` | **−161** lines (707 → 546) | Removed γ helpers, simplified `toFirJavaTypeRef` + `toConeTypeProjection` annotation assembly. |
| `compiler/fir/fir-jvm/src/.../JavaModelExtensions.kt` | −16 lines | Deleted `JavaTypeWithExternalAnnotationFiltering` interface. File kept (two other interfaces still in use — see §"What did *not* go away" below). |
| `compiler/java-direct/src/.../resolution/JavaModelSessionAccess.kt` | +96 lines | New `JavaModelTypeUseClassIdCache : FirSessionComponent`, `registerJavaModelTypeUseCacheIfAbsent`, and `FirSession.isTypeUseAnnotationClass` / `computeIsTypeUseAnnotationClass` / `hasTypeUseTarget` / `isTypeUseElement` (relocated from `JavaTypeConversion.kt`, `ClassId`-keyed instead of FQN-string-keyed; cross-package guard simplified). |
| `compiler/java-direct/src/.../resolution/JavaResolutionContext.kt` | +12 lines | Thin `isTypeUseAnnotationClass(ClassId): Boolean` wrapper delegating to the session helper. |
| `compiler/java-direct/src/.../model/JavaTypeOverAst.kt` | +19/−14 lines | `annotations` getter now returns `filteredMemberAnnotations + typePositionAnnotations`; lazy filter via `resolutionContext.isTypeUseAnnotationClass(classId)`. Dropped `JavaTypeWithExternalAnnotationFiltering` supertype, `needsTypeUseAnnotationFiltering`, the old `filterTypeUseAnnotations` callback. |
| `compiler/java-direct/src/.../JavaClassFinderOverAstImpl.kt` | +5 lines | `session.registerJavaModelTypeUseCacheIfAbsent()` next to the existing in-flight-resolution registration. |
| `compiler/java-direct/test/.../JavaParsingAnnotationsTest.kt` | −20/+5 lines | Dropped the two test snippets that exercised the retired callback (the type-position annotation presence checks above them stay; that path still works without the callback). |

Net codebase delta after cleanup: ≈ **−74 LoC**, distributed as
−161 (`JavaTypeConversion.kt`) + −16 (`JavaModelExtensions.kt`) +
−15 (test) + +96 (session helper) + +12 (resolution-ctx wrapper) +
+5 (finder init) + +5 (`JavaTypeOverAst` net).

The 161-line drop on `JavaTypeConversion.kt` exceeded the ≈85 estimate
in the "Proposed change set" subsection above because the dropped
helpers brought 8 now-unused FIR-expression imports with them
(`FirAnnotation`, `FirCollectionLiteral`, `FirEnumEntryDeserializedAccessExpression`,
`FirExpression`, `FirPropertyAccessExpression`, `FirVarargArgumentsExpression`,
`FirResolvedNamedReference`, `JvmStandardClassIds`) and the
`toConeTypeProjection` attribute builder collapsed by more than
expected once the `typeUseAnnotations` / `additionalTypeUseAnnotations`
locals were removed.

### Test outcome

| Suite | Tests | Result |
|---|---|---|
| `:compiler:java-direct:test --tests "JavaUsingAst{Phased,Box}TestGenerated"` | 2793 | `BUILD SUCCESSFUL in 42s`, 0 failures |
| `:compiler:fir:analysis-tests:test --tests "PhasedJvmDiagnosticLightTreeTestGenerated.*"` (PSI regression slice, `--rerun`) | (full slice) | `BUILD SUCCESSFUL in 1m 16s`, 0 failures |
| `:compiler:java-direct:test --tests "JavaParsingTest"` (covered by the suite above) | included | green |

Perf canary against `KotlinFullPipelineTestsGenerated` was not yet
run; the architectural argument (the per-`ClassId` cache amortises
what used to be a per-occurrence closure) is exercised by the cache's
unit-level correctness on `:compiler:java-direct:test`, but the +5 %
regression on the multi-module corpus that motivated the original
`needsTypeUseAnnotationFiltering` gate should be re-measured against
the post-cleanup shape before claiming the gate's deletion is
perf-neutral. This is a follow-up item, not a blocker.

### Refreshed diff regions (by line range in pre, against post-cleanup HEAD)

| Region | Pre range | Pre-cleanup Δ | Post-cleanup Δ | Status |
|---|---|---:|---:|---|
| Imports | 8-24 | +12 | **+7** | Dropped 5 FIR-expression/StandardClassIds-package imports |
| `toFirJavaTypeRef` | 64-69 | +18 | **+1** | `annotationBuilder` reads `annotations` directly; no closure, no helper call |
| `toConeTypeProjection` attrs | 96-112 | +17 | **+12** | Removed `typeUseAnnotations` + `additionalTypeUseAnnotations` locals; kept the empty-attrs short-circuit (~2837 hits — still useful) |
| `JavaClassifierType` is_raw block | 117-130 | +31 | +31 | Unchanged; D1/D2 deletions in this region still pending broader-corpus validation |
| `toConeKotlinTypeForFlexibleBound` JavaClass branch | 188-235 | +47 | +47 | Unchanged |
| `null ->` branch | 248-251 | +66 | +66 | Unchanged; D1 deletions still pending broader-corpus validation |
| New helpers (5 functions) | (none) | +247 | +247 | Unchanged: `resolveTypeName`, `findOuterTypeArgsFromHierarchy`, `findTypeArgsForClassInHierarchy`, `substituteTypeArgs`, `findClassIdByFqNameString` |
| TYPE_USE helpers | (none) | +63 | **0** (gone) | Relocated to `JavaModelSessionAccess.kt` as `computeIsTypeUseAnnotationClass` / `hasTypeUseTarget` / `isTypeUseElement`; `filterTypeUseAnnotationsIfNeeded` deleted outright |

Sum of post-cleanup Δs: 7 + 1 + 12 + 31 + 47 + 66 + 247 + 0 = **+411**.
Combined with the original pre-java-direct body (284 lines), this gives
≈546 lines, matching the measured HEAD length and confirming the table.

### Refreshed category assessment

| Category | Was (pre-γ cleanup) | Now (post-γ cleanup) | Notes |
|---|---|---|---|
| α (Step 4.5c adapter) | ~120 lines | ~120 lines | Unchanged: JavaClass-branch outer-args recovery + helpers. D3 (`JavaTypeParameterWithFirSymbol` shortcut inline) still pending broader-corpus validation. |
| β (null-classifier flow) | ~145 lines | ~145 lines | Unchanged. D1 (delete dead null-branch sub-blocks, ~57 lines) still pending broader-corpus validation. |
| γ (TYPE_USE filtering) | ~85 lines | **0 lines in `JavaTypeConversion.kt`** | Relocated to `JavaModelSessionAccess.kt` (~85 lines on java-direct side, plus the cache machinery). Net codebase reduction ≈ 74 LoC. |
| δ (refactor / extraction) | ~30 lines | ~30 lines | Unchanged (`buildTypeProjections` extraction, empty-attrs short-circuit). |
| New imports | ~12 lines | ~7 lines | 5 imports dropped with γ helpers. |
| TYPE_USE detection helpers | ~63 lines | **0 lines in `JavaTypeConversion.kt`** | Subsumed into γ above. |
| `findOuterTypeArgsFromHierarchy` + helpers | ~90 lines | ~90 lines | Unchanged. |

### Refreshed floor estimate

After the γ relocation:

- `JavaTypeConversion.kt`: **546 lines** (was 707; pre-java-direct 284).
- Net java-direct overhead vs pre: **+262 lines** in `JavaTypeConversion.kt` alone (was +423).

If the still-pending D1 + D2 + D3 deletions land (broader-corpus
validation required first — `KotlinFullPipelineTestsGenerated` +
`IntelliJFullPipelineTestsGenerated`):

- D1 — ~37 lines off the null-branch.
- D2 — ~18 lines off the raw-detection clause.
- D3 — ~2 lines off the `JavaTypeParameterWithFirSymbol` shortcut.

Projected `JavaTypeConversion.kt` floor: **~489 lines** (≈+205 vs
pre-java-direct). With D5 (TYPE_USE helpers file split) no longer
applicable — those helpers now live in the java-direct module by
default — the floor estimate stops dropping below 489 unless D6
(path-B1 fix) is pursued, which only trims the null-branch hit count
without deleting code.

### What did *not* go away

`JavaModelExtensions.kt` was **not** deleted. Two of its three
interfaces remain in use:

- `JavaFieldWithExternalInitializerResolution` —
  cross-language constant-evaluation callback used by
  `JavaMemberOverAst.JavaFieldOverAst.resolveInitializerValue` and read
  back in `javaAnnotationsMapping.kt`. Retired only when java-direct
  fields stop needing FIR to evaluate Kotlin `const val` references.
- `JavaEnumValueAnnotationArgumentWithConstFallback` —
  enum-vs-const disambiguation callback used by
  `JavaEnumValueAnnotationArgumentOverAst.couldBeConstReference` and
  read by `FirJavaFacade.kt` / `javaAnnotationsMapping.kt`.

A natural follow-up would be to repeat the same critical analysis on
those two callbacks: if the assumption "by the time the callback fires
the referenced FirRegularClass is resolvable through the session"
holds (and `JavaResolutionContext` has the session anyway), both could
move into java-direct on the same pattern as TYPE_USE just did. The
remaining contents of `JavaModelExtensions.kt` would then be zero and
the file could be deleted in full. This is **not** done here; it is
flagged as a candidate for a separate iteration.

### Outstanding items

| Item | Status | Note |
|---|---|---|
| Perf re-measurement against `KotlinFullPipelineTestsGenerated` | Not done | The cache amortises closures away; +5 % regression on the original gate's benchmark should not re-fire. Re-measure before claiming the gate is fully retired. |
| Broader-corpus probe (D1/D2/D3 validation) | Not done | Independent of γ cleanup; same prerequisites as before. |
| `JavaFieldWithExternalInitializerResolution` / `JavaEnumValueAnnotationArgumentWithConstFallback` relocation | Not done | Apply the same critical-analysis lens; if either survives, `JavaModelExtensions.kt` can be deleted entirely. |
| Doc updates in `TYPE_USE_ANNOTATION_HANDLING_2026_05_04.md` | Not done | §3-5 of that doc still treat the FIR-side filter as "load-bearing". Mark obsoleted by this iteration. |

### Bottom line (refreshed)

The "TYPE_USE handling cannot be cleaned up" verdict in
`TYPE_USE_ANNOTATION_HANDLING_2026_05_04.md` §5 is **empirically
falsified**. The user's hypothesis — *external annotations are
already resolved at the moment the TYPE_USE flag is required* — held
exactly as stated, and the resulting cleanup removed 161 lines from
`JavaTypeConversion.kt` (≈23 % of the file) without test regressions,
without hard-coded annotation names, and without breaching the shared
Java-model interface rule. The java-direct module absorbed a +96/+12/+5
line trio plus a small `JavaTypeOverAst` net delta in exchange. The
filter still consults `FirSession.symbolProvider` once per distinct
annotation `ClassId` (now session-scoped cached), so the "delegation
to FIR" the docs warned about is reduced rather than eliminated — but
the *call site* is now where the rest of the java-direct cross-file
resolution lives, and the FIR-jvm module no longer carries
java-direct-specific TYPE_USE machinery.

---

## Follow-up section (2026-05-25): `JavaModelExtensions.kt` retired entirely

The "What did *not* go away" subsection above noted that
`JavaModelExtensions.kt` still held two callback interfaces —
`JavaFieldWithExternalInitializerResolution` and
`JavaEnumValueAnnotationArgumentWithConstFallback` — and flagged the
file as a candidate for a next-iteration cleanup with the same
critical-analysis lens. That follow-up was applied on the same day;
the file is **deleted** and both callbacks have been replaced by
direct calls into a new java-direct-side resolver
(`compiler/java-direct/.../resolution/JavaExternalConstResolver.kt`,
185 lines).

### Critical analysis recap

Both callbacks share the structural shape that made TYPE_USE
removable: the value the callback delegates the resolution of (the
qualified-name target, in both cases) is already known to the java-
direct model at the point the callback fires, and the FIR-side
implementation of the callback only needs `FirSession.symbolProvider`,
which `JavaResolutionContext` carries unconditionally.

- `JavaFieldWithExternalInitializerResolution.resolveInitializerValue`
  was a callback whose body the FIR side filled with
  `resolveExternalFieldValue(session, classQualifier, fieldName, packageFqName)`.
  All four arguments are either known to the java-direct model
  (`classQualifier` / `fieldName` are produced by `ConstantEvaluator`
  itself, `packageFqName` is `JavaResolutionContext.packageFqName`) or
  are reachable through it (`session`). The two-step
  `value ?: callback`-fallback shape inside FIR's `lazyInitializer`
  was a workaround for the model not having the session, not a
  semantic requirement.

- `JavaEnumValueAnnotationArgumentWithConstFallback.couldBeConstReference`
  gated a FIR-side `resolveConstFieldValue(session, classId, fieldName)`
  call on `JavaEnumValueAnnotationArgument`. `classId` came from
  `JavaEnumValueAnnotationArgument.enumClassId` (model-side), `fieldName`
  from `entryName` (model-side). Again the FIR-side implementation
  used `session.symbolProvider` exclusively. The model could have
  produced a `JavaLiteralAnnotationArgument` directly if it had been
  given the session — which Step 4.5a's `LazySessionAccess` already
  did, but the callback bridge was retained because the design doc
  treated the model layer as session-agnostic.

Both rationales mirror TYPE_USE: the FIR-side helpers are the
implementation, not the predicate. Moving them sits naturally where
`JavaAnnotationOverAst.classId` and friends already call
`cycleSafeClassLikeSymbol` / `cycleSafeTryResolveClass`.

### What the fix removes

| File | Removed |
|---|---|
| `compiler/fir/fir-jvm/src/.../FirJavaFacade.kt` | `resolveExternalFieldValue`, `tryResolveAsTopLevel`, `tryResolveAsClassMember`, `tryResolveAsCompanionMember` + the `?: callback` fallback in `lazyInitializer`. Net: ≈73 lines. |
| `compiler/fir/fir-jvm/src/.../javaAnnotationsMapping.kt` | `resolveConstFieldValue`, `extractEvaluatedConstValue`, `tryExtractConstantValue`, the `JavaEnumValueAnnotationArgumentWithConstFallback`-cast block. Net: ≈75 lines. |
| `compiler/fir/fir-jvm/src/.../JavaModelExtensions.kt` | **File deleted entirely** (73 lines including header). |

### What the fix adds

A single new file on the java-direct side: `JavaExternalConstResolver.kt`
(185 lines). Hosts `FirSession.resolveExternalFieldValue`,
`FirSession.resolveConstFieldValue`, plus the three private helpers
(`tryResolveAsTopLevel`, `tryResolveAsClassMember`,
`tryResolveAsCompanionMember`, `resolveConstPropertyValueInClass`) and
two const-extraction primitives (`tryExtractConstantValue`,
`extractEvaluatedConstValue`) ported verbatim from
`javaAnnotationsMapping.kt`. `resolveConstFieldValue` was reshaped to
go through `FirSession.getClassDeclaredPropertySymbols` instead of
`firClass.declarations` to avoid the `DirectDeclarationsAccess` opt-in
required in fir-jvm's host module (`getClassDeclaredPropertySymbols`
is part of `compiler:fir:providers` and already public).

`JavaResolutionContext` grew two thin wrappers
(`resolveExternalFieldValue(classQualifier, fieldName)` and
`resolveConstFieldValue(classId, fieldName)`, total +21 lines) that
fan into the new file. `JavaFieldOverAst.initializerValue` reads the
external resolver inline (replacing the
`JavaFieldWithExternalInitializerResolution` callback override). In
`JavaAnnotationOverAst.createAnnotationArgumentFromValue` the
`REFERENCE_EXPRESSION` arm now resolves `enumClassId` + `entryName`
through `resolveConstFieldValue`; if a const value comes back it
returns a `JavaLiteralAnnotationArgumentOverAst` instead of the
enum-value shape (matching PSI/javac-wrapper structure-build-time
disambiguation). All three callbacks return `null` cleanly when the
session has no symbol provider (parsing-level fixtures) — every helper
short-circuits on `nullableSymbolProvider == null`.

### Subtle correctness invariants

1. **No-symbol-provider parsing tests.** `createAnnotationArgumentFromValue`
   now invokes `resolutionContext.resolveConstFieldValue(classId, …)`
   on every `REFERENCE_EXPRESSION`. In parsing-only mode (dummy
   `FirSession`, no symbol provider) the call falls all the way
   through to `nullableSymbolProvider?` short-circuits and returns
   `null`, so the model emits `JavaEnumValueAnnotationArgumentOverAst`
   — the same shape as before. The three
   `JavaParsingAnnotationsTest.testEnumValueArgument*` tests
   initially crashed on `symbolProvider` access until the guard was
   added; with the guard, they pass unchanged.

2. **`getClassDeclaredPropertySymbols` vs `firClass.declarations`.**
   The fir-jvm `resolveConstFieldValue` walked
   `firClass.declarations.filterIsInstance<FirProperty>()` directly.
   Java-direct cannot do that without a module-wide
   `DirectDeclarationsAccess` opt-in; using
   `getClassDeclaredPropertySymbols` is the public-API equivalent and
   covers the same set of declared properties (enum-companion const
   props, class-direct const props, companion const props). Tests
   confirm semantic parity.

3. **Const-vs-enum disambiguation timing.** PSI emits `JavaLiteralAnnotationArgument`
   for `KConstsKt.WARNING` at structure-build time; java-direct now
   does the same inside `createAnnotationArgumentFromValue`. The
   resolution fires lazily — `JavaAnnotationOverAst.arguments` is a
   getter, not eagerly populated — so the cost is paid only when an
   annotation argument is consumed, matching the previous FIR-side
   timing (the `JavaEnumValueAnnotationArgumentWithConstFallback`
   cast block also fired at annotation-conversion time).

### Test outcome

| Suite | Result |
|---|---|
| `:compiler:java-direct:test --tests "JavaUsingAst{Phased,Box}TestGenerated"` (2793 tests) | `BUILD SUCCESSFUL`, 0 failures |
| `:compiler:java-direct:test --tests "JavaParsing*Test"` | green; the three `JavaParsingAnnotationsTest.testEnumValueArgument*` tests pass with the `nullableSymbolProvider == null` guard inside the resolver |
| `:compiler:fir:analysis-tests:test --tests "PhasedJvmDiagnosticLightTreeTestGenerated.*" --rerun` | `BUILD SUCCESSFUL in 1m 11s`, 0 failures |

### Final file-size accounting (cumulative across both 2026-05-25 iterations)

| File | Pre-2026-05-25 | Post-2026-05-25 | Δ |
|---|---:|---:|---:|
| `JavaTypeConversion.kt` | 707 | 546 | **−161** |
| `FirJavaFacade.kt` | 838 (approx.) | 771 | **−67** |
| `javaAnnotationsMapping.kt` | 524 (approx.) | 458 | **−66** |
| `JavaModelExtensions.kt` | 73 | **(deleted)** | **−73** |
| `JavaResolutionContext.kt` | 715 (approx.) | 761 | +46 (TYPE_USE wrapper + 2 const-resolver wrappers) |
| `JavaModelSessionAccess.kt` | 79 | 175 | +96 (TYPE_USE cache machinery) |
| `JavaTypeOverAst.kt` | unchanged shape | +5 | +5 (lazy-filtered annotations getter) |
| `JavaClassFinderOverAstImpl.kt` | unchanged shape | +5 | +5 (TYPE_USE cache registration) |
| `JavaMemberOverAst.kt` | unchanged shape | −5 | −5 (inlined external resolver into `initializerValue`) |
| `JavaAnnotationOverAst.kt` | unchanged shape | +5 | +5 (const-vs-enum disambiguation block) |
| **`JavaExternalConstResolver.kt`** (new) | 0 | 185 | **+185** |

**Net FIR-jvm module delta**: ≈ **−367 LoC**.
**Net java-direct module delta**: ≈ **+337 LoC**.
**Net codebase delta**: ≈ **−30 LoC**, plus three retired callback
interfaces, plus a deleted file. Architecturally the win is larger:
every `JavaModelExtensions.kt`-shaped model→FIR callback that the
2026-05-04 design doc treated as the canonical bridge has been
replaced by a direct call routed through `JavaResolutionContext`'s
session — leaving the public Java-model interface clean and FIR-jvm
unaware of java-direct's existence.

### Outstanding items (revised)

| Item | Status | Note |
|---|---|---|
| Perf re-measurement against `KotlinFullPipelineTestsGenerated` | Not done | TYPE_USE cache + per-call resolver lookups should re-validate against the existing perf canary. |
| Broader-corpus probe (D1/D2/D3 validation) | Not done | Same as before; independent of these cleanups. |
| Doc updates in `TYPE_USE_ANNOTATION_HANDLING_2026_05_04.md` | Not done | §3-5 of that doc still treat the FIR-side TYPE_USE filter as load-bearing; should be marked obsoleted. |
| Doc updates in `INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md` | Not done | §3-§3.x catalogue the three retired callback interfaces as "still in flight"; mark all three retired and the `JavaModelExtensions.kt` file as deleted. |
| Doc updates in `FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` | Not done | §11-12 reference all three callbacks as the model→FIR bridges; flag the section as historical. |

### Bottom line (revised, again)

After the 2026-05-25 iterations, the public Java-model interface
surface in `core/compiler.common.jvm/.../structure/*` is identical to
the pre-java-direct shape (rule 7 of `AGENT_INSTRUCTIONS.md`
satisfied), the FIR-jvm module is no longer carrying any
java-direct-specific protocol interface, and the three callback
patterns documented in `TYPE_USE_ANNOTATION_HANDLING_2026_05_04.md`
have been collapsed into direct calls. `JavaModelExtensions.kt` —
the file that *named* this entire bridge pattern — is **gone**.
