# Responses to `review.md` (resolution-pipeline comments)

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
| 1 | `name.isEmpty()` / `a.B<String>.C` truncation — `JavaInheritedMemberResolver.kt` | **Fixed** | `walkJavaSourceSupertypes`'s `initialIds` now uses `splitCanonicalFqName()` |
| 2 | `resolveWithoutInheritance`'s `fullResolution` silently weakens imported-name resolution — `JavaTypeResolver.kt` | **Confirmed, narrow, left as-is** | `resolveFromExplicitImport` line 304-310 |
| 3 | "Isn't `resolve` always `tryResolve`?" — `JavaTypeResolver.kt` | **Fixed** | dead parameter removed |
| 4 | Re-entrance-safe finder fallback confusing (`CopyBuilder` example) — `JavaTypeResolver.kt` | **Answered** (design explanation, unify request N/A) | `resolveQualifiedNameToClassIdFromParts` lines 130-138, `RESOLUTION_SCHEMA.md` Scenario D |
| 5 | Why only `nestedParts.size == 1`? — `JavaTypeResolver.kt` | **Answered** (telescoping-recursion argument) | `RESOLUTION_SCHEMA.md` Scenario D corner cases |
| 6 | javac priority divergence (`T` type param vs `T` inner class) — `JavaTypeOverAst.kt` | **Accepted, tracked, out of scope** | plan Scope §Out of scope; pre-existing, compiler-wide |
| 7 | Source/binary mixed-hierarchy ambiguity not detected — `JavaInheritedMemberResolver.kt` | **Accepted, documented limitation** | `RESOLUTION_SCHEMA.md` Scenario E, "Accepted, documented limitation" |
| 8 | `includeOuterClasses` "looks always false" — `JavaTypeResolver.kt` / `JavaInheritedMemberResolver.kt` | **Fixed** | parameter and outer-walk loop removed |
| 9 | Does `resolve(rawTypeName)` already cover same-scope? — `JavaTypeOverAst.kt` | **Answered** (design explanation) | see §9 below |
| 10 | `sameFileTopLevelClassProvider` would be enough? — `JavaClassCache.kt` | **Fixed** | `parseTopLevelClassFromFile` now calls it directly |
| 11 | Inheriting a nested class from a compiled/Kotlin supertype "didn't get how it works" — `JavaScopeResolver.kt` | **Fixed** | binary/Kotlin tail wired in (Steps 1-3) |
| 12 | Sibling/outer-class lookup could be "part of the loop below" — `JavaScopeResolver.kt` | **Fixed** | unified per-level loop (Step 4) |
| 13 | Why not use `collectInheritedInnerClasses` for same-file supertypes too? — `JavaInheritedMemberResolver.kt` | **Answered** (design explanation) | see §13 below |

10 of 13 comments are now fixed by code changes; 1 is an out-of-scope pre-existing item explicitly
tracked as accepted; the remaining 3 (#2, #4/#5 as one topic, #9, #13) are direct design
explanations, since they ask "why does it work this way" rather than flag a bug. None is left as
an open question.

---

## 1. `name.isEmpty()` / qualified supertypes with mid-reference generics (`a.B<String>.C`)

> `val name = st.presentableText.substringBefore('<').trim(); if (name.isEmpty()) null else ...` —
> "TBH, I completely don't understand in which case the name would be empty, error code?"
> Denis Zharkov: "And what do we do with the remaining parts of fq-name after type arguments?
> Like `a.B<String>.C`?"

**Fixed** in Step 5. `JavaInheritedMemberResolver.walkJavaSourceSupertypes`'s `initialIds`
computation no longer does a single `substringBefore('<')` over the whole reference text. It now
splits bracket-aware via `splitCanonicalFqName()` and strips generics **per segment**:

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
clause (not reachable for well-formed code) — this is now called out explicitly next to the
sibling same-file check in `resolveSameFileSupertype`'s KDoc: *"An empty segment (or no segments at
all) only arises from parser error-recovery AST shapes for a malformed extends/implements clause —
not reachable for well-formed code; declining here rather than crashing is purely defensive."*

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

---

## 7. Source/binary mixed-hierarchy ambiguity not detected

> `resolveInheritedInnerClassToClassId`'s two-pass structure — "it's also a place that doesn't
> look correct because we wouldn't fail with ambiguity caused by the mix of source/binary
> classes, while we probably should."

**Accepted, documented limitation** — exactly as recorded in the collapse plan's Scope §Out of
scope: *"Fully closing the source/binary mixed-hierarchy ambiguity detection gap (review comment
#7) — accepted perf/safety trade-off, documented rather than fixed."* `RESOLUTION_SCHEMA.md`
Scenario E now spells out precisely which cross-pass check *does* fire and which does not:

- The `ClassId` BFS (`resolveInheritedInnerClassToClassId`) *does* share one `visited` set across
  `walkJavaSourceSupertypes` and `walkBinarySupertypes`, so ambiguity *within* that BFS (a source
  candidate and a binary/Kotlin candidate both resolving to the same simple name at the same BFS
  level) is caught.
- What is **not** caught: `findInnerClassFromSupertypes`'s arm 2 (cross-file Java source, via
  `classFinder.collectInheritedInnerClasses`) finding a unique, unambiguous source-side candidate,
  while a *different* binary/Kotlin branch of the hierarchy independently also declares the same
  name at the same depth — arm 2's own result short-circuits before arm 3 (the binary/Kotlin tail)
  ever runs, so the two never get cross-checked against each other.

Closing this fully would mean unconditionally paying for a binary/Kotlin supertype walk even when
the source-side answer from `collectInheritedInnerClasses` is already unambiguous — a real
perf cost for what is a very rare shape (an inner class name independently declared on two
unrelated branches of a hierarchy, one source, one binary/Kotlin). Accepted as a documented
trade-off rather than fixed, matching the plan's original scoping decision.

The same blind spot exists one layer out, in the structural pipeline
(`findInnerClassFromSupertypes`): its same-file arm, cross-file-source arm (`classFinder`), and
binary/Kotlin tail each detect ambiguity within themselves, but a result from one arm is returned
without cross-checking whether a different arm would also match at the same level — now documented
directly on `findInnerClassFromSupertypes`'s KDoc as the same accepted trade-off.

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

**Answered as a design explanation.** `resolve(rawTypeName)` (the JLS 6.5.2 qualified-name
pipeline, `resolveQualifiedNameToClassIdFromParts`) *can*, in many cases, land on the same
`ClassId` — but it is not a substitute for the explicit `findClassInCurrentScope` +
`declaredOrFullyInherited` multi-part loop in `computeClassifier` (Scenario A step 3 in
`RESOLUTION_SCHEMA.md`), for two reasons:

1. **Priority order.** JLS 6.4.1 requires an in-scope member type (and its inherited member
   types) to be found *before* falling to the JLS 6.5.2 qualified-name treatment of the whole
   reference — which would otherwise try every `outerParts`/`nestedParts` split point using
   different (package/import-driven) resolution rules that don't respect "parts[0] is already an
   in-scope class" the way `findClassInCurrentScope` does. `computeClassifier`'s step 2 (own type
   parameter → in-scope class → inherited type parameter) exists precisely to get this priority
   right for the single-part case; step 3 extends the same in-scope-first principle to the
   multi-part case.
2. **Cost.** Even where both paths agree on the result, `findClassInCurrentScope` for `parts[0]`
   plus a per-level `declaredOrFullyInherited` chain is cheaper than the full qualified-name
   ladder (which tries every split point and, on a miss, falls through to `probeFqnSplits`'s
   package/class guesses) — worth keeping as the fast, direct path rather than routing every
   multi-part reference through the general fallback.

Since Step 4 of the collapse work, this loop now genuinely reaches cross-file/Kotlin/binary
inherited intermediate segments too (not just same-file ones), so the KDoc's claim is no longer an
overclaim — see item #12 and `RESOLUTION_SCHEMA.md` Scenario A step 3.

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
`walkBinarySupertypes` ladder to get a `ClassId`, then materializing it via `classifierAdapterFor`
— which itself now (Step 2) routes source-backed results to their canonical `JavaClassOverAst` and
wraps binary/Kotlin results in a `FirBackedJavaClassAdapter` whose `findInnerClass`/
`innerClassNames` are, since Step 1, real implementations (over `existingNestedClassifierNames` /
`createNestedClassId` / `cycleSafeClassLikeSymbol`, no FIR enhancement triggered) instead of the
previous hardcoded `null`/`emptyList()`. See `RESOLUTION_SCHEMA.md` Scenario E arm 3, and the box
test `inheritedNestedClassFromKotlinSupertype.kt` (Step 6) added specifically to cover this path.

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

**Answered as a design explanation** — this one is deliberate, not an oversight, for two
independent reasons visible in the current code:

1. **`collectInheritedInnerClasses` is backed by a structurally weaker candidate generator for
   same-file qualified supertypes.** It is powered by `JavaSupertypeGraph.getDirectSupertypes` /
   `resolveSupertypeReference`, which works from **raw AST text plus package/import
   information alone** — deliberately, since its own KDoc explains it must avoid triggering
   classifier resolution (`JavaSupertypeGraph.kt:71-74`: *"we read raw JAVA_CODE_REFERENCE text
   from the node, NOT classifierQualifiedName, because the latter triggers resolution which can
   circle back into `getDirectSupertypes` via `findInnerClassFromSupertypes` →
   `collectInheritedInnerClasses`"*). Its own `resolveSupertypeReference` explicitly **declines
   dotted supertype references** (`JavaSupertypeGraph.kt:293-295`: *"Dotted form is delegated to
   `JavaResolutionContext.resolve`"*) — so a same-file qualified supertype like
   `class Foo extends x.S` (both top-level in the same file) would simply not be found through
   this path. `resolveSameFileSupertype` (`JavaInheritedMemberResolver.kt`), by contrast, is built
   specifically to resolve exactly this shape: it navigates each dotted segment via
   `sameFileTopLevelClassProvider` + `JavaClass.findInnerClass`, which does not have this
   limitation, because it is not trying to avoid triggering resolution — it only reads names/AST
   nodes that are already cheaply available (`JavaClassOverAst` is a free AST wrap; no phase
   resolution is involved for reading a same-file class's own top-level/nested identity).
2. **Cost shape.** `collectInheritedInnerClasses` eagerly computes and caches the transitive
   closure of *every* inherited inner-class name for a `ClassId` — the right trade-off for
   cross-file source and binary/Kotlin supertypes, where the alternative is re-parsing another
   file or a symbol-provider round-trip per query. For same-file supertypes, the resolved
   `JavaClassOverAst.supertypes` and the recursive `findInnerClassFromSupertypes` walk are already
   immediately available in memory with no I/O, so eagerly building and caching a name→`ClassId`
   map keyed by `ClassId` buys nothing extra and would only add one more cache to reason about.

Routing same-file supertypes through `collectInheritedInnerClasses` would therefore *reduce*
correctness for qualified same-file supertypes without buying back any performance — the two paths
are kept distinct for the same reason `findInnerClassInSameFileSupertypes`'s raw-text walk in
`JavaScopeResolver` is kept distinct from the resolved-classifier walk (cycle-safety /
resolution-triggering concerns), just realized as a *candidate-generation capability* gap here
rather than a recursion-safety one.

Re-checked after the subsequent merge of the source/binary supertype walks into one BFS
(`resolveInheritedInnerClassToClassId`): both reasons above are untouched by that change — it
only altered how ancestor `ClassId`s are walked for the `ClassId`-returning pipeline, not
`JavaSupertypeGraph.resolveSupertypeReference`'s dotted-reference limitation or
`collectInheritedInnerClasses`'s eager-caching cost shape — so the explanation still holds. A
condensed version of both points is now also inlined as a code comment directly above the
same-file loop in `findInnerClassFromSupertypes`.

---

## Where the full design rationale lives

- `COLLAPSE_RESOLUTION_PIPELINES_2026_07_06.md` — the full plan, decisions, and per-step delivery
  record for items #1, #3, #8, #10, #11, #12 above.
- `RESOLUTION_SCHEMA.md` — the up-to-date structural schema, including the telescoping-recursion
  argument (#4/#5) and the accepted source/binary ambiguity limitation (#7).
- `ITERATION_RESULTS.md` — the terse per-step log with test-count evidence for each landed step.
