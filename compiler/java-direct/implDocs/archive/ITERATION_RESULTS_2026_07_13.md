# Java-Direct: Iteration Results 2026-06-09 → 2026-07-13 (Archived)

**Archive Date**: 2026-07-13
**Coverage**: dated entries from 2026-06-09 through 2026-07-13 — the supertype
cycle-breaker minification, real `source` for `*OverAst` FIR declarations, the
model-side inherited-outer-arg recovery relocation (+ reviewer follow-ups), the
sealed-`permits` JLS 8.1.6 / 9.1.4 parity work, the full 6-step resolution-pipeline
collapse (`COLLAPSE_RESOLUTION_PIPELINES_2026_07_06.md`, all landed), the
`review.md` responses / reviewer Q&A, and the JSpecify TYPE_USE field-annotation fix.
**Result**: box 1178/1178 + phased 1660/1660 (2838/2838, 100%) at archive close. 0 known won't-fix.

> **Warning**: This document is archived for historical reference. Only consult it
> if you need the implementation decisions behind the resolution-pipeline collapse,
> the model-side outer-arg recovery, the sealed-`permits` parity work, the cycle-breaker
> minification, or the reviewer Q&A responses. For the authoritative current state see
> `AGENT_INSTRUCTIONS.md` and the live `ITERATION_RESULTS.md`.

---

### 2026-07-13 — Attach TYPE_USE annotations to field types (JSpecify warn-mode enhancement)
- **Change**: `JavaFieldOverAst.computeType()` built the field type with plain `createJavaType`,
  dropping the field's modifier-list annotations, so JSpecify `@Nullable`/`@NonNull` TYPE_USE
  annotations never reached the field type. In `JSPECIFY_STATE: warn` the flexible field type then
  rendered without its `@Nullable()`/`@NonNull()` type annotations and the
  `RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS` diagnostic was missing. Now uses
  `createJavaTypeWithAnnotations` (the same path `JavaMethodOverAst.returnType` already uses), which
  TYPE_USE-filters the member annotations onto the type — matching PSI's `PsiField.type.annotations`.
- **Files**: `model/JavaMemberOverAst.kt` (`computeType`, 1 line + comment).
- **Tests**: box 1178/1178, phased 1660/1660 (full suite green); previously failing
  `SmartCasts.Kt87278.testEnhancedNotNullWarn` / `testEnhancedNullableWarn` now pass.
- **Result**: regression fixed (new master tests); green.

### 2026-07-10 — Document why the KT-74097 guard is kept over a lazy-annotation fix (JavaCycleBreakerTest reviewer Q)
- **Change**: Recorded the answer to the recurring reviewer question on `JavaCycleBreakerTest.kt`'s
  KT-74097 note ("wouldn't it help to stop resolving annotations while computing
  `FirJavaClass.declarations`? we already have `FirLazyJavaAnnotationList` for that"). The hint is
  correct about the *trigger* — the enum-entry arm of `convertJavaFieldToFir` / `buildEnumEntry`
  resolves annotations eagerly (`setAnnotationsFromJava` + `replaceDeprecationsProvider`), unlike
  other Java members that defer via `FirLazyJavaAnnotationList` — but it is not a local
  `java-direct` fix: `FirEnumEntry` has no lazy-annotation slot, `buildEnumEntry` lives in shared
  `fir-jvm` (compiler-wide ordering knock-ons), and laziness would remove only this trigger, not the
  cycle class the chokepoint guard defends. Conclusion documented: the `cycleSafeClassLikeSymbol`
  guard stays; enum-entry annotation laziness is an upstream KT-74097 follow-up.
- **Files**: `AGENT_INSTRUCTIONS.md` (KT-74097 Critical Pattern bullet extended with the Q&A).
- **Tests**: none (documentation-only).
- **Result**: green; no production/behavior change.

### 2026-07-10 — Document why computeClassifier's in-scope navigation loop is not redundant (review comment #9)
- **Change**: Re-investigated review comment #9 ("doesn't `resolve(rawTypeName)` already handle the
  same-scope case?"). Confirmed empirically that removing the explicit `findClassInCurrentScope` +
  `declaredOrFullyInherited` multi-part loop keeps the box + phased suite (2793) green **but** breaks
  4 `JavaParsingTypeResolutionTest` unit tests (`Outer.Inner`, `a.b`, `SimpleFunctionDescriptor.CopyBuilder`,
  + sibling): those parsing fixtures have no `FirSession`, and `resolve`'s existence probe needs the
  session's symbol provider (`cycleSafeClassLikeSymbol`), so the AST/model walk is the only path that
  resolves same-file / in-scope references there (and it also saves a symbol-provider round-trip per
  segment when a session is present). Kept the loop; replaced its comment with this concrete,
  test-cited reason.
- **Files**: `model/JavaTypeOverAst.kt` (`computeClassifier` loop kept + reason documented);
  `implDocs/REVIEW_MD_RESPONSES_2026_07_08.md` §9 + table + summary; `RESOLUTION_SCHEMA.md` Scenario A.
- **Tests**: `JavaParsingTest` + `JavaParsingTypeResolutionTest`, box + phased — all green, 0 failures.
- **Result**: green; no production behavior change (comment/doc-only after the round trip).

### 2026-07-09 — Pin the javac type-param-vs-nested-class divergence (review comment #6) with a test
- **Change**: Review comment #6 (own type parameter `T` shadows a same-named nested `class T`,
  diverging from javac) was accepted/out-of-scope but only described in prose. Confirmed via
  `javac` that the divergence is real (javac binds the bare `T` to the nested class `a.x.T`),
  established there was no compiler-wide test pinning the *own-declared* nested-class case (only
  the inherited-case sibling `InheritedInnerAndTypeParameterWithSameNames.kt` existed), and added
  one. It is a shared diagnostics test, so it runs both compiler-wide and in this module's phased
  suite. Verified the reviewer's proposed reordering (nested class before type parameter) makes it
  fail with `UNRESOLVED_REFERENCE ... on receiver of type 'x.T'`, then reverted. No production
  behavior changed.
- **Files**: new `testData/diagnostics/tests/javac/typeParameters/OwnNestedClassAndTypeParameterWithSameNames.kt`;
  `model/JavaTypeOverAst.kt` (`computeClassifier`'s `findTypeParameter` step now references both
  pinning tests inline — comment-only); `implDocs/REVIEW_MD_RESPONSES_2026_07_08.md` §6 + table.
- **Tests**: new test green in `JavaUsingAstPhasedTestGenerated`; fails as expected under the
  temporary flip, passes after revert.
- **Result**: green.

### 2026-07-09 — Fix explicit-import nested-class mis-split on the reentrance-safe path
- **Change**: `resolveFromExplicitImport` used the last-dot `ClassId.topLevel` split in the
  reentrance-safe (`fullResolution = false`) flavor, so an explicitly imported *nested* class
  used as a supertype (`import a.b.Outer.Middle; class C extends Middle`) mis-split as package
  `a.b.Outer` / class `Middle` and its inherited inner classes failed to resolve. Now always uses
  `resolveAsClassId` (reentrance-safe: it only probes existence, never re-enters inherited
  lookup), matching the static/star import steps. Closes review comment #2.
- **Files**: `JavaTypeResolver.kt` (unify one import branch + KDoc); new box test
  `codegen/box/javaDirect/inheritedInnerClassFromExplicitlyImportedNestedSupertype.kt`.
- **Tests**: box + phased + `JavaParsingTest` green, 0 failures; new box test fails before / passes after.
- **Result**: green.

### 2026-07-08 — Drop walkSupertypeClassIds' now-redundant lambda parameters
- **Change**: `walkSupertypeClassIds` had exactly one call site
  (`resolveInheritedInnerClassToClassId`), which always passed the same
  `directSupertypeClassIds`/`tryResolveInherited` closures and a fresh `visited` set — so its
  5-parameter signature carried no real flexibility. Gave it a `JavaResolutionContext` context
  receiver and dropped the `directSupertypeClassIds`/`tryResolve`/`visited` parameters: it now
  reads `tryResolveInherited`/`directSupertypeClassIds` off the context directly and initializes
  `visited` internally.
- **Files**: `resolution/JavaInheritedClassResolver.kt` (`walkSupertypeClassIds` signature +
  KDoc); `RESOLUTION_SCHEMA.md` updated.
- **Tests**: `:compiler:java-direct:compileKotlin` succeeds (signature-only change, no test
  call sites — `walkSupertypeClassIds` is private).
- **Result**: green. No production function in this file still takes injectable lambdas.

### 2026-07-08 — Drop the 3-lambda-parameter test-only overload of resolveInheritedInnerClassToClassId
- **Change**: The 5-parameter generic `resolveInheritedInnerClassToClassId(simpleName, tryResolve,
  directSupertypeClassIds, containingClass, resolveWithoutInheritance)` had exactly one production
  caller (the 2-parameter context-bound wrapper) — its 3 lambda parameters existed only so a unit
  test could inject fakes to verify the level-1 raw-AST-text safety invariant. Merged the two
  overloads into one context-bound function taking only `simpleName`/`containingClass`, binding
  `tryResolveInherited`/`directSupertypeClassIds`/the reentrance-safe name resolver directly via the
  context receiver. Replaced the removed unit test with an end-to-end diagnostics test exercising
  the real hazard: an unqualified reference inside a class's own `implements` clause to its own
  nested class, inherited two supertypes up — the companion of the existing qualified-reference
  regression test.
- **Files**: `resolution/JavaInheritedClassResolver.kt` (overloads merged; `walkSupertypeClassIds`
  unchanged); `test/.../JavaParsingTypeResolutionTest.kt` (old unit test + unused imports removed);
  new `testData/diagnostics/tests/jvm/javaDirect/simpleInheritedNestedClassInOwnImplementsClause.kt`;
  `RESOLUTION_SCHEMA.md` updated.
- **Tests**: `:compiler:java-direct:compileKotlin`/`compileTestKotlin` succeed; full
  `:compiler:java-direct:test` suite green, 2836/2836 (0 failures) — same count as before (one
  removed unit test, one added diagnostics test).
- **Result**: green. Production signature now has no injectable lambdas left.

### 2026-07-08 — Convert JavaInheritedMemberResolver to top-level functions; rename file
- **Change**: The class had no instance state left after the earlier collapses, so its members
  became top-level functions in a renamed `JavaInheritedClassResolver.kt` (it resolves classes,
  not "members"). Dropped the now-unneeded `JavaFileContext.inheritedMemberResolver` field.
  Eliminated the `tryResolve` parameter from the context-bound `resolveInheritedInnerClassToClassId`
  overload and the `resolveInherited`/`classifierAdapterFor` parameters from
  `findInnerClassFromSupertypes` — every production and test call site built the same closures, so
  they're now hardcoded via the context receiver instead of injected.
- **Files**: `resolution/JavaInheritedMemberResolver.kt` → `resolution/JavaInheritedClassResolver.kt`;
  `resolution/JavaFileContext.kt`, `resolution/JavaResolutionContext.kt`,
  `resolution/JavaScopeResolver.kt`, `resolution/JavaTypeResolver.kt` (call sites + KDoc refs
  updated; `resolveQualifiedNameToClassIdFromParts`/`resolveSimpleNameToClassIdImpl` widened to
  `internal`); `resolution/LeanJavaClassFinder.kt`, `model/JavaClassOverAst.kt` (KDoc refs);
  `test/.../JavaParsingTestBase.kt`, `JavaParsingTypeResolutionTest.kt` (tests call the top-level
  functions directly); `RESOLUTION_SCHEMA.md`, `ARCHITECTURE.md` updated.
- **Tests**: `:compiler:java-direct:compileKotlin`/`compileTestKotlin` succeed; full
  `:compiler:java-direct:test` suite green, 2836/2836 (0 failures).
- **Result**: green. Pure structural cleanup — the 5-arg generic BFS entry point keeps all its
  parameters (still test-injected by `testResolveInheritedInnerClassToClassIdNeverQueriesContainingClassOwnSupertypeClassIds`).

### 2026-07-08 — Collapse the same-file arm; findInnerClassFromSupertypes is now one ladder
- **Change**: The same-file arm's remaining justification (works with no `LeanJavaClassFinder`/FIR
  session at all) was purely technical/testability — every production `JavaResolutionContext`
  has both. Removed `resolveSameFileSupertype` and the recursive same-file walk;
  `findInnerClassFromSupertypes` now just materializes `resolveInheritedInnerClassToClassId`'s
  result via `classifierAdapterFor`, so same-file, cross-file-source, binary Java, and Kotlin
  supertypes all go through one BFS with no representation-specific arm left.
- **Files**: `resolution/JavaInheritedMemberResolver.kt` (`resolveSameFileSupertype` and
  `sameFileTopLevelClassProvider` ctor param removed), `resolution/JavaResolutionContext.kt` /
  `resolution/JavaScopeResolver.kt` (call sites updated); `test/.../JavaParsingTestBase.kt` (new
  `SameFileOnlyClassFinder` test double replaces the removed provider for unit tests that need a
  finder-less same-file setup); `JavaParsingTypeResolutionTest.kt`
  (`testInheritedInnerClassFromNestedGenericSupertype` rewired onto the production BFS instead of
  stubbed callbacks; the other affected test only needed its constructor call updated);
  `RESOLUTION_SCHEMA.md`/`REVIEW_MD_RESPONSES_2026_07_08.md` updated.
- **Tests**: `:compiler:java-direct:test` full suite green, 2836/2836 (0 failures), confirmed via
  a forced `--rerun`.
- **Result**: green. The class-resolution code now has exactly one narrow exception left (the
  level-1 raw-AST-text read for cycle-safety in `resolveInheritedInnerClassToClassId`); the
  same-file/cross-file/binary/Kotlin split is fully collapsed.

### 2026-07-08 — Correct stale `collectInheritedInnerClasses` claim behind the same-file-arm split
- **Change**: Reviewer re-flagged the same-file arm in `findInnerClassFromSupertypes` as
  unconvincing and asked for a test / cross-check against the other arm. Verified the comment's
  premise — that the other arm ("`resolveInherited`") is backed by `collectInheritedInnerClasses`,
  which declines dotted references — is stale: since the earlier BFS merge, `resolveInherited` goes
  through `tryResolveInherited`/`directSupertypeClassIds`, which never calls
  `collectInheritedInnerClasses` and handles dotted references fine via the already-resolved
  `.classifier`. `collectInheritedInnerClasses` has no remaining production caller in this scenario
  (only its own public `LeanJavaClassFinder` method + tests). The split's real, still-load-bearing
  reason is that the same-file arm is the only part working with no class finder/FIR session at all
  (`testInheritedInnerClassFromNestedGenericSupertype`); a pre-existing test
  (`testInheritedInnerClassFromQualifiedNestedSameFileSupertype`) already covers the dotted
  same-file-supertype case and is now referenced from the code.
- **Files**: `resolution/JavaInheritedMemberResolver.kt` (KDoc rewrite, no logic change),
  `util/JavaSupertypeGraph.kt` (two stale KDoc/comment corrections),
  `REVIEW_MD_RESPONSES_2026_07_08.md` §13 corrected.
- **Tests**: comment-only change; `:compiler:java-direct:test` `JavaParsingTypeResolutionTest` green.
- **Result**: green. No behavior change; `collectInheritedInnerClasses`/`getDirectSupertypes` being
  dead in this scenario is flagged as a follow-up, not removed in this pass.

### 2026-07-08 — Fix findInheritedNestedClass's own cycle-guard-skip hazard; drop its now-redundant fallback
- **Change**: Closed the "third, narrower occurrence" flagged as a follow-up in an earlier entry:
  `resolveQualifiedNameToClassIdFromParts` had a `collectInheritedInnerClasses`-based fallback for
  when `findInheritedNestedClass` was cycle-guard-skipped on `outerClassId`'s own
  `directSupertypeClassIds`, but that fallback was itself source-only (the same cross-origin
  ambiguity blind spot fixed elsewhere this session) and restricted to `parts.size == 2`. Instead of
  keeping the compensating fallback, fixed `findInheritedNestedClass` itself: it now materializes
  `outerClassId` via `classifierAdapterFor` and delegates to
  `resolveInheritedInnerClassToClassId(nestedName, ..., containingClass)`, which reads
  `containingClass`'s own direct supertypes from raw AST text instead of the guarded
  `directSupertypeClassIds`, exactly like the already-fixed simple-name caller. This makes
  `findInheritedNestedClass` un-guard-skippable, so the fallback became dead code and was removed.
- **Files**: `resolution/JavaTypeResolver.kt` (`findInheritedNestedClass` rewritten, fallback block
  removed), `resolution/JavaInheritedMemberResolver.kt` (`resolveInheritedNestedClassId` removed —
  folded into the existing `resolveInheritedInnerClassToClassId`);
  `testData/diagnostics/tests/jvm/javaDirect/qualifiedInheritedNestedClassInOwnImplementsClause.kt`
  (new) exercises a real, `javac`-verified-legal shape: a class implementing a generic interface
  parameterized by its own inherited nested class.
- **Tests**: `:compiler:java-direct:test` full suite green, 2836/2836 (0 failures; 1 new).
- **Result**: green. `findInheritedNestedClass`'s qualified `Outer.Nested` lookup now shares the
  same raw-AST-text safety and origin-agnostic ambiguity detection as the simple-name path, instead
  of relying on a narrower, source-only compensating fallback.

### 2026-07-08 — Merge the classFinder-only fast paths into the origin-agnostic ladder; add j-k-j regression test
- **Change**: Reviewer asked for a reproduction of the same-level cross-origin ambiguity blind spot
  in a Java-Kotlin-Java (j-k-j) shape and a real fix rather than an accepted-limitation note. Found
  two independent classFinder-only fast paths that returned a single, non-ambiguous *source-only*
  candidate without ever checking a same-level Kotlin/binary competitor: (1)
  `resolveInheritedInnerForLevel` (`JavaTypeResolver.kt`, the `ClassId`-returning Step-1 dispatcher)
  used a cached `collectInheritedInnerClasses` map and returned on `candidates.size == 1`; (2)
  `findInnerClassFromSupertypes`'s separate `classFinder`-backed cross-file-source arm (already
  merged into the origin-agnostic ladder together with the binary/Kotlin tail earlier this
  session). Simplified (1) to always delegate to `resolveInheritedInnerClassToClassId` via the new
  `tryResolveInherited` (classFinder-first, FIR-fallback existence probe, reused from (2)'s earlier
  fix), removing the now-dead `getInheritedInnerClassesForClass` cache and
  `JavaScopeContext.InheritedInnerCache`. Also fixed a real ambiguity-walk bug in
  `walkSupertypeClassIds`: expansion to the next level was gated on a *global* `foundClassId`, so an
  unrelated sibling ancestor stopped being expanded as soon as any other ancestor matched at the
  same level, hiding a deeper conflicting match — now gated per-ancestor.
- **Files**: `resolution/JavaTypeResolver.kt` (`resolveInheritedInnerForLevel` simplified,
  `tryResolveInherited` added, dead cache removed), `resolution/JavaScopeContext.kt`
  (`InheritedInnerCache` removed), `resolution/JavaInheritedMemberResolver.kt`
  (`walkSupertypeClassIds` per-ancestor fix, `findInnerClassFromSupertypes` KDoc),
  `resolution/JavaScopeResolver.kt` (`declaredOrFullyInherited` uses `tryResolveInherited`);
  `testData/diagnostics/tests/jvm/javaDirect/ambiguousInheritedInnerClassAcrossSourceAndKotlinSupertypes.kt`
  (new).
- **Tests**: `:compiler:java-direct:test` full suite green, 2835/2835 (0 failures; 1 new).
- **Result**: green. A Java class inheriting the same nested-class name from an unrelated cross-file
  Java source ancestor and a Kotlin/binary ancestor now correctly fails to resolve
  (`MISSING_DEPENDENCY_CLASS`) in both resolution pipelines, instead of silently picking the
  source-only candidate.

### 2026-07-08 — Merge the source/binary supertype walks into one BFS; add reentrancy regression test
- **Change**: Reviewer follow-up on the two-pass split in `resolveInheritedInnerClassToClassId`
  (source-first, binary/Kotlin-deferred), which masked cross-origin ambiguity at depth. Merged
  `walkJavaSourceSupertypes` and `walkSupertypeClassIds` into one origin-agnostic BFS, keeping only
  the one load-bearing exception: `containingClass`'s own direct supertypes are still read from raw
  AST text via `resolveWithoutInheritance`, because `containingClass`'s own `SUPER_TYPES` FIR phase
  can still be on the call stack (resolving a name written inside its own extends/implements
  clause). Every ancestor beyond that first level now goes through `directSupertypeClassIds`
  uniformly, so same-depth source/Kotlin/binary candidates are compared for ambiguity together
  instead of the source pass short-circuiting first. Tried a diagnostics-test reproduction first,
  but real `javac` itself rejects referencing a self-inherited member type from within a class's own
  extends/implements clause (confirmed empirically) — no valid Java program can exercise a
  successful match there. Added a direct unit test instead
  (`testResolveInheritedInnerClassToClassIdNeverQueriesContainingClassOwnSupertypeClassIds`) that
  fails `directSupertypeClassIds` if ever invoked with `containingClass`'s own `ClassId`, while
  still expecting the walk to find a name inherited two levels up — referenced from the exception's
  KDoc/comment.
- **Files**: `resolution/JavaInheritedMemberResolver.kt`,
  `test/JavaParsingTypeResolutionTest.kt` (+1 test), `implDocs/RESOLUTION_SCHEMA.md`.
- **Tests**: `:compiler:java-direct:test` full suite green, 2834/2834 (0 failures; 1 new).
- **Result**: green. Closes the previously-documented cross-origin ambiguity blind spot in the
  `ClassId` pipeline.

### 2026-07-08 — Fold findInheritedNestedClass's DFS into the shared BFS; drop MAX_SUPERTYPE_DEPTH
- **Change**: Reviewer noted `findInheritedNestedClass` (in `JavaTypeResolver.kt`, the `Outer.Nested`
  qualified-name tail) used its own recursive DFS with no ambiguity detection, while
  `resolveInheritedInnerClassToClassId`'s supertype walk used BFS. Folded it into the same BFS:
  added `JavaInheritedMemberResolver.resolveInheritedNestedClassId`, seeding the renamed
  `walkSupertypeClassIds` (was `walkBinarySupertypes`) with the single starting `ClassId` instead of
  a containing-class's non-source supertypes. `findInheritedNestedClass` now delegates to it,
  gaining proper ambiguity detection (previously first-match-wins) and dropping its own
  `cycleGuardedSupertypeWalk` wrapper and the now-redundant `collectInheritedInnerClasses` fallback.
  Also dropped `MAX_SUPERTYPE_DEPTH` (both walk loops already terminate solely via their `visited`
  sets, bounded by the finite reachable `ClassId` set — the cap was pure defensive insurance).
- **Files**: `resolution/JavaInheritedMemberResolver.kt`, `resolution/JavaTypeResolver.kt` (comment
  + delegation), `resolution/JavaModelSessionAccess.kt` + `test/JavaCycleBreakerTest.kt` (stale
  comments), `implDocs/RESOLUTION_SCHEMA.md`.
- **Tests**: `:compiler:java-direct:test` full suite green, 2833/2833 (0 failures).
- **Result**: green. No new tests added — this is a mechanical fold of an existing search onto the
  existing generic BFS engine; ambiguity-detection coverage is exercised by pre-existing
  `MISSING_DEPENDENCY_CLASS` tests already run by the suite.

### 2026-07-08 — Regression tests + RESOLUTION_SCHEMA.md update (collapse step 6, final)
- **Change**: Step 6 (last) of the resolution-pipeline collapse. Added 3 new tests, each verified
  (by temporarily disabling the relevant fix) to genuinely depend on it, not pass vacuously:
  `testGetDirectSupertypesDoesNotTruncateQualifiedGenericSupertype` /
  `testClassifierAdapterForRoutesSourceBackedClassIdToCanonicalInstance` (unit,
  `JavaParsingClassFinderTest`), and a box test where a Java class's field type navigates
  `Local.Deeper.EvenDeeper` — `Local` same-file-declared, `Deeper`/`EvenDeeper` inherited from a
  *Kotlin* supertype two levels deep, exercising the structural pipeline's binary/Kotlin tail
  *and* the adapter's chained `findInnerClass` in one scenario. Rewrote `RESOLUTION_SCHEMA.md`
  Scenarios A/C/E (unified per-level lookup, identity routing, 3-arm inherited-inner-class
  resolver) and added the doc-only clarifications from review (telescoping-recursion argument,
  empty-name guard comment, javac priority-divergence and source/binary-ambiguity notes as
  accepted-elsewhere items).
- **Files**: `test/JavaParsingClassFinderTest.kt` (+2 tests);
  `testData/codegen/box/javaDirect/inheritedNestedClassFromKotlinSupertype.kt` (new);
  `implDocs/RESOLUTION_SCHEMA.md`; `resolution/JavaInheritedMemberResolver.kt` (comment),
  `model/JavaTypeOverAst.kt` (comment).
- **Tests**: `:compiler:java-direct:test` full suite green, 2833/2833 (0 failures; 3 new).
- **Result**: green. All 6 steps of `archive/COLLAPSE_RESOLUTION_PIPELINES_2026_07_06.md` landed.

### 2026-07-08 — Fix qualified-supertype truncation bugs, drop dead params (collapse step 5)
- **Change**: Step 5 of the resolution-pipeline collapse. Fixed two confirmed `substringBefore('<')`
  truncation bugs: `JavaSupertypeGraph.resolveSupertypeReference` now bracket-aware-splits the raw
  reference (`splitCanonicalFqName()`) before deciding it's a single, non-dotted simple name —
  previously `ref.substringBefore('<').trim()` truncated `a.B<String>.C`-shaped refs to `a.B`
  *before* checking dottedness, silently dropping `.C` and mis-treating it as a same-package/import
  candidate for `a.B`. Same fix applied to `JavaInheritedMemberResolver.walkJavaSourceSupertypes`'s
  `initialIds` computation. Also dropped two confirmed-dead parameters: `resolveWithoutInheritance`'s
  second (`resolve`) callback parameter (its sole caller always closed over the same `tryResolve`
  already in scope) and `includeOuterClasses`/its outer-walk loop on
  `resolveInheritedInnerClassToClassId` (always called with `false`; outer-class coverage is already
  provided by collapse-step-4's unified per-level loop in the caller). Simplified
  `JavaClassCache.parseTopLevelClassFromFile` to call `sameFileTopLevelClassProvider` directly
  instead of `findClassInCurrentScope` (behaviorally identical for its only call shape — a fresh
  context with `containingClass == null`).
- **Files**: `util/JavaSupertypeGraph.kt`, `resolution/JavaInheritedMemberResolver.kt`,
  `resolution/JavaTypeResolver.kt`, `resolution/JavaScopeResolver.kt`, `JavaClassCache.kt`.
- **Tests**: `:compiler:java-direct:test` full suite green, 2830/2830 (0 failures).
- **Result**: green. Step 6 (new regression tests for the newly-reachable paths, `RESOLUTION_SCHEMA.md`
  update) is tracked in `archive/COLLAPSE_RESOLUTION_PIPELINES_2026_07_06.md` for this session.

### 2026-07-06 — Unify per-level lookup in findClassInCurrentScope / computeClassifier (collapse step 4)
- **Change**: Step 4 (last of the requested 1-4) of the resolution-pipeline collapse. New
  `declaredOrFullyInherited(cls, name)` = `declaredOrSameFileInherited` (same-file) +
  `JavaInheritedMemberResolver.findInnerClassFromSupertypes` (cross-file source/binary/Kotlin, now
  capable per step 3). `findClassInCurrentScope`'s previously asymmetric 4-step ladder (full lookup
  only at the innermost level, same-file-only for outer levels) collapses into one loop over
  `self, outerClass, outerClass.outerClass, ...` applying `declaredOrFullyInherited` at every
  level. `JavaTypeOverAst.computeClassifier`'s multi-part navigation loop now calls the same
  function per hop instead of the same-file-only helper, so an intermediate segment inherited from
  a cross-file/Kotlin/binary supertype navigates correctly (previously same-file only, despite the
  KDoc's unqualified claim).
- **Files**: `resolution/JavaScopeResolver.kt` (+`declaredOrFullyInherited`, unified
  `findClassInCurrentScope` loop), `model/JavaTypeOverAst.kt` (multi-part loop + KDoc).
- **Tests**: `:compiler:java-direct:test` full suite green, 2830/2830 (0 failures).
- **Result**: green. Steps 1-4 of `archive/COLLAPSE_RESOLUTION_PIPELINES_2026_07_06.md` are now
  landed; steps 5-6 (dead-parameter removal, truncation-bug fixes, new regression tests, doc
  updates) are tracked there for a follow-up session.

### 2026-07-06 — findInnerClassFromSupertypes gains a binary/Kotlin tail (collapse step 3)
- **Change**: Step 3 of the resolution-pipeline collapse. `JavaInheritedMemberResolver.findInnerClassFromSupertypes`
  — the structural `JavaClass`-returning pipeline, previously same-file + cross-file-source only —
  now falls through to a binary/Kotlin tail (only when the same-file/cross-file-source arms found
  nothing) that reuses `resolveInheritedInnerClassToClassId`'s generic `ClassId` BFS (exposed
  `internal`) and materializes the result via `classifierAdapterFor` (steps 1-2). A Java source
  class inheriting a nested class from a Kotlin or binary superclass is now visible through this
  structural entry point (`findClassInCurrentScope`), not just the `resolve()`/`ClassId` one.
- **Regression fixed during this step**: the new tail must not run when the cross-file-source arm's
  `collectInheritedInnerClasses` map already reports an *ambiguous* (`size > 1`) candidate for the
  name — falling through in that case let the tail's differently-shaped BFS silently pick one
  candidate, masking the ambiguity (caught by 3 pre-existing `MISSING_DEPENDENCY_CLASS` tests:
  `Clash.kt`, `InheritanceAmbiguity2.kt`, `InheritanceAmbiguity4.kt`).
- **Files**: `resolution/JavaInheritedMemberResolver.kt` (`findInnerClassFromSupertypes` signature
  + tail), `resolution/JavaScopeResolver.kt` (call site wiring), `resolution/JavaTypeResolver.kt`
  (`resolveInheritedInnerClassToClassId` made `internal`); test
  `JavaParsingTypeResolutionTest.kt` (updated call site, no-op callbacks for the parsing-only test).
- **Tests**: `:compiler:java-direct:test` full suite green, 2830/2830 (0 failures) after the
  ambiguity fix.
- **Result**: green.

### 2026-07-06 — classifierAdapterFor routes source-backed ClassIds to canonical JavaClassOverAst (collapse step 2)
- **Change**: Step 2 of the resolution-pipeline collapse. `classifierAdapterFor` no longer
  unconditionally builds a fresh `FirBackedJavaClassAdapter`. It first checks
  `classFinder.isClassInIndex(classId)` (the same check `directSupertypeClassIds` arm 1 already
  performs) and, if source-backed, returns the finder's canonical `JavaClassOverAst` via
  `finder.findClass(...)`. Fixes the identity break where a same-file/other-file source class
  reached via the generic `ClassId` ladder (e.g. `computeClassifier`'s `resolve()` fallback) came
  back as a second, non-navigable wrapper instead of the live, identity-preserving instance.
- **Files**: `resolution/JavaTypeResolver.kt` (`classifierAdapterFor`).
- **Tests**: `:compiler:java-direct:test` full suite green, 2830/2830 (0 failures).
- **Result**: green; routing-only change, no new FIR laziness.

### 2026-07-06 — FirBackedJavaClassAdapter gains real findInnerClass/innerClassNames (collapse step 1)
- **Change**: Step 1 of the resolution-pipeline collapse (see
  `archive/COLLAPSE_RESOLUTION_PIPELINES_2026_07_06.md`). `FirBackedJavaClassAdapter` — the uniform
  `JavaClass` façade over binary/Kotlin FIR targets — no longer hardcodes `findInnerClass`/
  `innerClassNames` to null/empty. `FirJavaClass` targets enumerate the now-public
  `existingNestedClassifierNames`; other `FirRegularClass` targets (Kotlin/deserialized) enumerate
  `declarations` (safe there; the KT-74097 hazard is `FirJavaClass`-specific). Existence is probed via
  `cycleSafeClassLikeSymbol`, no enhancement/phase-forcing.
- **Files**: `resolution/FirBackedJavaClassAdapter.kt` (+`findInnerClass`/`innerClassNames`/
  `nestedClassifierNames` helper); fir-jvm `java/declarations/FirJavaClass.kt`
  (`existingNestedClassifierNames` made public, mirroring `directSupertypeClassIds()`).
- **Tests**: `:compiler:java-direct:test` full suite green, 2830/2830 (0 failures).
- **Result**: green; this only adds a new capability, no caller wired to it yet (steps 2-3 do that).

### 2026-06-16 — Inherited inner type shadows an enclosing-declared one (JLS 6.4.1 parity)
- **Change**: A member type *inherited* by an inner class now shadows one merely *declared* in a
  lexically-enclosing class, matching PSI on the cross-file/binary/Kotlin path (baseline only did
  this for same-file supertypes). `findClassInCurrentScope` probes the containing class's
  supertype-inherited inners *before* the sibling/outer-declared step; `resolveFromLocalScope`
  replaces the "all-declared-then-one-aggregated-inherited" pass with a per-level interleave
  (declared then this level's inherited inners), keeping innermost-wins priority for the
  `resolve()` callers.
- **Files**: `resolution/JavaScopeResolver.kt`, `resolution/JavaTypeResolver.kt`
  (+`resolveInheritedInnerForLevel`, per-class `getInheritedInnerClassesForClass`),
  `resolution/JavaInheritedMemberResolver.kt` (+`includeOuterClasses`),
  `resolution/JavaScopeContext.kt` (per-class `inheritedInnerCache`); test
  `testData/.../javaDirect/inheritedInnerShadowsOuterDeclared.kt` (3 cases).
- **Tests**: java-direct phased 1523/1523 (0 failures) + full `:compiler:java-direct:test` green;
  new test passes on both `JavaUsingAstPhasedTestGenerated` and `PhasedJvmDiagnosticPsiTestGenerated`.
- **Result**: green; resolution-only change, no shared FIR touched. Test data shared with PSI; both
  engines agree on the JLS golden.

### 2026-06-12 — Implicit `permits` match is now resolution-based + lazy (PSI `isInheritor` parity)
- **Change**: `JavaClassOverAst.deriveImplicitPermittedTypes` no longer matches subtypes by raw
  `extends`/`implements` text. A candidate is permitted iff one of its *direct* declared supertypes
  **resolves** (`JavaClassifierType.classifier`) to this sealed type (compared by `fqName`), mirroring
  PSI's `isInheritor(this, checkDeep = false)`. This removes the textual false-positive (a sibling
  whose `Shape` resolves to a shadowing nested type) and false-negative (FQ/imported reference) gaps.
- **Key subtlety / recursion-safety**: resolution is **lazy** — only CLASS-node enumeration
  (`collectClassNodes` from the file root) is eager; node→`JavaClass` resolution and supertype
  resolution run inside the returned `Sequence`, so they fire only when FIR iterates the deferred
  `setSealedClassInheritors { ... }` provider, never while this type's own `permittedTypes` is on the
  stack — exactly why PSI also defers its `isInheritor` filter behind a `Sequence`.
- **Files**: `model/JavaClassOverAst.kt` (−`collectImplicitPermittedSubtypes`/`directSupertypeRefNamesOf`,
  +lazy resolution match + `collectClassNodes`), `test/JavaParsingModifiersAndSpecialClassesTest.kt`
  (+`testSealedImplicitPermitsMatchesByResolutionNotText`).
- **Tests**: java-direct `JavaUsingAstBoxTestGenerated` + `JavaUsingAstPhasedTestGenerated` green
  (0 failures, test task executed) + `JavaParsingModifiersAndSpecialClassesTest` 12/12.
- **Result**: green; model-only change, no shared FIR or test data touched.

### 2026-06-12 — Implicit `permits` now scans the whole compilation unit (JLS 8.1.6 / 9.1.4)
- **Change**: `JavaClassOverAst.deriveImplicitPermittedTypes` (sealed type, no `permits` clause) now
  recurses from the file root over **every** CLASS node — top-level siblings and member types at any
  depth — instead of only the sealed type's directly-nested members, matching PSI's
  `lazilyComputePermittedTypesInSameFile` (`SyntaxTraverser.psiTraverser(containingFile)`). Matching
  stays purely syntactic (raw `extends`/`implements` text vs simple/FQ name); a matched node is
  turned into a `JavaClass` via the file's `sameFileTopLevelClassProvider` + declared-only
  `findInnerClass` chain, so no supertype resolution is triggered (recursion-safe).
- **Key subtlety**: the synthetic compilation-unit root is itself typed `CLASS`, so the enclosing-chain
  walk must stop at the root (climb only while the parent is a CLASS *and* not the root) — otherwise
  `chain.first()` is the identifier-less root and resolution returns null for every match.
- **Files**: `model/JavaClassOverAst.kt` (+whole-file scan/`collectImplicitPermittedSubtypes`,
  `resolveSameFileClassNode`, `directSupertypeRefNamesOf`), `test/JavaParsingModifiersAndSpecialClassesTest.kt`
  (+`testSealedImplicitPermitsScansWholeCompilationUnit`).
- **Tests**: java-direct `JavaUsingAstBoxTestGenerated` + `JavaUsingAstPhasedTestGenerated` green
  (0 failures, test task executed) + `JavaParsingModifiersAndSpecialClassesTest` 11/11.
- **Result**: green; model-only change, no shared FIR or test data touched.

### 2026-06-11 — Fix fragile `substringBefore('.')` in the same-file supertype walk (reviewer concern)
- **Change**: `findInnerClassInSameFileSupertypes` (relocated walk) no longer takes only the first
  dot-segment of a supertype reference. New `resolveSameFileSupertypeRefToClass` navigates the full
  reference, reusing the module's own resolution: head via `findClassInCurrentScope`, tail via
  declared-only `findInnerClass`. Fixes the qualified-nested same-file case (`class x1 extends x.S`
  now resolves `x.S`, not just `x`) and makes package-qualified refs (`extends com.example.Base`)
  cleanly decline (head `com` isn't a class → owned by the cross-file / `ClassId` paths) instead of
  mistaking the package root for a class.
- **Key subtlety**: tail segments use declared-only `findInnerClass` (a written `x.S` names a
  concrete declared type), keeping the walk from re-entering the supertype recursion.
- **Files**: `resolution/JavaScopeResolver.kt` (−first-segment shortcut, +`resolveSameFileSupertypeRefToClass`),
  `test/JavaParsingTypeResolutionTest.kt` (+`testInheritedInnerClassFromQualifiedNestedSameFileSupertype`).
- **Tests**: java-direct `JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated` + all
  `JavaParsing*` unit tests green (0 failures, test task executed).
- **Result**: green; model/resolution-only change, no shared FIR or test data touched. The identical
  shortcut in `JavaInheritedMemberResolver.findInnerClassFromSupertypes` (resolved-`supertypes` arm)
  is left as-is — out of this concern's scope and on a separate path.

### 2026-06-11 — Make `JavaClassOverAst.findInnerClass` declared-only (reviewer contract concern)
- **Change**: `findInnerClass` now returns only directly declared member types, matching
  `JavaClassImpl` (PSI, `findInnerClassByName(name, false)`) and `BinaryJavaClass`
  (`ownInnerClassNameToAccess`). The recursion-safe same-file AST-text supertype walk was
  relocated out of the model into the resolution layer (`findInnerClassInSameFileSupertypes` +
  `declaredOrSameFileInherited`); use-sites (scope steps 1/2/4, multi-part type navigation)
  invoke it explicitly, preserving resolution order.
- **Key subtlety**: the relocated walk must resolve each supertype simple name within the
  *walked* class's own `resolutionContext` (not the caller's ambient context) — using the
  ambient context loops (`StackOverflowError`).
- **Files**: `model/JavaClassOverAst.kt` (−`findInnerClassInSupertypes`, +`directSupertypeRefNames`),
  `resolution/JavaScopeResolver.kt` (+walk/helper, rewire), `model/JavaTypeOverAst.kt` (multi-part hop),
  `resolution/JavaInheritedMemberResolver.kt` (KDoc), `test/JavaParsingTypeResolutionTest.kt`.
- **Tests**: java-direct `JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated` + all
  `JavaParsing*` unit tests green (0 failures).
- **Result**: green; model-only change, no shared FIR or test data touched.

### 2026-06-11 — Add static-boundary stop to inherited outer-arg recovery (reviewer concern)
- **Change**: `recoverInheritedOuterTypeArguments` in `JavaTypeResolver.kt` now stops at a `static`
  nested class along the lexical containing chain — a `static` class has no enclosing instance, so it
  severs the implicit-outer-arg chain (JLS), matching PSI's `getTypeParameters` static break and the
  model's lexical walk. Closes a latent PSI divergence (over-recovery on already-illegal code).
- **Key subtlety**: static-ness is read from the AST-backed source chain (`containingClass.outerClass`
  → `JavaClassOverAst.isStatic`), **not** `FirBackedJavaClassAdapter.isStatic` — the latter is a FIR
  heuristic that misreports a non-static inner of a *non-generic* outer (e.g. `J1`) as static (first
  attempt regressed `KJKComplexHierarchyWithNested`) and reports `true` for top-level classes.
- **Files**: `resolution/JavaTypeResolver.kt` (ClassId-chain walk → JavaClass-chain walk + static break).
- **Tests**: java-direct `JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated` +
  `JavaCycleBreakerTest` + `JavaParsingTest` green.
- **Result**: green; model-only change, no shared FIR or test data touched.

### 2026-06-11 — Reuse FIR's substitutor for inherited outer-arg substitution (reviewer "partial reuse")
- **Change**: `substituteTypeArgs` in `JavaTypeResolver.kt` now builds a real `ConeSubstitutor` via
  `substitutorByMap` and applies it with `substituteOrSelf` (mirroring FIR's `createSubstitutionForSupertype`),
  instead of the hand-rolled top-level-only rewrite. Fixes the latent nested-occurrence gap
  (`Super<List<X>>` → `Super<List<String>>`) and handles variance/star projections. The adapter-driven
  `findTypeArgsForClassInHierarchy` DFS is kept, so all supertype reads stay on the
  `cycleGuardedSupertypeWalk`/on-air cycle-safe path; the declaring class's params are still read via
  `cycleSafeClassLikeSymbol`.
- **Files**: `resolution/JavaTypeResolver.kt` (−naive rewrite, +`substitutorByMap`; −3 unused imports, +5).
- **Tests**: java-direct `JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated` +
  `JavaCycleBreakerTest` green.
- **Result**: green; model-only change, no shared FIR or test data touched.

### 2026-06-10 — Relocate inherited-inner outer-arg recovery from shared FIR into the model
- **Change**: Give `FirBackedJavaClassAdapter` a real on-air-resolved `supertypes` chain (mirroring
  `FirJavaElementFinder.resolveSupertypesOnAir`) and move the implicit-outer-class-type-argument
  recovery for bare inherited inner-class refs into `JavaClassifierTypeOverAst.computeTypeArguments`;
  delete the java-direct-specific recovery (`outerTypeArgs` branch + 3 helpers) and the
  `containingClassSymbol` side-channel from shared FIR. Design: `archive/MODEL_SIDE_OUTER_ARG_RECOVERY_2026_06_10.md`.
- **Files**: `resolution/FirBackedJavaClassAdapter.kt` (real `supertypes` + on-air resolver),
  `model/JavaTypeOverAst.kt` (+`FirBackedJavaClassifierType`/`FirBackedJavaWildcardType`/`firBackedJavaType`),
  `resolution/JavaTypeResolver.kt` (+`recoverInheritedOuterTypeArguments` + cone walk/substitute);
  fir-jvm `java/JavaTypeConversion.kt` (−recovery branch, −3 helpers, −4 imports) /
  `MutableJavaTypeParameterStack.kt` (−`containingClassSymbol`) / `FirJavaFacade.kt` (−setter).
- **Tests**: java-direct phased+box + `JavaCycleBreakerTest` + `JavaParsingTest` green; PSI gate
  `PhasedJvmDiagnosticLightTreeTestGenerated.*` green; `CompileKotlinAgainstKotlin` gate green.
- **Result**: green; no public Java-model interface member added (rule 7) — all new types are model-private.

### 2026-06-10 — Populate real `source` for java-direct FIR declarations
- **Change**: java-direct `*OverAst` elements now carry a real, AST-backed `KtLightSourceElement`
  (reaching parity with the PSI loader) instead of `null`. Added a `JavaLightTree` →
  `FlyweightCapableTreeStructure<LighterASTNode>` adapter (`JavaLightTreeStructure` + `JavaLightAstNode`,
  shared non-registering placeholder `IElementType`), wired via a fir-jvm-owned seam interface
  `JavaDirectSourceElementOwner` implemented by `JavaElementOverAst`; `FirJavaFacade.toSourceElement()`
  falls through to it. Reverted the enum-entries `&& classSource != null` guard to master
  (`fromSource -> Source`) and changed the record `isPrimary` branch from `source == null` to
  `source?.psi == null` (non-PSI canonical-record detection). Offsets/text exact; element-type fidelity
  intentionally out of scope.
- **Files**: `parse/JavaLightTreeStructure.kt` (new), `parse/JavaLightTree.kt` (memoized adapter),
  `model/JavaElementOverAst.kt` (seam impl), fir-jvm `java/JavaDirectSourceElementOwner.kt` (new) +
  `java/FirJavaFacade.kt`; test `JavaLightSourceElementTest.kt` (new).
- **Tests**: `:compiler:java-direct:test` box+phased green (0 failures); PSI gate
  `PhasedJvmDiagnosticLightTreeTestGenerated.*` green; `CompileKotlinAgainstKotlin` gate green.
- **Result**: green; PSI behaviour unchanged (enum revert matches master; `source?.psi == null` never
  fires for PSI).

### 2026-06-09 — Minify supertype cycle breaker to a session-keyed guard
- **Change**: Replaced the per-file `JavaSupertypeCycleChecker` (thread-local deque + dead
  `recordCycleEdge`/`consumeCycleEdges` diagnostic machinery, never wired to a diagnostic) with a
  session-registered `JavaModelSupertypeWalkGuard` + `cycleGuardedSupertypeWalk`, co-located with
  `cycleSafeClassLikeSymbol`/`JavaModelInFlightResolutions` and mirroring its shape (concurrent
  per-session set, no thread-local). Behaviour is unchanged: re-entry on an in-flight `ClassId`
  returns the caller default, bounding `A→B→A` Java inheritance cycles.
- **Comments**: `cycleSafeClassLikeSymbol` KDoc now states the *hypothetical* re-entrance trigger
  (no IntelliJ-test mention); `JavaCycleBreakerTest` documents the real `testIntellij_vcs_git` /
  KT-74097 scenario (`GitSimpleEventDetector.Event.@Deprecated`, refs to
  `archive/ITERATION_RESULTS_2026_05_11.md`) for the in-flight guard and the hypothetical
  malformed-cyclic-Java pattern for the supertype guard.
- **Files**: `JavaModelSessionAccess.kt` (+guard), `JavaTypeResolver.kt`, `JavaFileContext.kt`
  (−`cycleChecker`), `JavaClassFinderOverAstImpl.kt` (+register), `JavaCycleBreakerTest.kt`;
  deleted `JavaSupertypeCycleChecker.kt`.
- **Tests**: `:compiler:java-direct:test` 2816/2816 (455 files, 0 failures); `JavaCycleBreakerTest`
  4/4 (each breaker proven load-bearing — `StackOverflowError` when the guard component is absent).
- **Result**: green; valid-code paths unaffected, both breakers stay out of the way.
