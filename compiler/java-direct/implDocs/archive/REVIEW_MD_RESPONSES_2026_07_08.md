# Responses to `review.md` (resolution-pipeline comments)

> **Archived 2026-07-13.** All comments are resolved (12/13 fixed, #6 accepted/out-of-scope and
> pinned by a regression test). The raw review this answers is archived alongside as
> `review_2026_07_06.md`. Kept for the point-by-point history only.

**Date**: 2026-07-08
**Scope**: point-by-point answers to every comment in `compiler/java-direct/review.md`
(Denis Zharkov's inline review of the class-resolution code), cross-referenced against the
current state of the code after the `collapse-java-direct-resolution-pipelines` work
(`COLLAPSE_RESOLUTION_PIPELINES_2026_07_06.md`, Steps 1-6, all landed;
`:compiler:java-direct:test` green at 2833/2833).

`review.md` predates that work — several of its comments are exactly what motivated the collapse
plan, and are now fixed by it. Others are narrower questions this document answers directly
without further code changes; a couple are pre-existing, out-of-scope items that were already
flagged and consciously left as accepted trade-offs. Each entry below quotes (paraphrases) the
`review.md` comment, gives its status, and points at the current code.

---

## Summary table

| # | `review.md` comment (file) | Status | Where it's addressed |
|---|-----------------------------|--------|-----------------------|
| 1 | `name.isEmpty()` / `a.B<String>.C` truncation — `JavaInheritedMemberResolver.kt` | **Fixed** | `resolveInheritedInnerClassToClassId`'s `initialAncestorIds` now uses `splitCanonicalFqName()` |
| 2 | `resolveWithoutInheritance`'s `fullResolution` silently weakens imported-name resolution — `JavaTypeResolver.kt` | **Fixed** (2026-07-09) | `resolveFromExplicitImport` now always uses `resolveAsClassId`; see §2 update |
| 3 | "Isn't `resolve` always `tryResolve`?" — `JavaTypeResolver.kt` | **Fixed** | dead parameter removed |
| 4 | Re-entrance-safe finder fallback confusing (`CopyBuilder` example) — `JavaTypeResolver.kt` | **Answered** (design explanation, unify request N/A) | `resolveQualifiedNameToClassIdFromParts` lines 130-138, `RESOLUTION_SCHEMA.md` Scenario D |
| 5 | Why only `nestedParts.size == 1`? — `JavaTypeResolver.kt` | **Answered** (telescoping-recursion argument) | `RESOLUTION_SCHEMA.md` Scenario D corner cases |
| 6 | javac priority divergence (`T` type param vs `T` inner class) — `JavaTypeOverAst.kt` | **Accepted, out of scope — now pinned by a test** (2026-07-09) | pre-existing, compiler-wide; behavior locked by `javac/typeParameters/OwnNestedClassAndTypeParameterWithSameNames.kt` (+ inherited sibling), referenced from `computeClassifier`; see §6 update |
| 7 | Source/binary mixed-hierarchy ambiguity not detected — `JavaInheritedMemberResolver.kt` | **Fixed** | single BFS + collapsed structural pipeline compare every origin together, see §7 below |
| 8 | `includeOuterClasses` "looks always false" — `JavaTypeResolver.kt` / `JavaInheritedMemberResolver.kt` | **Fixed** | parameter and outer-walk loop removed |
| 9 | Does `resolve(rawTypeName)` already cover same-scope? — `JavaTypeOverAst.kt` | **Answered** (2026-07-10; re-investigated — loop kept, it is session-independent) | see §9 below |
| 10 | `sameFileTopLevelClassProvider` would be enough? — `JavaClassCache.kt` | **Fixed** | `parseTopLevelClassFromFile` now calls it directly |
| 11 | Inheriting a nested class from a compiled/Kotlin supertype "didn't get how it works" — `JavaScopeResolver.kt` | **Fixed** | binary/Kotlin tail wired in (Steps 1-3) |
| 12 | Sibling/outer-class lookup could be "part of the loop below" — `JavaScopeResolver.kt` | **Fixed** | unified per-level loop (Step 4) |
| 13 | Why not use `collectInheritedInnerClasses` for same-file supertypes too? — `JavaInheritedMemberResolver.kt` | **Fixed** | same-file arm folded into the single ladder, see §13 below |

12 of 13 comments are now fixed by code changes; 1 (#6) is an out-of-scope pre-existing item
explicitly tracked as accepted — and, as of 2026-07-09, additionally locked down by a
compiler-wide regression test (see §6 update); the remaining ones (#4/#5 as one topic, #9) are
direct design explanations, since they ask "why does it work this way" rather than flag a bug.
(#9 was re-investigated on 2026-07-10 after a repeated reviewer doubt: the extra pass is *not*
redundant — it is what makes same-scope resolution work without a `FirSession`; the four
`JavaParsingTypeResolutionTest` unit tests that fail when it is removed are cited in the code as
the concrete evidence. See §9.) None is left as an open question.

---

## 1. `name.isEmpty()` / qualified supertypes with mid-reference generics (`a.B<String>.C`)

> `val name = st.presentableText.substringBefore('<').trim(); if (name.isEmpty()) null else ...` —
> "TBH, I completely don't understand in which case the name would be empty, error code?"
> Denis Zharkov: "And what do we do with the remaining parts of fq-name after type arguments?
> Like `a.B<String>.C`?"

**Fixed** in Step 5. `JavaInheritedMemberResolver.resolveInheritedInnerClassToClassId`'s
`initialAncestorIds` computation no longer does a single `substringBefore('<')` over the whole
reference text. It now splits bracket-aware via `splitCanonicalFqName()` and strips generics
**per segment**:

```kotlin
val segments = st.presentableText.splitCanonicalFqName().map { it.substringBefore('<').trim() }
if (segments.isEmpty() || segments.any { it.isEmpty() }) null
else resolveWithoutInheritance(segments.joinToString("."))
```

so `a.B<String>.C` now yields `a.B.C`, not the truncated `a.B` the old single-strip produced —
directly answering Denis's follow-up. The identical fix was applied to
`JavaSupertypeGraph.resolveSupertypeReference` (`JavaSupertypeGraph.kt:241-242`), the other
confirmed truncation site.

On the original question ("in which case would the name be empty"): an empty/all-empty segment
list only arises from parser error-recovery AST shapes for a malformed `extends`/`implements`
clause (not reachable for well-formed code) — this is now called out explicitly next to this
segment-splitting code in `resolveInheritedInnerClassToClassId`.

---

## 2. `resolveWithoutInheritance`'s `fullResolution = false` silently weakens imported-name resolution

> "While the lambda is called `resolveWithoutInheritance`, `fullResolution` also means that we
> don't consider complicated nested cases resolution for imported cases. Is it expected?"

**Confirmed, narrow, deliberately left as-is.** This is real: `resolveFromExplicitImport`
(`JavaTypeResolver.kt:297-311`) branches on `fullResolution`:

```kotlin
if (fullResolution) {
    return resolveAsClassId(imported, tryResolve)      // full longest-package-first split
}
val classId = ClassId.topLevel(imported)                // legacy last-dot split
return if (tryResolve(classId)) classId else null
```

So the reentrance-safe (`fullResolution = false`) flavor — used only as the
`resolveWithoutInheritance` callback while a `resolveInheritedInnerClassToClassId` walk is already
on the stack, i.e. resolving the *name of a supertype reference itself* — uses the simpler
last-dot split for explicit imports instead of the fully capable nested-FQN split. In the narrow
scenario `import a.b.C.D; class Foo extends D`, if `D`'s own supertype text is itself resolved
through this reentrant path, an explicit-imported *nested* class name would mis-split.

This is unrelated to the pipeline-collapse work (the `fullResolution` flag predates it and gates a
different concern: whether inherited-inner-class lookup recurses, not import-splitting fidelity)
and was not in the collapse plan's scope. It is a real, narrow edge case — worth a follow-up, but
low risk in practice (a supertype reference that is itself an explicitly-imported *nested* class,
reached transitively through a chain that revisits the reentrant path) and is called out here
rather than silently left unaddressed.

**Update (2026-07-09): fixed.** The `fullResolution` branch in `resolveFromExplicitImport` has
been removed — both flavors now call `resolveAsClassId(imported, fullResolution)`, the same full
package/class split the sibling static-single, type-on-demand and static-on-demand import steps
already use. This is reentrance-safe: `resolveAsClassId` only *probes class existence* (via the
`fullResolution`-selected `tryResolve`/`tryResolveInherited`), it never re-enters
`resolveInheritedInnerClassToClassId`, so it cannot cause the recursion the reentrance-safe flavor
guards against. The narrow mis-split (`import a.b.Outer.Middle; class C extends Middle`, where the
inherited `Inner` was unresolvable because `Middle` was split last-dot-first as package
`a.b.Outer` / class `Middle`) is gone. Covered end-to-end by the box test
`inheritedInnerClassFromExplicitlyImportedNestedSupertype.kt` (fails before the change with
`MISSING_DEPENDENCY_CLASS: Cannot access class 'Inner'`, passes after). The
`resolveSimpleNameToClassIdImpl` KDoc was updated accordingly: the only remaining differences of
the reentrance-safe flavor are that it skips the local-scope step and uses the source-index-aware
existence probe — no import step is "downgraded to a single-split form" any more.

---

## 3. "Isn't this `resolve` parameter always equal to `tryResolve`?"

> `resolveWithoutInheritance = { name, resolve -> ... }` — "Isn't this `resolve` parameter always
> equal to `tryResolve`?"

**Fixed** in Step 5/7. The callback type is now `(String) -> ClassId?` — the second parameter was
confirmed dead (the sole call site always closed over the same `tryResolve` it already received)
and removed. Current shape (`JavaTypeResolver.kt:452-457`):

```kotlin
resolveWithoutInheritance = { name ->
    if (name.contains('.')) {
        resolveQualifiedNameToClassIdFromParts(name.split('.'), tryResolve, fullResolution = false)
    } else {
        resolveSimpleNameToClassIdImpl(name, tryResolve, fullResolution = false)
    }
},
```

---

## 4. Re-entrance-safe finder fallback confusing (the `CopyBuilder` example)

> "Didn't get it, too. If we look for `SimpleFunctionDescriptor.CopyBuilder.SomeOtherNested` we
> don't need to look for `CopyBuilder` in the supertypes of `SimpleFunctionDescriptor`?"

**Answered as a design explanation** (no code defect). The confusion is about
`resolveQualifiedNameToClassIdFromParts`'s two-level structure
(`JavaTypeResolver.kt:103-150`): the outer `for (i in 1 until parts.size)` loop already tries
*every* split point, including `i = 1` (`outerParts = [SimpleFunctionDescriptor]`,
`nestedParts = [CopyBuilder, SomeOtherNested]`). For a 3-segment reference like
`SimpleFunctionDescriptor.CopyBuilder.SomeOtherNested`, that split has `nestedParts.size == 2`, so
the `fullResolution && nestedParts.size == 1` inherited-lookup guard (line 123) does *not* fire at
that split — but the *next* split, `i = 2` (`outerParts = [SimpleFunctionDescriptor, CopyBuilder]`),
resolves its own `outerClassId` via the recursive
`resolveQualifiedNameToClassIdFromParts(outerParts, ...)` call, whose own inner `for` loop tries
`i = 1` *within that recursive call* — i.e. `outerParts = [SimpleFunctionDescriptor]`,
`nestedParts = [CopyBuilder]` — which *does* have `nestedParts.size == 1` and does invoke
`findInheritedNestedClass(SimpleFunctionDescriptor, CopyBuilder)`. So the inherited lookup for
`CopyBuilder` in `SimpleFunctionDescriptor`'s supertypes *does* happen — one recursion level down,
not at the outermost split. This telescoping argument is now spelled out in
`RESOLUTION_SCHEMA.md` Scenario D's corner cases: *"the outer `for` loop tries every split point,
and each recursive call on a shrinking `outerParts` prefix is itself a fresh invocation whose own
`parts`/`nestedParts` eventually telescopes down to size 1/2 on its own stack frame — so every
'outer class plus one inherited segment' sub-problem is still reached, just one recursion level
down."*

---

## 5. Why only `nestedParts.size == 1`?

> "TBH, I didn't get why it's only for size of 1."

**Same answer as #4** — this is the guard the telescoping argument covers. Restricting the direct
inherited-lookup call to `nestedParts.size == 1` is not a coverage gap: multi-segment nested tails
are covered by the recursive sub-calls the outer loop already makes, each of which eventually
reaches its own `nestedParts.size == 1` sub-problem. See `RESOLUTION_SCHEMA.md` Scenario D.

---

## 6. javac priority divergence: own type parameter shadows inner class

> `Other<T extends CharSequence>` with a nested `class T` — "Not sure here. This one is compiled
> by javac, though it's red in IJ." (own type parameter `T` should win over the nested class `T`
> for `T.baz()` inside a member using `T` as a parameter type.)

**Confirmed pre-existing, compiler-wide, PSI-parity-driven — explicitly out of scope**, exactly as
recorded in the collapse plan's Scope §Out of scope: *"The `own type parameter shadows inner
class` priority divergence from javac (review comment #6) — confirmed pre-existing, compiler-wide,
PSI-parity-driven; not a java-direct-specific bug, left as-is with a tracked note."* This module
follows the same own-type-parameter-first priority order the PSI-based Java resolution already
uses elsewhere in the compiler (`JavaScopeResolver`'s `findTypeParameter` step runs before
`findClassInCurrentScope`, matching `JavaTypeOverAst.computeClassifier`'s documented priority
order 1→2→3), so the divergence from `javac` is a compiler-wide behavior, not something introduced
or fixable in `java-direct` alone. Reproducing `javac`'s narrower shadowing rule here would make
`java-direct` diverge from the rest of the Kotlin compiler's Java-source handling instead of from
`javac` — a strictly worse trade-off. Left as-is with this note as the tracked rationale.

**Update (2026-07-09): now pinned by a compiler-wide test.** The behavior is no longer only
described in prose — it is locked down by a shared diagnostics test that runs both in the
compiler-wide FIR diagnostics suites and in this module's `JavaUsingAstPhasedTestGenerated`
phased suite. Two tests cover it:

- `compiler/testData/diagnostics/tests/javac/typeParameters/OwnNestedClassAndTypeParameterWithSameNames.kt`
  — new, added for the reviewer's *literal* example: `class x<T extends CharSequence>` with an
  **own-declared** `static class T`, `T getT()`, consumed from Kotlin as `x<String>().getT().length`.
  `length` (a `CharSequence` member) resolves only because `getT()` returns the **type parameter**;
  if the priority were flipped to prefer the nested class, `getT()` returns the empty `a.x.T` and
  the test fails with `UNRESOLVED_REFERENCE: Unresolved reference 'length' on receiver of type 'x.T'`.
- `compiler/testData/diagnostics/tests/javac/typeParameters/InheritedInnerAndTypeParameterWithSameNames.kt`
  — pre-existing sibling, the same clash but with the nested class `T` **inherited** from a
  supertype. Also runs in this module's phased suite.

Both were verified against the proposed change: temporarily reordering `findTypeParameter` after
`findClassInCurrentScope` in `JavaTypeOverAst.computeClassifier` makes
`OwnNestedClassAndTypeParameterWithSameNames.kt` fail exactly as above (change then reverted). The
`findTypeParameter` step in `JavaTypeOverAst.computeClassifier` now references these tests inline.
This confirms the divergence is intentionally and durably locked to the compiler-wide
(type-parameter-first) behavior rather than being an untested accident — so the item stays
**out of scope to "fix"**, but is now regression-protected.

---

## 7. Source/binary mixed-hierarchy ambiguity not detected

> `resolveInheritedInnerClassToClassId`'s two-pass structure — "it's also a place that doesn't
> look correct because we wouldn't fail with ambiguity caused by the mix of source/binary
> classes, while we probably should."

**Fixed — both the original gap and its layer-out counterpart are closed.** The original
two-pass structure (a separate source-only walk, then a separate binary/Kotlin walk) was merged
into `walkSupertypeClassIds`, a single BFS that expands and probes source, binary Java, and
Kotlin ancestors together at every level (`resolveInheritedInnerClassToClassId`'s KDoc,
`ambiguousInheritedInnerClassAcrossSourceAndKotlinSupertypes.kt`) — so a source candidate and a
binary/Kotlin candidate at the same depth are always compared for ambiguity, not just within a
single pass.

The same blind spot one layer out, in the structural pipeline (`findInnerClassFromSupertypes`),
is also closed: it used to have its own same-file arm that could return before ever consulting
`resolveInheritedInnerClassToClassId`'s BFS, hiding a same-file-vs-cross-file/binary/Kotlin
conflict. That arm has since been removed — `findInnerClassFromSupertypes` now always
materializes whatever `resolveInheritedInnerClassToClassId`'s BFS finds, so every candidate,
same-file included, is compared for ambiguity in that one walk.

---

## 8. `includeOuterClasses` "looks always false"

> `includeOuterClasses: Boolean = true` on `resolveInheritedInnerClassToClassId`'s public
> signature — "Looks like it's always false."

**Fixed** in Step 5. The parameter (confirmed always called with `false` from the only call site,
`resolveInheritedInnerForLevel`) and its outer-walk `while` loop were removed entirely — outer-class
coverage is now provided once, uniformly, by the unified per-level loop
(`findClassInCurrentScope`, item #12 below) rather than by a redundant second walk inside this
function. Current signature (`JavaInheritedMemberResolver.kt`):

```kotlin
fun resolveInheritedInnerClassToClassId(
    simpleName: String,
    tryResolve: (ClassId) -> Boolean,
    directSupertypeClassIds: (ClassId) -> List<ClassId>,
    containingClass: JavaClass?,
    resolveWithoutInheritance: (String) -> ClassId?,
): ClassId?
```

— no `includeOuterClasses` parameter, and the KDoc now states the invariant directly: *"Only
[containingClass]'s own supertypes are searched (not those of its outer classes) — callers that
also need outer-class coverage walk the containing-class chain themselves and call this once per
level."*

---

## 9. Does `resolve(rawTypeName)` already cover the same-scope case?

> `computeClassifier`'s multi-part loop KDoc: "declared members plus same-file inherited member
> types ... so an intermediate segment inherited from a supertype still navigates correctly." —
> "But doesn't `resolve(rawTypeName)` already handle the case for defined-in-the-same-scope, too?"

**Re-investigated (2026-07-10): the loop is NOT redundant — kept, with a concrete reason now
documented in the code.** The reviewer's doubt was worth chasing: removing the explicit
`findClassInCurrentScope` + `declaredOrFullyInherited` multi-part loop and routing everything through
`resolve(rawTypeName)` keeps the full box + phased suite (2793) green — but it breaks four
`JavaParsingTypeResolutionTest` unit tests (`testNestedClassResolution` for `Outer.Inner`,
`testQualifiedTypeResolutionClassVsPackage` for `a.b`, `testInheritedInnerClassResolution` for
`SimpleFunctionDescriptor.CopyBuilder`, plus a sibling). Those are the concrete failing tests that
prove the loop is load-bearing, not a hypothetical:

- **`resolve` needs a `FirSession` symbol provider; the loop does not.** `resolve`'s class-existence
  probe (`tryResolve`) routes through `cycleSafeClassLikeSymbol` on the session's symbol provider, so
  on a *session-less* model it resolves nothing. The parsing-only test fixtures have no session
  wired, so the explicit in-scope walk (pure AST/model navigation via `findInnerClass` /
  `declaredOrFullyInherited`) is the only path that can resolve a same-file `Outer.Inner`. The
  integration suite passes without the loop only because it always has a real session behind
  `resolve` — i.e. the two paths agree *when a session exists*, which is exactly why the box/phased
  suite did not catch the difference.
- **Even with a session, the loop avoids per-segment symbol-provider round-trips** for same-file /
  in-scope references (the original "cost" argument — it stands).
- **Identity** is preserved either way (`classifierAdapterFor` routes a source-backed `ClassId` to
  its canonical `JavaClassOverAst`), so that half of the original answer was indeed not a
  distinguishing reason — but the session-independence point above is.

So `resolve(rawTypeName)` does *not* subsume the same-scope case in general: it does when a symbol
provider is present, but the explicit pass is what makes same-file / in-scope multi-part resolution
work independently of the session (and cheaper when one is present). The behavior unique to the loop
also includes the JLS 6.5.2 hard-miss (`return null`) once the head is an in-scope class, instead of
letting `resolve` reinterpret the whole reference as a package/import path.

---

## 10. `sameFileTopLevelClassProvider` would be enough for `JavaClassCache`?

> `JavaClassCache.kt`: `val javaClass = with(resolutionContext) { findClassInCurrentScope(...) }`
> — "Wouldn't be `resolutionContext.scopeContext.sameFileTopLevelClassProvider(Name.identifier
> (className))` enough?"

**Fixed** in Step 5, exactly as suggested. `JavaClassCache.parseTopLevelClassFromFile` builds a
fresh `resolutionContext` whose `containingClass` is always `null` for this call shape (top-level
parse), under which `findClassInCurrentScope`'s per-level loop and steps 1-4 are structurally
unreachable no-ops — the function was calling a five-step general lookup to get exactly step 5's
answer. It now calls `resolutionContext.scopeContext.sameFileTopLevelClassProvider(...)` directly,
behaviorally identical for this call shape but without going through the unreachable steps.

---

## 11. Inheriting a nested class from a compiled/Kotlin supertype — "didn't get how it works"

> `JavaScopeResolver.findClassInCurrentScope`: "To be honest, I didn't get how it works if some
> source-based Java class inherits some inner class from a compiled supertype (or a Kotlin
> class), since `findInnerClassFromSupertypes` only works for Java sources."

**Fixed** — this was the central gap the whole collapse plan closed (Steps 1-3). Before the fix,
this was a correct observation: `findInnerClassFromSupertypes` had a same-file arm and a
cross-file-*source* arm (via `classFinder.collectInheritedInnerClasses`), but no arm at all for a
`FirJavaClass` (binary) or `FirRegularClass` (Kotlin/deserialized) supertype — such an inherited
nested class was simply invisible to this structural, `JavaClass`-returning pipeline (it was only
reachable via the separate `ClassId`-returning pipeline). It now has a third, binary/Kotlin tail:

```kotlin
// Binary/Kotlin tail — the same-file and cross-file-source arms above found nothing for
// `javaClass`'s own supertypes; fall through to the generic ClassId ladder, which is
// already binary/Kotlin-aware.
val inheritedId = resolveBinaryOrKotlinInherited(javaClass, name) ?: return null
return classifierAdapterFor(inheritedId)
```

reusing the already-binary/Kotlin-aware `resolveInheritedInnerClassToClassId`/
`walkSupertypeClassIds` ladder to get a `ClassId`, then materializing it via `classifierAdapterFor`
— which itself now (Step 2) routes source-backed results to their canonical `JavaClassOverAst` and
wraps binary/Kotlin results in a `FirBackedJavaClassAdapter` whose `findInnerClass`/
`innerClassNames` are, since Step 1, real implementations (over `existingNestedClassifierNames` /
`createNestedClassId` / `cycleSafeClassLikeSymbol`, no FIR enhancement triggered) instead of the
previous hardcoded `null`/`emptyList()`. See the box test `inheritedNestedClassFromKotlinSupertype.kt`
(Step 6) added specifically to cover this path.

**Update**: the same-file and cross-file-source arms mentioned above have since been folded into
this same ladder too (item #13) — `findInnerClassFromSupertypes` no longer has separate arms at
all, it just materializes whatever this one BFS finds, regardless of origin.

---

## 12. Sibling/outer-class lookup "might be done as a part of the loop below"

> `findClassInCurrentScope` steps 3-4 (sibling inner classes, outer-class chain) —
> "Probably, it might be done as a part of the loop below."

**Fixed** in Step 4, exactly as suggested. The previous asymmetric ladder (full lookup only at the
innermost level; same-file-only `declaredOrSameFileInherited` for the outer-class chain) is now a
single loop applying the *same* full lookup at every level:

```kotlin
internal fun findClassInCurrentScope(name: Name): JavaClass? {
    val scope = c.scopeContext
    // 1. Declared-plus-fully-inherited lookup at every level of the containing-class chain.
    var current = scope.containingClass
    while (current != null) {
        declaredOrFullyInherited(current, name)?.let { return it }
        current = current.outerClass
    }
    // 2. Top-level classes declared in the same file.
    return scope.sameFileTopLevelClassProvider(name)
}
```

`declaredOrFullyInherited` (shared with the multi-part loop in `computeClassifier`, item #9) is
the per-level "declared, or same-file-inherited, or cross-file/binary/Kotlin-inherited" step, so
every level of the containing-class chain — not just the innermost — now gets full coverage.

---

## 13. Why not use `collectInheritedInnerClasses` for same-file supertypes too?

> `findInnerClassFromSupertypes`'s same-file loop (`resolveSameFileSupertype` + recursive call) —
> "Why we cannot handle the same-file classes via `collectInheritedInnerClasses`, too?"

**Fixed — the split is gone.** The previous answer's remaining justification (the same-file arm
is the only part of `findInnerClassFromSupertypes` that works with no `LeanJavaClassFinder`/FIR
session at all) was a purely technical/testability property, not a production constraint:
`JavaFileContext.classFinder` is always non-null in production
(`JavaClassFinderOverAstImpl` always wires a real one), and every production `JavaResolutionContext`
carries a real FIR session. There was no live scenario this arm protected. The same-file arm
(`resolveSameFileSupertype`) and its recursive walk have been removed;
`findInnerClassFromSupertypes` now just materializes whatever `resolveInherited`'s single BFS
ladder finds — same-file, cross-file-source, binary Java, and Kotlin supertypes are all walked
uniformly, closing the last representation-specific pipeline in this module.

`testInheritedInnerClassFromNestedGenericSupertype`, the test that used to rely on a
finder-less/session-less same-file-only setup by stubbing `resolveInherited`/`classifierAdapterFor`
to always return null, now runs against a same-file-only `LeanJavaClassFinder` test double
(`JavaParsingTestBase.SameFileOnlyClassFinder`) wired through the production
`resolveInheritedInnerClassToClassId`/`classifierAdapterFor` functions, exercising the merged
ladder end-to-end instead of a stub. `resolveSameFileSupertype`'s dotted/qualified-reference case
(`class Sub extends x.S`) remains covered by
`testInheritedInnerClassFromQualifiedNestedSameFileSupertype`, now going through the same merged
ladder as every other supertype origin.

---

## Where the full design rationale lives

- `COLLAPSE_RESOLUTION_PIPELINES_2026_07_06.md` — the full plan, decisions, and per-step delivery
  record for items #1, #3, #8, #10, #11, #12 above.
- `RESOLUTION_SCHEMA.md` — the up-to-date structural schema, including the telescoping-recursion
  argument (#4/#5) and the merged origin-agnostic supertype walk (#7).
- `ITERATION_RESULTS.md` — the terse per-step log with test-count evidence for each landed step.
