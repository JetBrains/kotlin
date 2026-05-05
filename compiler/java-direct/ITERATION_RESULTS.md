# Java-Direct: Iteration Results Log

**Current status**: 1178/1178 box + 1513/1513 phased (2793/2793, 100%). Phased and
box generators now actually route `// FILE: *.java` blocks through java-direct AST;
prior numbers were against PSI loading (see 2026-04-28 entry).

**Last Updated**: 2026-05-05 (Step 4 of merged plan: Unification Stage 4 landed in `JavaResolutionContext.resolveFromLocalScope` — `findLocalClass` collapsed out of the `ClassId`-resolution path in favour of a `getContainingClassIds()` walk through the FIR `tryResolve` callback; AST classifier path retains `findLocalClass` as a Stage-5-deferred fast path)

### Entry Template

```markdown
## [Title] — [Date]

### Overview
[1-2 sentence summary of what was done and why.]

### Changes
[Bullet list of concrete changes: file, what changed, why.]

### Test Results
[Suite name, pass/fail count, any regressions.]

### Files Modified
| File | Change |
|------|--------|
| `file.kt` | description |

### Key Learnings
[Bullet list of non-obvious findings useful for future work.]
```

> **Add new entries below this line.** Most recent first. Separate with `---`.

---

## Merged plan Step 4: Unification Stage 4 (`findLocalClass` removed from `ClassId`-resolution path; `resolveFromLocalScope` walks `getContainingClassIds()` via FIR `tryResolve`) — 2026-05-05 (Step 4)

### Overview

Landed Step 4 of `MERGED_REFACTORING_PLAN_2026_05_04.md` — the resolver-unification "Stage 4 + Stage 5 (partial)" piece — on top of the green Step-3 baseline. The AST-side `JavaScopeResolver.findLocalClass` is no longer in the `ClassId`-resolution path: `JavaResolutionContext.resolveFromLocalScope` (step 2 of `resolveSimpleNameToClassIdImpl`, JLS 6.5.2) now walks `getContainingClassIds()` from innermost to outermost and probes the FIR symbol provider via `tryResolve(containingId.createNestedClassId(name))`. Stage 5's full collapse (shrinking the AST side to "type parameter?" + `containingClassIds` only) remains a deferred concern — `findLocalClass` is retained for the AST classifier path (`JavaTypeOverAst.computeClassifier`), where the j+k_complex.kt trip-wire from the Step-3 post-mortem still requires a structural `JavaClass` with its full outer-class chain.

### Changes

- **Stage 4 — `JavaResolutionContext.resolveFromLocalScope`**
  - Replaced the previous AST-side 2a path:
    ```kotlin
    findLocalClass(Name.identifier(simpleName))?.let { localClass ->
        val fqName = localClass.fqName
        if (fqName != null) {
            val classId = fqNameToClassId(fqName)
            if (tryResolve(classId)) return classId
        }
    }
    ```
    with the Stage-4 spec's containing-chain FIR walk:
    ```kotlin
    val nameId = Name.identifier(simpleName)
    for (containingId in getContainingClassIds()) {
        val candidate = containingId.createNestedClassId(nameId)
        if (tryResolve(candidate)) return candidate
    }
    ```
  - The walk subsumes steps 1, 2, 4 of `JavaScopeResolver.findLocalClass` (directly-declared
    inner classes anywhere up the containing chain) by relying on the FIR symbol
    provider's existing `JvmSymbolProvider → JavaClassFinderOverAstImpl` chain to resolve
    `containingId.createNestedClassId(name)` to the same AST node those AST-side queries
    would have produced. JLS 6.3 innermost-wins ordering is preserved by iterating
    `getContainingClassIds()` from innermost to outermost (its existing contract).
  - Step 3 of the AST `findLocalClass` (inherited inners from supertypes) is covered by
    the existing 2b path (aggregated map / two-phase BFS via
    `resolveInheritedInnerClassToClassId`), unchanged.
  - Step 5 of the AST `findLocalClass` (same-file top-level fast path) is intentionally
    *not* reproduced inside `resolveFromLocalScope`: same-file top-level classes share
    their `ClassId` with same-package cross-file classes
    (`ClassId(packageFqName, simpleName)`), so they are picked up by the next step in
    `resolveSimpleNameToClassIdImpl` — `resolveFromSamePackage`. No new `tryResolve`
    cost: the same single probe happens, just one step later.
  - The KDoc on `resolveFromLocalScope` is rewritten to describe the Stage-4 outcome,
    cite the unification doc, and explicitly call out where each of the old
    `findLocalClass` steps now lives.

- **Stage 5 partial — `JavaScopeResolver.findLocalClass` (KDoc only)**
  - Rewrote the KDoc to record the post-Stage-4 role: this method is no longer in the
    `ClassId`-resolution path; it is the AST-side fast path used by the Java model layer
    (`JavaTypeOverAst.computeClassifier`, `JavaClassCache`, `ConstantEvaluator`). Body is
    unchanged — the five-step ordering is still required because the AST classifier path
    needs a structural `JavaClass` (with full outer-class chain) for cross-file
    inherited inners (the `j+k_complex.kt` trip-wire from the Step-3 post-mortem).
  - Stage 5's full collapse — shrinking the AST side to "type parameter?" +
    `getContainingClassIds()` — is documented as a deferred concern: it requires giving
    the AST classifier path a FIR-derived `JavaClass` for cross-file inherited inners,
    which the existing `getClassLikeSymbol` callback alone does not provide.

- **`JavaResolutionContext.findLocalClass` (KDoc only)** — passthrough doc updated to
  point at `JavaScopeResolver.findLocalClass`'s KDoc for the post-Stage-4 role.

### Test Results

`./gradlew :kotlin-java-direct:test --tests JavaUsingAstPhasedTestGenerated --tests JavaUsingAstBoxTestGenerated --rerun-tasks --no-build-cache` — **BUILD SUCCESSFUL** in 1m 56s, 0 failures / 0 errors. XML parse of `build/test-results/test/`: **2693 tests, all passed** (no regressions vs. the post-Step-3 baseline).

The Step-4 perf gate on `testIntellij_platform_externalProcessAuthHelper` (re-run parse counter on the Stage-3 testbed; per the merged plan validation gate, must be ≤ Step-3's value within noise) was **NOT** run in this iteration — same harness-unreachability constraint as Step 3. The Stage-4 change is structurally a *replacement* of one same-cost lookup with another (one `findLocalClass`-mediated `tryResolve` per innermost containing class becomes one `tryResolve(containingId.createNestedClassId(name))` per containing-class entry), so the parse counter cannot be affected by this change alone (`tryResolve` does not parse anything; `findLocalClass`'s syntactic AST queries do not parse either). The symbol-creation counter could theoretically tick up by one extra `getClassLikeSymbolByClassId` call per containing-chain level for misses, but the FIR `tryResolve` callback already short-circuits on the first hit, and the chain is typically 1–2 deep. If the harness becomes available before Step 5, this iteration's perf gate can be re-run retrospectively.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../resolution/JavaResolutionContext.kt` | `resolveFromLocalScope`: Stage-4 swap (2a → containing-chain FIR walk); KDoc rewrite. `findLocalClass` passthrough KDoc updated. |
| `compiler/java-direct/src/.../resolution/JavaScopeResolver.kt` | `findLocalClass` KDoc rewritten to describe post-Stage-4 role + Stage-5 deferral note. Body unchanged. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; `Last Updated` bumped. |

### Key Learnings

- **The Stage-4 spec's `findLocalClass: JavaClass?` signature is approximate.** The
  unification doc shows `fun findLocalClass(name): JavaClass? { /* FIR via getClassLikeSymbol */ }`,
  but `getClassLikeSymbolByClassId` returns a FIR symbol, not an AST `JavaClass`. The
  practical Stage-4 transformation operates at the *`ClassId`-resolution* layer
  (`resolveFromLocalScope`), where the FIR `tryResolve` callback already does what the
  spec describes. The AST classifier path keeps a separate `findLocalClass` because its
  consumers (`JavaTypeOverAst.computeClassifier`) require a structural `JavaClass`.
- **Same-file top-level classes don't need a dedicated fast path inside
  `resolveFromLocalScope`.** They share their `ClassId` with same-package cross-file
  classes, so `resolveFromSamePackage` (the next step in `resolveSimpleNameToClassIdImpl`)
  handles them with the same single `tryResolve` probe. The only behavioural change is
  that same-file top-level no longer beats inherited inners in the `ClassId` path — but
  that aligns with JLS 6.3 / 6.5.5.1 priority (inherited inners are in narrower scope
  than same-package top-level).
- **`getContainingClassIds()` already preserves innermost-wins ordering** (returns from
  containingClass outwards, walking `outerClass`), so the Stage-4 walk does not need a
  separate ordering pass.
- **Stage 5's full collapse is genuinely entangled with the AST classifier API.**
  `JavaTypeOverAst.computeClassifier` consumes `findLocalClass` for both single-name
  lookup AND multi-part navigation (via `JavaClass.findInnerClass`). Eliminating
  `findLocalClass` requires either restructuring `computeClassifier` to consult only
  `findTypeParameter` + same-file fast path (with FIR taking over for everything else),
  or providing a FIR-derived `JavaClass` for cross-file inherited inners. Neither is
  in scope for Step 4; both belong to the Stage-5 work that the merged plan defers
  through Step 5's verification-only sweep.

---

## Merged plan Step 3: Unification Stage 3 (replace `Java.Source` filter with `lazyResolveToPhase(SUPER_TYPES)`); Stage 2b deferred again — 2026-05-05 (later)

### Overview

Landed Step 3 of `MERGED_REFACTORING_PLAN_2026_05_04.md` — the substantive correctness-and-laziness piece of the resolver-unification track. Replaced the
`FirDeclarationOrigin.Java.Source` short-circuit in `JavaTypeConversion.getResolvedSupertypeClassIds`
(and the analogous `firClass is FirJavaClass` short-circuit in `findTypeArgsForClassInHierarchy`)
with `lazyResolveToPhase(SUPER_TYPES)` on the looked-up class symbol. Stage 2b ("drop Phase 1
of `JavaInheritedMemberResolver.resolveInheritedInnerClassToClassId`") was attempted as the
plan specifies but had to be reverted — see the Stage-2b post-mortem below.

### Changes

- **Stage 3 — `JavaTypeConversion.getResolvedSupertypeClassIds`**
  - Replaced the early-return `if (firClass is FirJavaClass && firClass.origin == FirDeclarationOrigin.Java.Source) return emptyList()`
    with `classSymbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)` *before* reading
    `superTypeRefs`. The phase contract is the cycle bound: when the symbol's `SUPER_TYPES`
    is already on the lazy stack the call is a no-op and we read whatever's already
    materialised; otherwise it lazily promotes the class to that phase. In compiler
    (non-LL-FIR) mode the call is a no-op outright, since the compiler is non-lazy and the
    phase is reached before Java class member conversion runs.
  - Removed the `FirJavaClass` import (now truly unused) and added `FirResolvePhase` +
    `lazyResolveToPhase` imports.
- **Stage 3 (analogue) — `JavaTypeConversion.findTypeArgsForClassInHierarchy`**
  - Replaced the `firClass is FirJavaClass` short-circuit (which made type-argument hierarchy
    walks bail out at the first Java-source supertype) with the same `lazyResolveToPhase(SUPER_TYPES)`
    pattern. Without this swap, `findOuterTypeArgsFromHierarchy` could not thread the
    `H ↦ Int` substitution through `Outer<H> extends BaseOuter<H>` for inherited inner
    classes — see the `j+k_complex.kt` post-mortem in this entry.
- **Stage 2b — attempted, reverted, deferred again (documentation-only this iteration)**
  - First attempt: rewrote `JavaInheritedMemberResolver.resolveInheritedInnerClassToClassId`
    as a single origin-agnostic BFS via `getSupertypeClassIds`, dropped
    `walkJavaSourceSupertypes` (Phase 1), dropped `findInnerClassFromSupertypes`,
    simplified the constructor to no-args, dropped step 3 of `JavaScopeResolver.findLocalClass`,
    and dropped the `inheritedMemberResolver` field on `JavaScopeResolver`. The
    `JavaUsingAst*` matrix regressed on **two** tests:
    1. `compiler/testData/diagnostics/tests/generics/innerClasses/j+k_complex.kt` —
       resolving `Outer.bar()`'s return type `BaseInner<Double, String>` no longer threaded
       the outer-type-argument substitution `H ↦ Int`. Root cause: the dropped
       `findInnerClassFromSupertypes` returned a `JavaClass(BaseInner)` with its full
       AST-side outer-class chain (`outerClass = BaseOuter`), which the rest of the AST
       pipeline (`JavaTypeOverAst.computeClassifier`,
       `JavaClassOverAst.findInnerClassInSupertypes`) feeds into FIR for type-argument
       substitution. The BFS-only path returns only a bare `ClassId` and loses that chain.
       FIR's `findOuterTypeArgsFromHierarchy` is supposed to reconstruct the substitution
       from `containingClassIds`, but it intentionally skips index 0 (the immediate
       containing class) to avoid re-entering `SUPER_TYPES` on it; for `Outer.bar()` only
       index 0 carries the `extends BaseOuter<H>` annotation. Widening that walk to
       index 0 (with `lazyResolveToPhase(SUPER_TYPES)` as the cycle bound) didn't help —
       the FIR-side path resolves the type *before* the lazy machinery has finalised the
       substitution.
    2. `compiler/testData/diagnostics/tests/j+k/collectionOverrides/mapMethodsImplementedInJava.kt` —
       resolving `Set<Entry<String, String>>` inside
       `Derived extends Base<String> implements Map<String, T>` failed to find
       `java.util.Map.Entry`, leaving `Derived` apparently abstract and producing
       `ABSTRACT_MEMBER_NOT_IMPLEMENTED` on `class Impl : Derived()` in `main.kt`. Root
       cause: in compiler (non-LL-FIR) mode `lazyResolveToPhase(SUPER_TYPES)` is a no-op,
       so `getResolvedSupertypeClassIds(Base)` reads `Base.superTypeRefs` directly. When
       the BFS is invoked while `Base`'s own `SUPER_TYPES` resolution is mid-stack,
       `superTypeRefs` may be empty / partial, so Phase 2 alone never reaches `Map`.
       Phase 1's classFinder/source-index walk doesn't depend on FIR's phase state, so it
       stays correct in this case.
  - Resolution: kept Stage 3 (the lazy-phase swaps), restored everything else: the original
    two-phase `resolveInheritedInnerClassToClassId` (Phase 1 + Phase 2), the
    `findInnerClassFromSupertypes` AST-side resolver, the constructor params on
    `JavaInheritedMemberResolver`, step 3 of `JavaScopeResolver.findLocalClass`, the
    `inheritedMemberResolver` field on `JavaScopeResolver`, and `findOuterTypeArgsFromHierarchy`'s
    original index-1+ walk. The Stage-2b deferral note on `JavaInheritedMemberResolver`
    is rewritten to record both regressions and the laziness-timing finding.

### Test Results

`./gradlew :kotlin-java-direct:test --tests JavaUsingAstPhasedTestGenerated --tests JavaUsingAstBoxTestGenerated --rerun-tasks --no-build-cache` — **BUILD SUCCESSFUL**, 0 failures / 0 errors. XML parse of `build/test-results/test/`: **2693 tests, all passed** (no regressions vs. the post-Step-2 baseline).

The Step-3 perf gate on `testIntellij_platform_externalProcessAuthHelper` (parse-counter / symbol-creation-counter from `AGENT_INSTRUCTIONS` rule 3) was NOT run in this iteration — the harness wasn't reachable in this session and the merged plan's Step 3 explicitly allows skipping the perf gate when it is "structurally non-applicable to the change set" (the `lazyResolveToPhase(SUPER_TYPES)` call is a no-op in compiler mode, so it cannot affect parse counts; the only observable cost in compiler mode is one extra method call per supertype lookup, well below the harness's signal threshold).

### Files Modified

| File | Change |
|------|--------|
| `compiler/fir/fir-jvm/src/.../JavaTypeConversion.kt` | `getResolvedSupertypeClassIds`: replaced `Java.Source` filter with `lazyResolveToPhase(SUPER_TYPES)`; KDoc rewritten. `findTypeArgsForClassInHierarchy`: replaced `firClass is FirJavaClass` short-circuit with `lazyResolveToPhase(SUPER_TYPES)`; KDoc rewritten. Removed unused `FirJavaClass` import; added `FirResolvePhase` + `lazyResolveToPhase` imports. |
| `compiler/java-direct/src/.../resolution/JavaInheritedMemberResolver.kt` | KDoc rewritten with explicit Stage-2b deferral note that records the `mapMethodsImplementedInJava.kt` and `j+k_complex.kt` regressions and the laziness-timing finding. Function bodies unchanged. |
| `compiler/java-direct/ITERATION_RESULTS.md` | Added this entry; bumped `Last Updated`. |

### Key Learnings

- **Stage 3's `lazyResolveToPhase(SUPER_TYPES)` is correctness-preserving in compiler mode but
  only behaviour-preserving — not behaviour-equivalent — when called mid-`SUPER_TYPES`.**
  In LL-FIR mode the call lazily promotes the supertype's phase before reading
  `superTypeRefs`, so the result is always materialised. In compiler mode the call is a
  no-op and `superTypeRefs` is read directly; if the supertype's `SUPER_TYPES` is on the
  call stack but not yet finished, `superTypeRefs` may be empty. The Stage-3 callers in
  `JavaTypeConversion` happen to not hit that case (the body-resolution-phase callers are
  past their containing class's `SUPER_TYPES`); the BFS in
  `resolveInheritedInnerClassToClassId` *would* hit it for cross-source-class chains
  (the `mapMethodsImplementedInJava` regression), which is exactly why Stage 2b's
  Phase-1-drop is unsafe in compiler mode despite Stage 3.
- **`findOuterTypeArgsFromHierarchy` cannot replace the AST-side `JavaClass` chain.**
  Widening it to include index 0 (the immediate containing class) doesn't recover the
  `H ↦ Int` substitution for the `j+k_complex.kt` case. The substitution is only
  available after the type ref has been converted with the AST `JavaClass`'s outer-class
  chain attached, because that's what carries the type-parameter binding. FIR's
  `containingClassIds` walk reaches the same supertype but via a different path that
  hasn't yet been substituted at the resolution point.
- **Stage 2b is a Stage-5 concern, not a Step-3 sub-step.** The merged plan grouped
  Stage 2b with Stage 3 because both conceptually depend on
  `getResolvedSupertypeClassIds` being origin-agnostic. In practice, the AST-side
  Phase 1 also serves as a *stability profile* (independent of FIR's lazy phase machinery)
  that Phase 2 cannot match in compiler mode. Collapsing Phase 1 + Phase 2 needs either
  (a) a phase-aware adapter that forces the supertype's `SUPER_TYPES` from the *outermost*
  lazy entry, or (b) Stage 5's "origin-agnostic AST-side core" that yields a `JavaClass`
  with the AST chain even for cross-file inherited inners. Option (b) is the cleaner
  long-term shape and is what the merged plan's Stage 5 already targets.
- **Bisection drove every decision in this iteration.** The `--rerun` gradle flag
  doesn't write `.actual` neighbours and gradle truncated `system-out` between forks,
  so the only reliable way to read the assertion's actual content was a temporary
  `assertEqualsToFile` instrumentation that wrote `expected` / `actual` to
  `/tmp/jd_assert_dumps/`. That instrumentation was removed before submission.

---

## Merged plan Step 2: Unification Stage 1 + partial Stage 2a (drop outer-chain inherited walks); Stage 2b deferred — 2026-05-05

### Overview

Landed Step 2 of `MERGED_REFACTORING_PLAN_2026_05_04.md` — the "mechanical, risk-free"
stage of the resolver-unification track. Two sub-stages applied in this iteration:
**Stage 1** added the `getClassLikeSymbol` callback API surface (origin-aware counterpart
to `tryResolve`) to `JavaResolutionContext.resolve()`; **Stage 2a** narrowed
`JavaScopeResolver.findLocalClass` by removing the AST-side `findInnerClassFromSupertypes`
walk on every *outer* class up the containing chain (the redundant path), retaining only
the walk on the immediate containing class as a load-bearing case. The original Step 2
also asks for **Stage 2b** ("drop `JavaInheritedMemberResolver`'s Phase 1") — that drop
turned out to be inseparable from Stage 3 and is deferred with a documenting KDoc; see
"Stage 2b deferral" below.

### Changes

- **Stage 1 — `getClassLikeSymbol` callback (new API surface)**
  - New file `compiler/java-direct/src/.../resolution/JavaResolvedClassLikeSymbol.kt`
    (~52 lines) introducing the public `JavaResolvedClassOrigin` enum
    (`JAVA_SOURCE` / `JAVA_LIBRARY` / `KOTLIN` / `OTHER` — mirrors the relevant subset
    of `FirDeclarationOrigin` without taking a FIR-internal dependency from `java-direct`)
    and the public `JavaResolvedClassLikeSymbol(classId, origin)` data class.
  - `JavaResolutionContext.resolve()` gained a fourth optional parameter
    `getClassLikeSymbol: ((ClassId) -> JavaResolvedClassLikeSymbol?)? = null`. When it
    is supplied, the function derives an `effectiveTryResolve = { getClassLikeSymbol(it) != null }`
    so the boolean and the rich callback can never disagree within one invocation; when
    it is not supplied (the only case for now — no current caller passes it), behaviour
    is byte-for-byte unchanged. The parameter is the API hook future stages plug into;
    Stage 1 is therefore behaviour-preserving by construction.
- **Stage 2a (partial) — `JavaScopeResolver.findLocalClass`**
  - Removed the call to `inheritedMemberResolver.findInnerClassFromSupertypes(name, outer, ...)`
    inside the `outer = containingClass.outerClass; while (outer != null) { ... }` loop.
    That walk was redundant with the aggregated-map / BFS lookup performed by
    `JavaResolutionContext.resolveFromLocalScope` step 2b (the aggregated map covers the
    same "inherited inner class through an outer's supertype" cases via the source index
    and the BFS fallback covers cross-file Kotlin/binary supertypes via FIR).
  - **Retained** the call on the *containing* class (step 3 of `findLocalClass`).
    Bisecting Stage 2a showed that removing this one too regresses
    `compiler/testData/diagnostics/tests/generics/innerClasses/j+k_complex.kt`. Root
    cause: the `findInnerClassFromSupertypes` path returns a `JavaClass` whose `fqName`
    yields a different (source-side) `ClassId` shape than the supertype-keyed ClassIds
    the aggregated map produces, and the FIR side has not yet materialised the latter
    at the resolution point. The retained call is therefore load-bearing today; cleaning
    it up is folded into Stage 5 (final origin-agnostic AST-side core).
  - KDoc on `findLocalClass` rewritten to describe the new five-step ordering and to
    cite the merged plan + `j+k_complex.kt` as the rationale for the retention.
- **Stage 2b — deferred to land with Stage 3 (documentation only this iteration)**
  - Added a "Stage 2b deferral note" block to the KDoc of
    `JavaInheritedMemberResolver.resolveInheritedInnerClassToClassId`. Rationale recorded
    inline: today `JavaTypeConversion.getResolvedSupertypeClassIds` short-circuits to
    `emptyList()` for `FirDeclarationOrigin.Java.Source` (the documented
    avoid-premature-lazy-resolution filter at line 446 of `JavaTypeConversion.kt`), so
    `walkBinarySupertypes` (Phase 2) cannot traverse Java-source supertypes today.
    `walkJavaSourceSupertypes` (Phase 1) is the only path that can reach inner classes
    inherited through a `JavaSource → JavaSource → ...` chain. Stage 3 of the unification
    replaces that filter with `lazyResolveToPhase(SUPER_TYPES)`; once it lands, Phase 1
    collapses cleanly into Phase 2. Until then, Phase 1 stays.

### Test Results

`./gradlew :kotlin-java-direct:test --tests JavaUsingAstPhasedTestGenerated --tests JavaUsingAstBoxTestGenerated --rerun-tasks --no-build-cache` — **BUILD SUCCESSFUL**, 0 failures, 0 errors. The `JavaUsingAst*` matrix is unchanged from the
previous green baseline. The intermediate state (Stage 2a as originally specified —
removing `findInnerClassFromSupertypes` from both the containing-class step and the outer
chain) regressed exactly one test (`InnerClasses.testJ_k_complex`) which is what drove
the partial-removal decision documented above.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../resolution/JavaResolvedClassLikeSymbol.kt` | New (~52 lines): `JavaResolvedClassOrigin` enum + `JavaResolvedClassLikeSymbol` data class. |
| `compiler/java-direct/src/.../resolution/JavaResolutionContext.kt` | `resolve()` gained `getClassLikeSymbol: ((ClassId) -> JavaResolvedClassLikeSymbol?)? = null`; existing `tryResolve` is replaced by an `effectiveTryResolve` that delegates to the rich callback when supplied. |
| `compiler/java-direct/src/.../resolution/JavaScopeResolver.kt` | `findLocalClass`: dropped the per-outer-class `findInnerClassFromSupertypes` walk; KDoc rewritten to describe the new five-step order and cite `j+k_complex.kt`. |
| `compiler/java-direct/src/.../resolution/JavaInheritedMemberResolver.kt` | KDoc-only: added Stage-2b deferral note to `resolveInheritedInnerClassToClassId`. |

### Key Learnings

- **Stage 2 is "mechanical" only above the line, not below it.** The merged plan's
  Step 2 reads as a single mechanical bundle; the actual code shows Stage 1 / Stage 2a
  outer-chain are genuinely mechanical, but `findLocalClass`'s containing-class walk and
  `JavaInheritedMemberResolver`'s Phase 1 are entangled with the `Java.Source` filter
  in `JavaTypeConversion.getResolvedSupertypeClassIds`. Removing them ahead of Stage 3
  regresses cases that depend on the source-index walk being the only origin-agnostic
  path. The right unit of landing is therefore "Stage 1 + Stage 2a outer-chain now;
  Stage 2a containing-class + Stage 2b together with Stage 3", not "Stage 2 wholesale".
- **`j+k_complex.kt` is the canonical pre-Stage-3 trip-wire.** It exercises an inherited
  inner class along a same-file Java-source `class Outer<H> extends BaseOuter<H>` chain
  where the inner is declared on `BaseOuter`. The aggregated map / BFS fallback path
  reaches the inner via supertype-keyed ClassIds that the FIR side has not yet
  materialised, while `findInnerClassFromSupertypes` reaches it via the AST/source-index
  walk. Pre-Stage-3, only the AST path is reliable.
- **`getClassLikeSymbol` should be public, not internal.** First attempt placed the new
  types as `internal` to mirror the convention of resolution-package internals; the
  Kotlin compiler then refused to expose them through the public `resolve()` signature
  on `JavaResolutionContext` (an unrelated public class). Public visibility for the
  callback's parameter type is structurally required, not stylistic.
- **`--rerun` does not re-write `assertEqualsToFile` `.actual` neighbours**, so debugging
  a Stage-2 regression had to lean on bisection (re-enable suspected calls one at a time
  and re-run the suite) rather than on diff inspection. The forbidden
  `kotlin.test.update.test.data=true` rule (AGENT_INSTRUCTIONS rule 5) is respected.

---

## Merged refactoring plan: PSI removal × resolver unification — 2026-05-04 (later)

### Overview

Added `implDocs/MERGED_REFACTORING_PLAN_2026_05_04.md`, a coordination-only design
document that sequences the two ongoing refactoring tracks
(`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` and
`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`) into a single seven-step execution
order. The merged ordering is **unification first → measure → PSI Phase 2/3**, agreed
in the cross-check planning rounds. The new doc references the two source documents
rather than duplicating their content; this iteration entry is the project-convention
log of the doc landing.

### Changes

- New `compiler/java-direct/implDocs/MERGED_REFACTORING_PLAN_2026_05_04.md`
  (~352 lines). Sections:
  - §1 Overview — frames the two refactorings as one execution plan, names the source
    documents, states the high-level outcome.
  - §2 Motivation — cites the cross-check verdict ("compatible and largely reinforcing")
    and the ordering review (unification mostly local, PSI Phase 2/3 broader); lists
    non-goals.
  - §3 Expected Results — bullet list of post-merge end-state items, each linked to the
    section in the source doc that owns the detail.
  - §4 Source documents and their continuing roles — table that codifies *what* each doc
    owns, with the explicit note that this doc does not duplicate iteration entries.
  - §5 Merged execution order — seven steps with a uniform template (Origin / Goal /
    Prerequisites / Validation gate / References): (1) PSI Phase 1 ✅ landed,
    (2) Unification Stages 1–2, (3) Unification Stage 3 + perf gate on clean Phase-1
    baseline, (4) Unification Stages 4–5, (5) performance & test-data sweep,
    (6) PSI Phase 2, (7) PSI Phase 3 + 1–2-release transition + PSI removal.
  - §6 Coupling points — indirect-caller audit shared between Step 3 and Step 6;
    doc-wording follow-ups when Step 6 lands; parse-counter guardrail run twice;
    Phase-1 follow-up failures dissolved by Step 6.
  - §7 Rationale — smaller blast radius first, clean baseline for perf gate, audit-work
    re-use, plus the explicit trade-off (IntelliJ-platform-dependency removal lands
    later).
  - §8 Cross-references — `AGENT_INSTRUCTIONS.md`, `ARCHITECTURE.md`, `RESOLUTION_PIPELINE.md`,
    the two source docs, `CLASSIFIER_RESOLUTION_TRACE_2026_05_04.md`, this log.
- Step 1 status reflects current reality (default-ON, 2692/2692 (100%), six follow-ups
  fixed plus `<javaSourceRoots packagePrefix=...>` plumbing landed) — not the stale
  "default-OFF / six follow-ups pending" state from the plan-template draft.

### Test Results

Documentation-only deliverable; no build, no tests, no production source modified, in
line with `AGENT_INSTRUCTIONS.md` § Non-Negotiable Rules and the prior planning-round
agreement that this is a planning/coordination deliverable only.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/implDocs/MERGED_REFACTORING_PLAN_2026_05_04.md` | New: ~352 lines, the merged execution-order plan. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; updated `Last Updated` line. |

### Key Learnings

- **A coordination doc should not duplicate source-document content.** Each subsection
  here cross-links to a source-doc section instead. If a source doc evolves (a stage is
  re-scoped, a phase is split), this plan only has to update the link, not re-derive
  anything.
- **Step 1's status was already moving when the plan template was drafted.** The
  template assumed "default-OFF, six follow-ups pending"; reality at writing time was
  "default-ON, six follow-ups fixed, plus `packagePrefix` plumbing for
  `IntelliJFullPipelineTestsGenerated` also landed". `ITERATION_RESULTS.md` (timestamped)
  is the source of truth for status; the merged plan reflects the post-2026-05-04 state.
- **Per-step validation gates pin where the parse-counter / symbol-creation-counter
  check runs.** Two runs (after Step 3 and after Step 6), each on a clean prior baseline,
  give single-redesign attribution. Anything else collapses two changes into one signal
  and forces hand-bisection if a regression appears.

---

## Phase 1 follow-up #2: honour `<javaSourceRoots packagePrefix="...">` in `JavaPackageIndexer` — 2026-05-04

### Overview

After turning `BinaryJavaClassFinder` ON by default, the `IntelliJFullPipelineTestsGenerated`
suite started reporting widespread `[UNRESOLVED_REFERENCE]` errors on Java symbols whose
sources live under content roots configured with a non-empty `packagePrefix`
(`<javaSourceRoots packagePrefix="com.intellij">`). Adding `packagePrefix` plumbing to
`JavaPackageIndexer` closes the gap; A/B-tested representative IntelliJ tests turn green
without affecting the source-only `JavaUsingAst*` suite (still 2692/2692).

### Why this regression appeared now

PSI's `JavaClassFinderImpl` honoured `packagePrefix` natively: when scanning project source
roots, a directory `<root>/foo/bar/Baz.java` under a root with `packagePrefix=com.intellij`
was treated as if `com.intellij.foo.bar.Baz`. While PSI was the binary half of
`CombinedJavaClassFinder`, that PSI scan also covered the source half — even though the
source-half finder (`JavaClassFinderOverAstImpl`) did NOT understand `packagePrefix` and
silently dropped any `.java` file whose declared package didn't mirror the on-disk path.
With PSI no longer there to compensate, the source-half gap surfaced as
`UNRESOLVED_REFERENCE` on every Java type from a prefixed source root and cascaded into
seemingly unrelated Kotlin diagnostics (`UNRESOLVED_REFERENCE 'add'`, `NO_CONTEXT_ARGUMENT`,
etc.) once the chain of resolution started failing.

The diagnosis was a single representative test (`testIntellij_platform_externalProcessAuthHelper`):
its 4 Java files live at `<srcRoot>/externalProcessAuthHelper/*.java` with `<javaSourceRoots
packagePrefix="com.intellij">`, declaring `package com.intellij.externalProcessAuthHelper;`.
`JavaPackageIndexer.findPackageDirectories(FqName("com.intellij.externalProcessAuthHelper"))`
walked `<srcRoot>/com/intellij/externalProcessAuthHelper` (which doesn't exist), returned
empty, and the four Java types stayed unresolved.

### Changes

- New `JavaSourceRootEntry(root: VirtualFile, packagePrefix: FqName)` data class —
  the per-root data shape `JavaPackageIndexer` needs.
- `JavaDirectPluginRegistrar.JavaClassFinderOverAstFactory.createJavaClassFinder` reads
  `JavaSourceRoot` instances from `CLIConfigurationKeys.CONTENT_ROOTS` directly (instead of
  via the path-only `configuration.javaSourceRoots` accessor), so the prefix survives the
  trip into the finder.
- `JavaClassFinderOverAstImpl` primary constructor now takes
  `List<JavaSourceRootEntry>`. The legacy `List<VirtualFile>` call shape is kept via
  `Companion.invoke` (operator `invoke`) — modelled this way because both ctors would erase
  to `(List, JavaSourceFileReader)` on the JVM and Kotlin would reject the platform
  declaration clash. `Companion.invoke` is only picked when no constructor matches the
  argument types, so existing tests that pass `List<VirtualFile>` keep compiling unchanged.
- `JavaPackageIndexer`:
  - `findPackageDirectories(packageFqName)` honours each root's prefix: if a root has
    prefix `com.intellij`, a request for `com.intellij.foo` descends to `<root>/foo`, and
    the root contributes nothing to packages outside `com.intellij`. The unqualified-root
    case (`packageFqName.isRoot`) only includes prefix-less roots.
  - `containsPackage(packageFqName)` returns `true` for any ancestor of (or equal to) a
    configured prefix — so a root with `packagePrefix=com.intellij` makes `com`,
    `com.intellij`, and `com.intellij.foo` all valid `JavaPackage`s.
  - `subPackagesOf(fqName)` enumerates prefix-derived sub-packages: a root with prefix
    `com.intellij` contributes `intellij` as a sub-package of `com`, even though the disk
    root has no `intellij` directory.
  - Two new helpers (`findPackageDirectoryUnder`, `addSubdirsAsSubPackages`,
    `packageStartsWithOrEquals`) factor out the common walks.

### Test Results

| Test | `USE_BINARY_FINDER=false` (PSI) | `USE_BINARY_FINDER=true` + this fix |
|------|---------------------------------|-------------------------------------|
| `testIntellij_platform_externalProcessAuthHelper` | ✅ pass | ✅ pass (was ❌ before fix) |
| `testIntellij_platform_credentialStore_impl` | ✅ pass | ✅ pass (was ❌ before fix) |
| `testIntellij_database_dialects_h2` | ✅ pass | ✅ pass (was ❌ before fix) |
| `testIntellij_gradle_java` | ✅ pass | ✅ pass (was ❌ before fix) |
| `testIntellij_yaml` | ✅ pass | ✅ pass (was ❌ before fix) |
| `testIntellij_javascript_parser` | ❌ fail | ❌ fail (pre-existing, unrelated) |
| `testToolbox_ui_common` | ❌ fail | ❌ fail (pre-existing, unrelated) |
| `testFleet_noria_cells` | ❌ fail | ❌ fail (pre-existing, unrelated) |

The pre-existing failures show Kotlin-side diagnostics (`CONTEXT_PARAMETERS_ARE_DEPRECATED`,
`LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR`, JS-parser-specific compilation errors) that
also fail under PSI as binary half — they are not caused or affected by `BinaryJavaClassFinder`
or this fix and are out of scope here.

`JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated` (the source-half
regression suite) with `USE_BINARY_FINDER=true`: **2692/2692 (100%)** — no regression.

`JavaParsingClassFinderTest` + `JavaParsingLightweightScannerTest` (unit tests, MUST stay
green): all green.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../JavaPackageIndexer.kt` | New `JavaSourceRootEntry` data class; `findPackageDirectories` / `containsPackage` / `subPackagesOf` honour `packagePrefix`; helpers `findPackageDirectoryUnder` / `addSubdirsAsSubPackages` / `packageStartsWithOrEquals`. |
| `compiler/java-direct/src/.../JavaClassFinderOverAstImpl.kt` | Primary ctor now takes `List<JavaSourceRootEntry>`; `Companion.invoke` keeps the legacy `List<VirtualFile>` call shape working without a JVM signature clash. |
| `compiler/java-direct/src/.../JavaDirectPluginRegistrar.kt` | Reads `JavaSourceRoot` entries from `CLIConfigurationKeys.CONTENT_ROOTS` directly so each root's `packagePrefix` is preserved when the finder is built. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; updated `Last Updated` line. |

### Key Learnings

- **`packagePrefix` is a JLS-flavoured logical-package mapping for source roots**, not a
  layout constraint. Two source files in the same on-disk directory may belong to different
  declared packages, but a content root with `packagePrefix=com.intellij` says *every*
  on-disk directory `<root>/d1/.../dN` maps to package `com.intellij.d1...dN`. PSI's
  `JavaSourceRoot` knows about this; java-direct now does too.
- **`UNRESOLVED_REFERENCE 'add' / 'remove' / NO_CONTEXT_ARGUMENT` on Kotlin code can be a
  cascade from a missing Java type.** Once Kotlin's resolver fails to find a Java
  supertype/return-type, downstream Kotlin overload resolution loses anchors and the
  diagnostic plume can look very Kotlin-side. The actual root cause is in the Java
  finder. The reliable diagnostic shortcut is to flip `USE_BINARY_FINDER` and re-run the
  same test; if it passes, the regression is a binary-finder/source-finder gap, not a
  Kotlin-resolver issue.
- **`Companion.invoke` is the cleanest way to add a constructor-shaped overload that
  would otherwise erase to the same JVM signature.** Constructors win overload resolution
  if they're applicable; only when none match does Kotlin look at `Companion.invoke`. Here,
  `JavaClassFinderOverAstImpl(listOf(virtualFile))` and `JavaClassFinderOverAstImpl(listOf(entry))`
  end up calling different APIs without any source-side annotation noise.
- **Reading from `CONTENT_ROOTS` directly preserves more data than the path-only accessors.**
  `CompilerConfiguration.javaSourceRoots: Set<String>` flattens away `packagePrefix` and
  `isFriend` and a few other flags; if a downstream module needs any of those, the
  `getList(CONTENT_ROOTS).filterIsInstance<JavaSourceRoot>()` path is the right one.

---

## Phase 1 follow-up: fix the six failures triggered by enabling `BinaryJavaClassFinder` — 2026-05-04

### Overview

The six Phase-1 follow-up failures listed in the 2026-04-30 entry below all came from the
**source half** (`JavaClassFinderOverAstImpl`), not from `BinaryJavaClassFinder` itself.
Once the binary half stops being PSI, the source half no longer benefits from PSI's
silent fallback for two source-side gaps in java-direct:

1. **Ancestor-package recognition.** `JavaClassFinderOverAstImpl.findPackage(fqName)` returned
   `null` for any package that did not directly contain `.java` files — so for tests with
   sources only at `priv/members/check/MyJClass.java`, the FIR pipeline could not resolve
   the intermediate packages `priv` and `priv.members`, and dotted FQN references like
   `priv.members.check.foo()` (kt57845) plus star imports such as `import third.*`
   (`EnumEntryVsStaticAmbiguity4`) failed with `UNRESOLVED_IMPORT` /
   `UNRESOLVED_REFERENCE`. PSI's `JavaClassFinderImpl.findPackage` recognised these
   ancestors via `PsiPackage` lookups against the project source roots; with the
   PSI binary half no longer present in `CombinedJavaClassFinder`, java-direct's source
   half had to grow the same recognition.

2. **Package declarations without a trailing semicolon.** Five of the six failing
   test-data files (`EnumEntryVsStaticAmbiguity4.kt`, `protectedGetterWithPublicSetter.kt`,
   `protectedWithGenericsInDifferentPackage.kt`, `kt57845.kt`,
   `syntheticPropertyOnUnstableSmartcast.kt`, plus `annotationWithEnum.kt`) declare
   their `// FILE: */*.java` blocks as `package foo` without `;`. PSI's Java parser
   is error-tolerant and accepts that, but the lightweight pre-parse scanner used by
   java-direct (`PACKAGE_REGEX`) required `;`. Files were silently rejected from the
   index (the per-directory walk discards entries whose declared package mismatches the
   directory path), so the Java classes inside them — `OtherTypes`, `Super`, `Nls`,
   etc. — were `UNRESOLVED_REFERENCE` in the diagnostic output.

Both gaps are independent and both contribute. They were only invisible while PSI was
serving the binary half because PSI's package/class lookup found the same source files
through its own scan.

### How we diagnosed it

Added a temporary `kotlin.javaDirect.actualDumpDir` system-property hook in
`JUnit5Assertions.assertEqualsToFile` that wrote the failed-test `actual` text to a
sibling file. Diffing each captured `.actual.txt` against the original test data
showed the same shape across all six tests: the `// FILE: */*.java` block disappears
from the diagnostic output (its diagnostics are gone), and the Kotlin half acquires
`UNRESOLVED_IMPORT` / `UNRESOLVED_REFERENCE` markers on whatever symbol used to come
from that Java block. That pattern uniquely points at the source-side index. The
hook was reverted before submission.

### Changes

- `JavaPackageIndexer.containsPackage(packageFqName)` — new method. Returns `true` when
  a directory mirroring the package exists in some source root, OR when any
  `fileRootIndex` key equals `packageFqName` or is a sub-package of it. Cheap: walks
  `findChild` chains and `fileRootIndex.keys` only — no file content reads.
- `JavaClassFinderOverAstImpl.findPackage` — split the original `if (no classes && no
  package-info-annotations) return null` into three explicit positive cases (direct
  classes / package-info annotations / ancestor package via `containsPackage`).
- `PACKAGE_REGEX` in `JavaSourceIndex.kt` — trailing `;` is now optional
  (`...\s*;?` instead of `...\s*;`), matching PSI's error-tolerant Java parser. Added
  unit test `testLightweightScannerPackageWithoutTrailingSemicolon`.

### Test Results

- `JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated` with the flag
  default-ON (current state of `JavaDirectPluginRegistrar.kt`): **2692/2692 (100%)**,
  no FAILED markers, all six previously-failing tests now pass.
- `JavaParsingLightweightScannerTest` (unit tests, MUST stay green): all green,
  including the new missing-`;` case.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../JavaPackageIndexer.kt` | Added `containsPackage(packageFqName)` for ancestor-package recognition. |
| `compiler/java-direct/src/.../JavaClassFinderOverAstImpl.kt` | `findPackage` now also returns a package for ancestor fqNames via `containsPackage`. |
| `compiler/java-direct/src/.../util/JavaSourceIndex.kt` | `PACKAGE_REGEX` accepts `package <fqn>` with optional trailing `;`. |
| `compiler/java-direct/test/.../JavaParsingLightweightScannerTest.kt` | New unit test covering the missing-`;` case. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; updated `Last Updated` line. |

### Key Learnings

- **PSI's binary-side fallback was masking source-side gaps in java-direct**, not just
  binary ones. Even though `findClass` / `findPackage` for source code is logically
  the source half's responsibility, when the binary half is also a PSI implementation
  scanning the project, it can find the same source files and silently cover for any
  source-half index miss. Removing PSI from the binary half exposes those source-half
  gaps immediately.
- **`extractFileInfoLightweight` returning `null` is silent.** When the lightweight
  scanner couldn't extract a package (because the file had no `;` after `package`),
  the file was dropped from the index without warning. Top-level classes inside it
  became invisible. The `JavaParsingLightweightScannerTest` suite had no missing-`;`
  case; the new test closes that gap so future regex tightening is caught
  immediately.
- **The lightweight scanner needs to track PSI's tolerance, not Java's grammar.**
  Test data — and IntelliJ-generated `.java` snippets in general — frequently rely on
  PSI's error-tolerant parser. For java-direct to be a drop-in replacement of the
  PSI source half, its pre-parse scanner has to accept the same superset of inputs
  PSI does (or at least the subset used in the corpus we test against).
- **Ancestor packages are first-class JLS entities.** A package exists once any
  compilation unit declares it, including units of any sub-package — `package
  a.b.c.Foo` makes `a`, `a.b`, and `a.b.c` all valid `JavaPackage`s. PSI's
  `JavaClassFinderImpl` reflects this via the JVM `PsiPackage` model; the new
  `containsPackage` reflects the same rule directly against the source-root
  directory tree (and `fileRootIndex` for non-mirror file-roots).

---

## Phase 1: `BinaryJavaClassFinder` landed behind default-OFF flag — 2026-04-30 (later still)

### Overview

Implemented Phase 1 of the PSI removal plan documented in
`implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`: an index-based, PSI-free
`BinaryJavaClassFinder` (placed inside the `java-direct` module) backed by the same
`JvmDependenciesIndex` / `KotlinClassFinder` snapshot the deserializer already uses, plus the
existing ASM-based `BinaryJavaClass`. It replaces the legacy PSI binary half of
`CombinedJavaClassFinder` when the `kotlin.javaDirect.useBinaryClassFinder` system property
is `true`. Default is `false`, so existing production behaviour is unchanged.

### Changes

- Added `compiler/java-direct/src/.../BinaryJavaClassFinder.kt`. ~205 lines. Mirrors
  `KotlinCliJavaFileManagerImpl.findClass` for binary classes (top-level virtual file lookup
  via `JvmDependenciesIndex.findClassVirtualFiles`, ASM materialization via `BinaryJavaClass`,
  inner classes via `BinaryJavaClass.findInnerClass`, per-call fresh
  `ClassifierResolutionContext` for type-parameter / inner-class isolation, scope-free resolver
  for cross-classpath references).
- Added `compiler/cli/src/.../extensions/BinaryJavaClassFinderInputs.kt`: a small data carrier
  (`JvmDependenciesIndex` + `GlobalSearchScope` + `enableSearchInCtSym`) plumbed through
  `JavaClassFinderFactory`. The carrier exists to avoid a circular dependency: `compiler/cli`
  cannot reference types from `compiler/java-direct`, so `cli` ships the *inputs* and the
  factory in `java-direct` constructs the actual finder.
- `JavaClassFinderFactory.createJavaClassFinder` now takes an optional
  `binaryClassFinderInputsProvider: (() -> BinaryJavaClassFinderInputs?)?` parameter (default
  `null`). Lazy provider returns `null` outside CLI environments (e.g. LL-FIR), in which case
  the factory falls back to the legacy PSI default — preserves existing behaviour for non-CLI
  callers.
- `VfsBasedProjectEnvironment.getFirJavaFacade` plumbs the inputs lazily by downcasting
  `VirtualFileFinderFactory.getInstance(project)` to `CliVirtualFileFinderFactory` and
  reading its `index` / `enableSearchInCtSym`.
- `CliVirtualFileFinderFactory.index` and `enableSearchInCtSym` are now `val` (publicly
  readable) so the environment can hand them off to the factory.
- `JavaDirectPluginRegistrar.JavaClassFinderOverAstFactory.createJavaClassFinder` now reads
  the system property `kotlin.javaDirect.useBinaryClassFinder` (default `false`). When `true`
  and inputs are available, the binary half of `CombinedJavaClassFinder` is the new
  `BinaryJavaClassFinder`; otherwise the legacy PSI `defaultFinderProvider()` is used.
- `compiler/java-direct/build.gradle.kts`: added a one-line `systemProperty` passthrough so
  the flag flows from `-Pkotlin.javaDirect.useBinaryClassFinder=true` into the test JVM.

### Test Results

- **Default (flag OFF)**: `JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated`
  = **2692/2692 (100%)**. No regression vs. baseline.
- **Flag ON** (`-Pkotlin.javaDirect.useBinaryClassFinder=true`): **2686/2692 (99.78%)**. Six
  remaining test-data divergences (all `assertEqualsToFile` diffs in the diagnostic phase),
  documented as Phase-1 follow-up work below.

### Phase-1 follow-up work

The six failures under flag ON are documented for a follow-up iteration; the flag stays
default-OFF so production parity is preserved while these are triaged:

1. `JavaUsingAstPhasedTestGenerated.Tests.Imports.testEnumEntryVsStaticAmbiguity4`
2. `JavaUsingAstPhasedTestGenerated.ResolveWithStdlib.J_k.testAnnotationWithEnum`
3. `JavaUsingAstPhasedTestGenerated.Tests.Properties.testProtectedGetterWithPublicSetter`
4. `JavaUsingAstPhasedTestGenerated.Tests.testProtectedWithGenericsInDifferentPackage`
5. `JavaUsingAstPhasedTestGenerated.Tests.Regressions.testKt57845`
6. `JavaUsingAstPhasedTestGenerated.Tests.SmartCasts.Inference.testSyntheticPropertyOnUnstableSmartcast`

All six are `Actual data differs from file content: *.kt` diagnostic-phase divergences (no
crashes, no compile errors). They likely involve subtle differences between PSI's package
enumeration and the index-based `knownClassNamesInPackage` (e.g. how multi-file packages with
mixed Java/Kotlin sources are unioned across source ∪ binary halves), or how
`BinaryJavaPackage` reports `mayHaveAnnotations` differently from `JavaPackageImpl`.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../BinaryJavaClassFinder.kt` | New: ~205 lines, the index-based finder (Phase 1 stepping stone). |
| `compiler/cli/src/.../extensions/BinaryJavaClassFinderInputs.kt` | New: small data carrier for the cli→java-direct plumbing. |
| `compiler/cli/src/.../extensions/JavaClassFinderFactory.kt` | Added `binaryClassFinderInputsProvider` parameter. |
| `compiler/cli/src/.../VfsBasedProjectEnvironment.kt` | Plumbs inputs lazily via `CliVirtualFileFinderFactory` downcast. |
| `compiler/cli/cli-base/src/.../CliVirtualFileFinderFactory.kt` | Made `index` / `enableSearchInCtSym` public. |
| `compiler/java-direct/src/.../JavaDirectPluginRegistrar.kt` | Reads the system-property flag and selects which binary finder to inject. |
| `compiler/java-direct/build.gradle.kts` | One-line `systemProperty` passthrough for the flag. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; updated `Last Updated` line. |

### Key Learnings

- **`ClassifierResolutionContext` is mutable** — it accumulates type parameters and
  inner-class info across every `BinaryJavaClass` it materializes. Sharing one instance
  across `findClass` calls leaks type parameters from one class into the resolution of an
  unrelated one (symptom: "Unresolved type for E"). The fix is to construct a fresh context
  per top-level `findClass` invocation, exactly as `KotlinCliJavaFileManagerImpl.findClass`
  line 151 does.
- **The internal resolver must use `allScope` (not the finder's `scope`)** for cross-class
  references inside bytecode signatures — otherwise references to JDK classes from a
  library-scoped finder fail silently. Mirrors the same `allScope` choice in the reference
  implementation.
- **Circular module-dependency avoidance** — `compiler/cli` cannot depend on
  `compiler/java-direct`, so the cli-side environment ships *inputs* (an index handle, a
  scope, a flag) rather than constructing the `JavaClassFinder` itself; the `java-direct`
  factory builds the finder from those inputs.
- **Default-OFF flag** is a real safety net — even with all the structural plumbing in
  place, a single edit error (forgotten function-signature change, stale build) shows up as
  "BUILD FAILED" but **the test results directory still has the *previous* run's XMLs**,
  giving a misleadingly clean count. Always verify test results were freshly written
  *after* the BUILD FAILED was resolved.

---

## Design doc revision: three-phase plan for PSI removal — 2026-04-30 (later)

### Overview

Reframed `implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` from a single-step
`BinaryJavaClassFinder` proposal into an explicit three-phase plan: Phase 1 lands
`BinaryJavaClassFinder` as a short-lived stepping stone (not kept across releases);
Phase 2 collapses the abstraction, moves binary lookups into
`JvmClassFileBasedSymbolProvider`, and makes `FirJavaFacade.classFinder` source-only;
Phase 3 keeps PSI for **1–2 releases as a source-only fallback** behind a flag, after
which PSI is removed from the JVM-FIR / `java-direct` compilation path entirely. No
production source files were modified.

### Changes

- Rewrote §0 Executive Summary with three replacement diagrams (today / Phase 1 /
  end-state) and an explicit "PSI removed at end of Phase 3" goal.
- Restructured §2.1 around strategic goals (across all phases) plus per-phase
  constraints; added the IntelliJ-platform-dependency removal goal.
- Marked the existing `BinaryJavaClassFinder` design (§2.2) and cycle-avoidance (§2.3)
  as Phase-1-specific.
- Added new §2.4 Phase 2 (structural refactoring) and new §2.5 Phase 3 (source-only
  PSI/AST switch).
- Renumbered the migration plan (§2.6) to span all three phases; renumbered Risks
  (§2.7) into per-phase subsections including indirect-caller audit, narrowed
  `FirSession.javaSymbolProvider` semantics, AST/PSI source parity gate, and PSI
  removal blast radius.
- Renumbered Alternatives (§2.8) to record explicitly that "Keep `BinaryJavaClassFinder`
  long-term" and "Keep PSI as a binary-side fallback" were considered and rejected.

### Test Results

N/A (documentation only).

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` | Three-phase plan revision (§0, §2.1–§2.8). |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; updated `Last Updated` line. |

### Key Learnings

- The transitional fallback for the PSI removal effort belongs on the *source* side
  (Phase 3), not on the binary side; binary PSI is removed at the end of Phase 1 with
  no transitional residence.
- `BinaryJavaClassFinder` is justified strictly as a risk-isolation device — observable
  equivalence with PSI, A/B-flag-flippable — and is dissolved in Phase 2; keeping it
  long-term would re-introduce a parallel class-finder abstraction on top of a symbol
  provider stack that already owns the data source.
- The dominant cost of the structural Phase 2 is the audit of the four indirect
  callers of `session.javaSymbolProvider` (`FirJvmConflictsChecker`,
  `FirDirectJavaActualDeclarationExtractor`, Lombok `AbstractBuilderGenerator`, and
  out-of-scope `KaFirJavaInteroperabilityComponent`); this is paid once and unblocks
  the contract narrowing of `FirSession.javaSymbolProvider` to "source-only Java".
- Phase 3's PSI removal completes the IntelliJ-platform-dependency shedding for the
  JVM-FIR / `java-direct` compilation path; full deletion of `JavaClassFinderImpl` is
  separate (K1 frontend and LL-FIR keep their own copies and are out of scope).

---

## Design doc: `PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` — 2026-04-30

### Overview

Design-only deliverable: a new `implDocs/` document that maps every PSI `JavaClassFinder` entry
point reachable in production with `java-direct` enabled, and proposes a `BinaryJavaClassFinder`
backed by `JvmDependenciesIndex` / `KotlinClassFinder` + `BinaryJavaClass` to replace the
PSI binary half of `CombinedJavaClassFinder`. No production source files are modified.

### Changes

- Added `compiler/java-direct/implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`.
- This entry in `ITERATION_RESULTS.md`.

### Test Results

N/A (documentation only).

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` | New design doc (Part 1: where PSI is used; Part 2: replacement plan). |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry. |

### Key Learnings

- `JavaClassFinderOverAstFactory` builds `CombinedJavaClassFinder(source, PSI-binary)` for **both** source and library FIR sessions in production — the test-fixture (`VfsBasedProjectEnvironmentOverAst`) only exercises PSI in the library session.
- `JvmClassFileBasedSymbolProvider.extractClassMetadata`'s no-metadata branch (`JvmClassFileBasedSymbolProvider.kt:180`) is the only place in FIR core that asks the facade to materialize a `JavaClass` from a binary `.class` — and the bytes are already in hand via `KotlinClassFinder.Result.ClassFileContent`, so a replacement does not need extra I/O.
- The `FirJavaFacade ↔ JvmClassFileBasedSymbolProvider` cycle is avoided by making `BinaryJavaClassFinder` a **peer** of the deserializer (both fed by `JvmDependenciesIndex`), not a wrapper around it.
- `JavaClassFinder.findClasses` (multi-result) has no production FIR caller — only PSI's own `JavaClassFinderImpl` and LL-FIR's `LLCombinedJavaSymbolProvider` use it; the replacement does not need to support it.

---

## PSI-path regression in shared FIR files: gate the java-direct fallbacks — 2026-04-29

### Overview

Investigated a ~5% regression on `KotlinFullPipelineTestsGenerated` (PSI path,
java-direct off) that appeared after the java-direct development cycle. Root cause: the
java-direct-specific resolution fallbacks added to shared FIR files run unconditionally
on the PSI/binary path even though they have no effect there. Closed by adding three
opt-in `Boolean` properties to the `JavaType` / `JavaField` / `JavaEnumValueAnnotationArgument`
interfaces (default `false`) and gating the FIR call sites on them. java-direct overrides
to `true` to keep the existing fallbacks active for its path.

### How the regression was identified

Branch state before the work: top two commits were
`66086559d511 ~ undo changes outside of java-direct` (reverts the shared-FIR/structure
changes accumulated during java-direct development) and
`a9e0e74fd498 ~ undo apply by default` (returns `LanguageFeature.JavaDirect` to
`sinceVersion = null` and the registrar guard back). With both reverted, the branch HEAD
is a pure baseline; reverting just the top commit re-applies the shared-FIR changes
without turning java-direct on.

Three SAME_THREAD measurements of `KotlinFullPipelineTestsGenerated` (single rep each,
XML test-phase wall time, build kept warm between runs) confirmed the regression as a
PSI-path issue, not a java-direct issue:

| Config | Time | Δ vs baseline |
|---|---:|---:|
| Baseline (HEAD as-is, external changes reverted) | 236.27s | — |
| Regression (revert of top commit, no fix) | 241.57s | **+2.24%** |
| With first gate (`couldBeConstReference`) | 230.30s | -2.53% |
| With all three gates | 235.30s | -0.41% |

The regression-vs-fix delta of ~5% matches the originally observed FP-test slowdown.
All "with-fix*" configurations are within single-run noise (~±2%) of baseline.

### Root cause

Three call sites in shared FIR files take a callback that's wasted on PSI/binary input:

1. **`javaAnnotationsMapping.toFirExpression`'s `JavaEnumValueAnnotationArgument` branch**
   calls `resolveConstFieldValue(session, classId, fieldName)` for every enum-shaped
   annotation argument — including dominant cases like `@Retention(RUNTIME)`,
   `@Target(METHOD)`, `@Target({TYPE, FIELD})`. The helper does
   `session.symbolProvider.getClassLikeSymbolByClassId(classId)`, allocates a
   `filterIsInstance<FirProperty>()` list of declarations, walks both the class and its
   companion, then probes `session.symbolProvider.getTopLevelPropertySymbols(...)`. PSI
   never reaches this code path with a real const-reference because PSI splits literal
   const refs (`KConstsKt.WARNING`) into `JavaLiteralAnnotationArgument` at structure-build
   time; only java-direct (which can't disambiguate at parse time) needs the fallback.

2. **`JavaTypeConversion.toFirJavaTypeRef` and `toConeTypeProjection`** both call
   `filterTypeUseAnnotations { fqName -> isTypeUseAnnotationClass(fqName, session) }` per
   type-ref / type-projection. PSI's `JavaTypeImpl` doesn't override
   `filterTypeUseAnnotations`, so the default impl just returns `annotations`; the cost
   is one closure capturing `session` plus a virtual-dispatch round-trip per call. Cheap
   per call, but `annotationBuilder` fires once per Java type ref during enhancement, so
   it adds up.

3. **`FirJavaFacade.convertJavaFieldToFir`'s `lazyInitializer`** falls back to
   `javaField.resolveInitializerValue { … }` when `initializerValue` is `null`. PSI's
   `JavaFieldImpl` doesn't override `resolveInitializerValue`, so the fallback returns
   `null` again — but at the cost of one closure capturing `session` and
   `classId.packageFqName`. Hits every cross-language const-evaluation site.

Other branches in the reverted commit (`setSealedClassInheritors` cross-file path,
`enumEntriesOrigin`, `isPrimary` for source records, the entire `null`-classifier branch
in `JavaTypeConversion`) are dead code on the PSI path because they're guarded by
`classifier == null` or `source == null` — so they cannot have caused the regression.

### Fix

Three `Boolean` opt-in properties (default `false`) — PSI/binary inherit the default and
never enter the costly branch; java-direct overrides to `true` and continues to take its
existing fallback path:

- `JavaEnumValueAnnotationArgument.couldBeConstReference` — gates `resolveConstFieldValue`.
  PSI structurally splits const-vs-enum at build time; java-direct can't, so it opts in.
- `JavaType.needsTypeUseAnnotationFiltering` — gates the `filterTypeUseAnnotations`
  callback closure. PSI's javac-wrapper pre-filters at the structure level; java-direct
  filters at FIR call time.
- `JavaField.supportsExternalInitializerResolution` — gates the
  `resolveInitializerValue` callback closure. PSI evaluates Java-side constants at
  structure-build time; java-direct uses the FIR callback for cross-language const refs.

Additionally, `resolveConstFieldValue` short-circuits when `firClass.classKind ==
ClassKind.ENUM_CLASS`. Real enum classes can only have const properties via their
companion (entries are `FirEnumEntry`, not `FirProperty`), and the top-level/facade
fallback doesn't apply to an `<EnumClass>.X` shape. This eliminates the
`filterIsInstance<FirProperty>()` allocation and the top-level lookup for the dominant
"actual enum entry" case on java-direct's own path.

### Files Modified

| File | Change |
|------|--------|
| `core/compiler.common.jvm/src/.../structure/annotationArguments.kt` | Add `JavaEnumValueAnnotationArgument.couldBeConstReference: Boolean = false` |
| `core/compiler.common.jvm/src/.../structure/javaTypes.kt` | Add `JavaType.needsTypeUseAnnotationFiltering: Boolean = false` |
| `core/compiler.common.jvm/src/.../structure/javaElements.kt` | Add `JavaField.supportsExternalInitializerResolution: Boolean = false` |
| `compiler/fir/fir-jvm/src/.../fir/java/javaAnnotationsMapping.kt` | Gate `resolveConstFieldValue` on `couldBeConstReference`; short-circuit `resolveConstFieldValue` for enum classes |
| `compiler/fir/fir-jvm/src/.../fir/java/JavaTypeConversion.kt` | Gate `filterTypeUseAnnotations` on `needsTypeUseAnnotationFiltering` at both call sites |
| `compiler/fir/fir-jvm/src/.../fir/java/FirJavaFacade.kt` | Gate `resolveInitializerValue` callback on `supportsExternalInitializerResolution` |
| `compiler/java-direct/src/.../model/JavaAnnotationOverAst.kt` | Override `couldBeConstReference = true` on `JavaEnumValueAnnotationArgumentOverAst` |
| `compiler/java-direct/src/.../model/JavaTypeOverAst.kt` | Override `needsTypeUseAnnotationFiltering = true` on `JavaTypeOverAst` |
| `compiler/java-direct/src/.../model/JavaMemberOverAst.kt` | Override `supportsExternalInitializerResolution = true` on `JavaFieldOverAst` |

### Test Results

- `kotlin-java-direct:test` (`JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated`):
  **2692/2692 green**, no FAILED markers. Run twice — once with only the first gate, once
  with all three gates plus the enum short-circuit. java-direct functionality preserved
  in both states.
- `KotlinFullPipelineTestsGenerated` (SAME_THREAD): see table above. Regression closed.

### Methodology notes

- `ExecutionMode.SAME_THREAD` was set in
  `GenerateModularizedIsolatedTests.kt:27` and the test class was regenerated via
  `:compiler:fir:modularized-tests:generateTests` so all 414 modules run sequentially —
  needed for stable wall-clock timing under SUM-not-MAX semantics. Revert before merge.
- The XML `time="…"` field in
  `compiler/fir/modularized-tests/build/test-results/test/TEST-…KotlinFullPipelineTestsGenerated.xml`
  is the right metric; Gradle's "BUILD SUCCESSFUL in Xm Ys" mixes test phase with build
  phase, and the build phase shrinks dramatically across runs as caches warm up,
  inflating the BUILD-SUCCESSFUL delta vs. real test-phase delta.
- Single-rep noise looked to be ~±2% on this corpus. Three reps each would tighten the
  signal, but the regression-vs-fix delta of ~+5% / +11s is well above noise on a single
  rep.

### Key Learnings

- **Adding overridable interface methods with default impls to shared types is not free
  for the default-path callers.** Even when the default impl is "return the same thing
  the caller already has", every call still pays a virtual-dispatch and a callback
  closure allocation. When the call site is hot (per-Java-type-ref or per-annotation-arg
  during FIR enhancement), this can cost a few percent on workloads that don't need the
  override at all. Pairing every such method with a `Boolean` "`needsX`" gate on the same
  interface is the cheap way to keep the API additive without taxing the default path.
- **`isResolved` is not a substitute for "needs the const fallback".** java-direct's
  `JavaEnumValueAnnotationArgumentOverAst.isResolved` returns `true` for the easy
  "simple-imported" case (where `enumClassId` is built from a known import), so gating on
  `!isResolved` would have skipped the const fallback for the very case it's needed —
  `@SomeAnno(SomeImportedClass.SOME_CONST)`. The right gate is "could this argument
  ever be a const reference" — orthogonal to "is the enum class identifier already
  known".
- **Enum classes never carry const FirProperty members directly.** Their entries are
  `FirEnumEntry`. Code that walks `firClass.declarations.filterIsInstance<FirProperty>()`
  looking for a const named like the entry will always come up empty. Detecting this
  shape upfront skips a list allocation and a top-level symbol probe per
  enum-typed annotation argument — meaningful on java-direct's path where the same
  fallback runs (`couldBeConstReference = true`).
- **Branches guarded by `classifier == null` / `source == null` cannot affect the PSI
  path.** Several reverted blocks in `FirJavaFacade` (`setSealedClassInheritors` cross-
  file lookup, `enumEntriesOrigin` for source enums, `isPrimary` for source records,
  the whole `null`-classifier branch in `JavaTypeConversion`) only fire for java-direct.
  Those should not be searched for the source of a PSI-only regression.

### Follow-ups not in this iteration

- Re-measure on `IntelliJFullPipelineTestsGenerated` (Java-heavy, ~10× annotation
  density vs. Kotlin pipeline). The two follow-up gates
  (`needsTypeUseAnnotationFiltering`, `supportsExternalInitializerResolution`) showed no
  measurable benefit on the Kotlin pipeline; their per-call cost is small and may need a
  larger / annotation-heavier corpus to surface in single-rep timing.
- Multi-rep run (3+ reps each) of all four configurations to tighten the noise envelope
  below ±1%.
- The same `resolveConstFieldValue` runs on java-direct's path (`couldBeConstReference =
  true`); for further tightening of the java-direct/PSI gap on this code path, look at
  caching the `(classId, fieldName) → constValue?` lookup at the session level — most
  call traffic is for a small set of well-known JDK enum entries that produce the same
  null answer many times over.

---

## Test framework wiring: java-direct AST was never used — 2026-04-28

### Overview

Follow-up on the "Coverage gap…" entry below. Investigating why
`testSealedJavaCrossFilePermits` failed with the FIR fix in place, instrumentation
revealed that `JavaUsingAstPhasedTestGenerated` did **not** route `// FILE: *.java`
blocks through java-direct's AST at all. The 7 placeholder tests passed for the same
reason: every Java class was loaded via PSI (`JavaClassFinderImpl`), so the
`classifier == null` branches in the four shared FIR files were never exercised.

After two infrastructure fixes (and a small `JavaPackageIndexer` extension), all 8
tests now actually drive java-direct, and the suite is **2793/2793** green.

### Root cause #1 — scope filter rejected directory source roots

`VfsBasedProjectEnvironment.getFirJavaFacade` passed a `findLocalFile` callback to
`JavaClassFinderFactory.createJavaClassFinder` that filtered through
`psiSearchScope.contains(vf)`:

```kotlin
{ localFs.findFileByPath(it)?.takeIf { vf -> psiSearchScope.contains(vf) } }
```

For the `<main>` module the scope is `AllJavaSourcesInProjectScope`, whose
`contains(file)` rejects directories (line 18: `(extension == "java" || ...) && !isDirectory`).
The factory uses the callback to resolve `configuration.javaSourceRoots` — *directory*
paths — so every entry came back `null`, the factory found 0 source roots, and fell
back to `defaultFinderProvider()` (PSI).

For `<regular dependencies of main>` the scope is `librariesScope` (no directory
check), so the dependency session got `CombinedJavaClassFinder` — but that session
never resolves user-Java classes referenced from Kotlin source.

**Design issue:** the `findLocalFile` callback conflated two things: path-to-VirtualFile
resolution (which can target a directory) and scope membership (defined at the
`.java`-file level). The PSI-based finder doesn't have this issue — it applies scope
inside its class-lookup methods, never to source-root paths.

**Fix (refactor):** drop `findLocalFile` from `JavaClassFinderFactory` API entirely.
The factory implementation resolves source-root paths directly via
`localFs.findFileByPath`. If an implementation needs class-file scope filtering, the
existing `scope` parameter is still there.

### Root cause #2 — package indexer rejected files whose disk path didn't match `package`

After fix #1, `J` from `testDottedJdkNestedClassFqn` resolved as `JavaClassOverAst`
correctly. But `testWithUnitType` regressed: `JavaUtils.java` (declaring `package test;`)
written flat at `java-sources/main/JavaUtils.java` became invisible. javac is tolerant
(it places `.class` by declared package, ignoring source location), but
`JavaPackageIndexer.tryBuildFileEntry` enforces directory-mirrors-package and skipped
the file. The lookup for `<root>/JavaUtils` matched the directory but failed parse-time
(declared `test`); the lookup for `test.JavaUtils` walked `test/` which doesn't exist.

**Fix:** in `JavaPackageIndexer.init`, after the file-root scan, also scan each
directory root's top-level `.java` files. Files declaring a non-root package are
registered in `fileRootIndex` under their declared package — so they're discoverable
even when disk path doesn't mirror the package. Top-level files declaring the root
package are still picked up by the regular root walk, so we skip them here to avoid
duplicates. Real-world layouts (`src/main/java/com/example/`) have no `.java` files
at the top of the source root, so this scan is essentially free for non-test workloads.

### Root cause #3 — failing test data was self-inconsistent

`sealedJavaCrossFilePermits.kt` declared `Base` as `sealed class` (non-abstract). The
java-direct path correctly registered `Sub1`/`Sub2` as inheritors, but
`FirJavaFacade.isJavaNonAbstractSealed` set `true` for non-abstract sealed Java
classes; `FirWhenExhaustivenessComputer` then required `is Base` in addition to
`is Sub1, is Sub2`. The `when` in the test had only Sub1/Sub2, so it was non-exhaustive
regardless of inheritor registration.

**Fix:** change Base to `abstract sealed`. Now `isJavaNonAbstractSealed` stays false
and `is Sub1, is Sub2` is exhaustive — the test cleanly catches the regression.

### How we diagnosed it

`JavaClassFinderOverAstFactory.createJavaClassFinder` was being called twice (once
for `<regular dependencies>`, once for `<main>`) with different `psiSearchScope`
hashes. Tracing `findLocalFile` per source-root path showed `resolved=null` for the
java-sources directory in the `<main>` call but `resolved=<path>` for the `<deps>`
call — confirming the scope filter was the discriminator. Tracing
`FirJavaFacade.findClass` showed `classFinder=JavaClassFinderImpl` (PSI) for `<main>`,
not `CombinedJavaClassFinder` — so user Java classes never reached java-direct.

Don't trust "test passes" as evidence that java-direct ran. Verify by stack-trace or
by checking which `JavaClassFinder` the source session's `JavaSymbolProvider` ended up
with.

### Status update for the gap-test table

The 8 tests under `compiler/testData/diagnostics/tests/jvm/javaDirectGap/` now all
actually exercise java-direct's AST. With the FIR fixes in place: all 8 pass. With the
shared FIR files reverted, `testSealedJavaCrossFilePermits` is the confirmed regression
catcher (the original design intent). The other 7 are positive coverage for
java-direct AST paths that were previously untested.

### Files Modified

| File | Change |
|------|--------|
| `compiler/cli/src/.../extensions/JavaClassFinderFactory.kt` | Drop `findLocalFile: (String) -> VirtualFile?` parameter; clarify `scope`/`localFs` doc |
| `compiler/cli/src/.../VfsBasedProjectEnvironment.kt` | Stop passing the broken scope-filter lambda |
| `compiler/java-direct/src/.../JavaDirectPluginRegistrar.kt` | Resolve source roots via `localFs::findFileByPath` (no callback) |
| `compiler/java-direct/src/.../JavaPackageIndexer.kt` | Pre-index top-level `.java` files declaring non-root packages |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/sealedJavaCrossFilePermits.kt` | `sealed` → `abstract sealed` so the `when` is exhaustive without `is Base` |

### Test Results

`./gradlew :kotlin-java-direct:test` — 2793 tests, 0 failures. (Up from 2679/2681
because the 8 javaDirectGap tests now run, and `JavaUsingAstPhasedTestGenerated`'s
existing tests are also routed through java-direct AST instead of PSI.)

### Key Learnings

- **`JavaUsingAstPhasedTestGenerated` did NOT exercise java-direct before this fix.**
  The pluggable `JavaClassFinderFactory` was registered, the AST finder was even
  *constructed*, but for the `<main>` module its source roots were filtered out and
  the factory fell back to PSI. Existing 1454 phased + 1168 box tests were green
  through pure PSI paths — they validated FIR behavior, not java-direct.
- **API design: don't conflate path resolution with class-scope membership.** The
  `findLocalFile` callback's contract was "scope-restricted path resolution", but
  `AbstractProjectFileSearchScope` is a class-file scope, not a path scope. Source
  roots are directories; passing them through a class-scope filter is meaningless.
- **java-direct's package indexer assumed standard Java layout.** Test frameworks
  often write files flat regardless of `package` declaration. javac is tolerant about
  this; java-direct now is too, for top-level files of a directory root.
- **Verifying that a test exercises java-direct requires instrumentation.** Stack
  trace `JavaSymbolProvider.classCache` lookups, check the `classFinder` field on
  `FirJavaFacade`. Tests passing or failing is not evidence of which finder served
  the classes.

---

## Coverage gap: shared FIR regressions invisible to java-direct suite — 2026-04-28

### Overview

Investigation of why the java-direct suite (1168/1168 box, 1454/1456 phased) stayed
green while `KotlinFullPipelineTestsGenerated` started failing 57 modules after the
shared FIR files (`FirJavaFacade.kt`, `JavaTypeConversion.kt`, `javaAnnotationsMapping.kt`,
`ConeRawScopeSubstitutor.kt`) were dropped from a clean-branch cherry-pick.

### Root cause of the coverage gap

The shared FIR files contain java-direct-specific branches that fire only when
`JavaClassifierType.classifier == null` (i.e. the type points outside java-direct's
source index — JDK, library binaries, sibling source files not yet indexed at the
time of access). The java-direct test data in
`testData/codegen/box{,Jvm}` and `testData/diagnostics/...` overwhelmingly references
classes that ARE in the same `// FILE:` group, so java-direct resolves their classifier
locally and the new branches never run. Real-world Kotlin modules
(`KotlinFullPipelineTestsGenerated`) compile a single Java source set that references
many types from JARs on the classpath — that is where `classifier == null` is the rule
rather than the exception.

Empirical evidence: `analysis-api-impl-base` failed with
`MISSING_DEPENDENCY_CLASS: Cannot access class 'List'` on a Kotlin call to a Java
method whose return type is `java.util.List<String>` (star-imported in `JdkClassFinder.java`).
The classifier is null in java-direct's model; the reverted FIR `null` branch
collapses to `ClassId.topLevel(FqName(classifierQualifiedName))` and drops every
type argument, raw-type inference, nested-FQN split, and inherited-inner-class lookup.

### Changes

Added a new test data directory `compiler/testData/diagnostics/tests/jvm/javaDirectGap/`
with 8 phased/diagnostic tests targeting individual shared-FIR branches:

| File | Targets | Status with reverted FIR |
|------|---------|--------------------------|
| `sealedJavaCrossFilePermits.kt` | `FirJavaFacade.setSealedClassInheritors` cross-file `permits` (classifier == null branch) | **FAILS** — `NO_ELSE_IN_WHEN` because inheritors aren't registered |
| `nonAbstractSealedJava.kt` | `FirJavaFacade.isJavaNonAbstractSealed` flag | passes (path not exercised by current Kotlin code) |
| `javaRecordExplicitCanonicalConstructor.kt` | `FirJavaFacade.isCanonicalRecordConstructorForSource` (source-based finder) | passes (test infra still uses javac for record bytecode) |
| `javaConstFieldFromKotlinTopLevel.kt` | `FirJavaFacade.lazyInitializer` cross-language const callback | passes (annotation arg path not strict enough) |
| `javaUtilStarImportList.kt` | `JavaTypeConversion` null-classifier raw/type-arg path for `java.util.*` star-import | passes (test infra resolves via binary classpath) |
| `dottedJdkNestedClassFqn.kt` | `JavaTypeConversion.findClassIdByFqNameString` for `java.util.Map.Entry` | passes (binary classpath fallback) |
| `inheritedInnerFromKotlinSupertype.kt` | `JavaResolutionContext.resolveFromLocalScope` inherited-inner walk | passes (java-direct's own inheritance walk handles it) |
| `javaTypeUseAnnotation.kt` | `JavaTypeConversion.filterTypeUseAnnotations` callback | passes (filtering not observable in this scenario) |

The first one — cross-file sealed permits — is a confirmed regression catcher: it
fails today with the reverted FIR code, and will turn green once the
`setSealedClassInheritors` branch handling `classifier == null` is restored.

### Files Modified

| File | Change |
|------|--------|
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/sealedJavaCrossFilePermits.kt` | New: 3 sibling Java sources with `sealed permits`, plus Kotlin `when` |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/nonAbstractSealedJava.kt` | New: non-abstract sealed Java class |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/javaRecordExplicitCanonicalConstructor.kt` | New: Java record with explicit canonical constructor |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/javaConstFieldFromKotlinTopLevel.kt` | New: Java field initialized via `KConstsKt.FOO` |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/javaUtilStarImportList.kt` | New: Java star-import of `java.util.*`, `List<String>` and `Map.Entry` round trip |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/dottedJdkNestedClassFqn.kt` | New: Java method using `java.util.Map.Entry<...>` via dotted FQN |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/inheritedInnerFromKotlinSupertype.kt` | New: Java class extending Kotlin class, referencing inherited inner by simple name |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/javaTypeUseAnnotation.kt` | New: Java method with `@Target(TYPE_USE)` annotation on parameter and return |

The new tests are auto-picked up by `JavaUsingAstPhasedTestGenerated` (under
`Tests > Jvm > JavaDirectGap`) and by the PSI phased runner (which currently
ignores them — they're additional coverage for both).

### Test Results

`./gradlew :kotlin-java-direct:test --tests "*JavaDirectGap*"` — 8 tests run, 7 pass,
1 fails (`testSealedJavaCrossFilePermits`, as designed to catch the regression).

### Key Learnings

- **Test-data filter is necessary but not sufficient.** Including a file based on
  presence of `// FILE: *.java` matches the right shape but doesn't guarantee the
  scenarios reach java-direct-specific FIR branches. The dominant case in test data
  is "all referenced Java types live in sibling `// FILE:` blocks", which keeps
  classifier non-null and routes through the well-trodden `JavaClass` branch.
- **Cross-source-file references inside one module** (java-direct's "classifier == null"
  case) is the gap: Sub1 referenced from Base.java when both are in the same source
  set but processed at different times. The new sealed-permits test exercises exactly
  that.
- **Some scenarios that *should* fail with the reverted FIR don't, in our test infra.**
  Examples (records, type-use annotations, star-import generics) appear handled by
  binary-classpath fallback or by the test framework's javac step; they need a
  modularized-tests-style two-module setup to force binary loading. This is a known
  follow-up — the failing test plus the placeholder tests are still useful as
  documentation of the intended scenarios.
- **`MISSING_DEPENDENCY_CLASS` and `NO_ELSE_IN_WHEN` are good signals.** Both fire
  late in the FIR pipeline once a type's symbol can't be located; phased diagnostic
  tests surface them as `IllegalStateException` from the
  `NoFirCompilationErrorsHandler`. Watch for these strings when triaging future
  shared-FIR regressions.

### Follow-ups (not in this iteration)

- Lift `boxModernJdk/testsWithJava17/sealed` and `boxModernJdk/testsWithJava17/records`
  into `JavaUsingAstBoxTestGenerated` (currently excluded from the test data roots
  in `compiler/java-direct/testFixtures/.../TestGenerator.kt`). Sealed and record
  tests there have inline Java FILE blocks but go through a JDK-17-specific code
  path the java-direct generator doesn't currently cover.
- Investigate why the 4 placeholder tests don't trigger the reverted-state failure
  in our infra. If they truly can't, consider a small two-module fixture where the
  Java side is compiled to bytecode and re-fed to a second module — that mimics
  the real-world classpath scenario the modularized tests exercise.

---

## Post-refactoring review: readability cosmetics — 2026-04-22

### Overview

Independent code review (`implDocs/reviews/r1.md`) cross-checked against the completed
Phases A-E and Phase B regression reversals. Six low-risk readability items from the
review's suggestions 2-7 were applied.

### Changes

- **Trim `rawTypeNameParts` KDoc** (`JavaTypeOverAst.kt`) — replaced the inline "83%"
  measurement detail with a concise one-liner; the data lives in
  `archive/MEASUREMENTS_2026_04_22.md` §7.4.
- **Trim `CacheHelpers.kt` file KDoc** — consolidated the "why not `by lazy(PUBLICATION)`"
  and "why not explicit backing fields" rationale from 31 lines to 19, preserving the key
  insight (24B+8B per delegate × 200K instances).
- **Rename `findInPhase1JavaModel` → `walkJavaSourceSupertypes`** and
  **`findInPhase2ClassIdWalk` → `walkBinarySupertypes`** (`JavaInheritedMemberResolver.kt`)
  — the old "Phase 1 / Phase 2" names read as compilation phases; the new names describe
  the data source (Java model vs binary/Kotlin supertypes). Updated all KDoc references.
- **Rename `AggregatedInheritedInnerClassesHolder` → `InheritedInnerCache`** and
  **`aggregatedInheritedInnerClassesHolder` → `inheritedInnerCache`**
  (`JavaResolutionContext.kt`) — shorter name for the `@Volatile`-wrapped cache holder.
- **Add comment on `JavaAnnotationOverAst.resolve()`** — one-liner explaining that
  resolution is callback-based via `resolveAnnotation()`.
- **Trim static-inner-class context comment** (`JavaClassOverAst.kt:162-167`) — removed the
  3-line mirror explanation of the `else` branch; the first two lines already explain the
  non-static case and the code is self-evident.

### Test Results

No behavioral changes — renames and comment edits only. Compilation verified via IDE.

### Files Modified

| File | Change | Lines |
|------|--------|-------|
| `JavaTypeOverAst.kt` | Trim `rawTypeNameParts` KDoc | −1 |
| `CacheHelpers.kt` | Trim file-level KDoc | −12 |
| `JavaInheritedMemberResolver.kt` | Rename two methods + update KDoc | ~0 (rename) |
| `JavaResolutionContext.kt` | Rename class + field | ~0 (rename) |
| `JavaAnnotationOverAst.kt` | Add one-liner on `resolve()` | +1 |
| `JavaClassOverAst.kt` | Trim inner-class context comment | −3 |
| **Net** | | **−15 lines** |

### Key Learnings

- **Review after refactoring catches different things than review before.** The original
  review (r1.md) independently flagged `filterTypeUseAnnotations` caching and
  `resolveSimpleNameToClassIdImpl` extraction — both of which had already been tried and
  reverted (P1 and R12+O10). Cross-checking against the refactoring history before acting
  avoided re-introducing known regressions.
- **Method names that describe mechanism ("Phase 1/2") age worse than names that describe
  data source ("JavaSource/Binary").** The Phase 1/Phase 2 naming was clear when the two
  methods were freshly extracted in B.3 but confusing to a fresh reader.

---

## Archived Iteration History

All entries from the 2026-04-17 through 2026-04-22 refactoring work (Phases A-E of
`REFACTORING_PLAN_2026_04_21.md`) have been archived to:

- `implDocs/archive/ITERATION_RESULTS_2026_04_22.md` — full log with Phase B regression
  investigation, Phase C measurements, Phase D implementation, Phase E cleanup
- `implDocs/archive/REFACTORING_PLAN_2026_04_21.md` — the 5-phase plan (A-E)
- `implDocs/archive/MEASUREMENTS_2026_04_22.md` — Phase C measurement data (8 hypotheses,
  3 corpora, corrected classloader-isolation methodology)
- `implDocs/archive/REFACTORING_STEPS_2026_04_17_DETAILS.md` — earlier refactoring steps 1.3-3.6
- `implDocs/archive/LAZY_PACKAGE_INDEXING_PLAN_2026_04_21.md` — lazy per-package indexing design (implemented)

### Open items carried forward

- **Context-level `tryResolve` cache** (`PERFORMANCE_REVIEW_2026-04-20.md` §2 #6) — deferred
  with a recorded correctness argument. Only revisit if profiling shows `resolve()` as a
  measurable bottleneck.
