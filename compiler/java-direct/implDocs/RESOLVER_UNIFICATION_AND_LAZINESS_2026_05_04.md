# Resolver Unification and the Lazy-Resolution Contract

This document captures the design considerations around a reviewer-suggested
unification: replace the **two parallel resolvers** that classifier resolution
goes through today (an AST-side resolver in `java-direct` for Java sources +
the FIR symbol provider for everything else) with **one origin-agnostic path**
that goes through FIR. It then refines that proposal with the constraint that
came up in the follow-up review: **the unification must preserve the laziness
of Java resolution** that compilation of predominantly-Java modules with a
small Kotlin surface depends on.

The earlier sister document — [CLASSIFIER_RESOLUTION_TRACE_2026_05_04.md](CLASSIFIER_RESOLUTION_TRACE_2026_05_04.md)
— traces a single resolution end-to-end and assesses redundancy *within*
the current architecture. This document is about *changing* the architecture,
and what would have to be true for that change to be safe.

---

## The reviewer's point

> "When resolving a type, we effectively had to treat all kind of origin of
> the referenced class separately (java sources / binary / Kotlin sources).
> While in fact we may just used their FIR representation which would work
> for all of them universally. I'm sure there would be some computation-loops
> there, but from my intuition it feels like they might be unbounded as we do
> in other places."

The point is real and well-founded. In `java-direct`, classifier resolution
is split across **two parallel mechanisms** that each see the world
differently:

1. An **AST-side resolver** built around `JavaResolutionContext` +
   `JavaScopeResolver` + `JavaInheritedMemberResolver`. This path only knows
   about Java sources — `JavaClassOverAst` instances backed by
   `JavaPackageIndexer` / `JavaClassFinderOverAstImpl`. Kotlin sources and
   Java binaries are invisible to it.
2. A **FIR-side resolver** built around
   `session.symbolProvider.getClassLikeSymbolByClassId(classId)` (the
   `tryResolve` callback in `JavaTypeConversion.kt`). This path is
   **origin-agnostic** — it returns whichever symbol exists, regardless of
   whether the underlying declaration is a Java source class, a Java `.class`
   file, or a Kotlin source class.

The split shows up concretely as duplicate logic:

- `JavaScopeResolver.findLocalClass`'s last fallback
  (`sameFileTopLevelClassProvider(name)`) is a **same-file, top-level,
  Java-sources-only** lookup. The FIR-side `resolveFromSamePackage(simpleName,
  tryResolve)` in `JavaResolutionContext.kt` does an analogous job universally
  for the same-package case — any origin — by going through `symbolProvider`;
  the two together cover the same physical source-class set, but via different
  routes (a same-file fast path vs. a per-package symbol query).
- `JavaInheritedMemberResolver.findInnerClassFromSupertypes` walks
  Java-source supertypes through the AST model. The FIR-side
  `getResolvedSupertypeClassIds` walks supertypes through `superTypeRefs`.
  Both exist because each is intentionally restricted to one half of the
  world: AST for `Java.Source`, FIR for `Kotlin / Java.Library`.
  `JavaInheritedMemberResolver.resolveInheritedInnerClassToClassId` literally
  does Phase 1 (AST walk for source supertypes) + Phase 2 (FIR-callback walk
  for non-source supertypes) — two implementations of the same JLS 6.5.2
  rule, glued together.

The duplication is structural, not accidental.

---

## Why the code looks the way it does today

Each piece of the current shape has a reason, but the reasons are weaker than
they look at first glance.

1. **Type parameters genuinely have to be AST-resolved.** A reference to `T`
   inside `class Derived<T> extends Base<T>` cannot be answered by
   `getClassLikeSymbolByClassId` — it is not a class. This is a
   non-negotiable AST-side concern; any unification has to keep it.
2. **Same-file sibling top-level classes can be probed without going through
   FIR symbol resolution.** Asking the source half directly via
   `sameFileTopLevelClassProvider` (a thin lookup over `JavaClassCache`) is cheaper than
   `tryResolve(ClassId(pkg, name))` → `JvmSymbolProvider` →
   `CombinedJavaClassFinder` → `BinaryJavaClassFinder` (miss) →
   `JavaClassFinderOverAstImpl`. This is the source of *some* of the AST-side
   preference, but it is a performance argument, not a correctness one.
3. **Loop avoidance.** This is the real reason and the one the reviewer also
   flagged. The comments already in the code call it out:
   - `JavaTypeConversion.kt` on `resolveSymbolBasedClassId`: *"in LL-FIR a
     direct probe can trigger lazy resolution of the very class being
     resolved, causing infinite recursion."*
   - `JavaTypeConversion.kt` on `getResolvedSupertypeClassIds`: *"Java SOURCE
     class supertypes are walked via the class finder in Phase 1… Accessing
     `FirJavaClass.superTypeRefs` for source classes could trigger premature
     lazy resolution (it calls javaClass.supertypes which may circle back
     into type conversion)."*

   The AST-side path was kept partly because, when a Java source class's
   *own* supertype is being resolved, asking FIR what `Base`'s supertypes
   are can recurse back into the very supertype-resolution we are running.
   The current solution is to filter by **origin** (`FirDeclarationOrigin.Java.Source`
   is excluded; `Kotlin / Java.Library` are allowed). It works, but it is
   the exact "treat origins separately" pattern the reviewer is uncomfortable
   with.

---

## Are the loops "bounded as we do in other places"?

Yes — and FIR already proves it elsewhere. The Kotlin frontend resolves
Kotlin supertypes, including chains involving Java declarations, through a
**lazy phase model** (`SUPER_TYPES`, `STATUS`, `TYPES`, `BODY_RESOLVE`). The
lazy resolver bounds the loop in two ways:

- **`getClassLikeSymbolByClassId(classId)` is phase-independent.** It returns
  *the symbol*, not its resolved supertypes. Asking for the symbol of
  `foo.Base` does not force `Base`'s `SUPER_TYPES` phase. This is exactly why
  `tryResolve(ClassId)` is safe even mid-resolution.
- **Reading `superTypeRefs` *does* require a phase.** If the class's
  `SUPER_TYPES` phase is already complete, you read; if not, the lazy
  resolver promotes it (and detects re-entry on the same class). FIR has the
  machinery to break cycles via `lazyResolveToPhase`.

The current java-direct code refuses to use that machinery for Java *source*
classes. Instead, it carves out an AST-side fast path with manual `visited`
sets (`JavaInheritedMemberResolver.findInnerClassFromSupertypes` passes
`mutableSetOf<JavaClass>()`) and per-call `HashMap` caches in
`JavaResolutionContext.resolve`. These are real, finite loop-bounding
mechanisms — they just happen to run on the Java model rather than on FIR
symbols.

In other words, the loops *are* bounded; the question is whether bounding
them through ad-hoc AST-side BFS + origin filters is better than bounding
them through FIR's phase model. The reviewer's intuition is that the latter
is cleaner and probably no worse on performance.

---

## The laziness constraint

The unification's "what" is independent of the "when". The "when" is the
load-bearing question: in a predominantly-Java module with a handful of
Kotlin sources, the rule must be

> **A Java declaration is parsed/symbol-ified only when something on the
> Kotlin side (transitively) reaches it.**

Anything that breaks that rule is a regression, no matter how clean the code
looks.

### Where laziness lives today

There are two distinct laziness axes, and they are easy to conflate:

1. **Index-level laziness** — owned by java-direct.
   - `JavaPackageIndexer.ensurePackageIndexed(fqName)` only indexes a
     directory the first time it is asked. A package nobody references is
     never walked.
   - `JavaClassCache.getOrPutIfNotNull` only parses `Foo.java` the first time
     `findClass(ClassId(pkg, Foo))` resolves to it. A `.java` file that
     nobody references is never parsed into a `JavaClassOverAst`. The
     `LightTreeBuilder` is never invoked for it.
   - The lightweight scanner used during indexing reads only `package` /
     top-level class names — full file parsing is deferred to actual use.
2. **FIR-level laziness** — owned by FIR.
   - `session.symbolProvider.getClassLikeSymbolByClassId(classId)` returns a
     *symbol*, not a fully-resolved class. The class's `SUPER_TYPES`,
     `STATUS`, `TYPES`, `BODY_RESOLVE` etc. phases are only promoted when
     something asks for them via `lazyResolveToPhase(...)`.
   - For Java sources specifically, `FirJavaClass.superTypeRefs` is a lazy
     property — reading it triggers Java→FIR type conversion, which calls
     back into the resolution context.

For the canonical `Derived extends Base` test case (see
[CLASSIFIER_RESOLUTION_TRACE_2026_05_04.md](CLASSIFIER_RESOLUTION_TRACE_2026_05_04.md)),
the laziness contract holds: `foo/Base.java` is only parsed because
`Derived extends Base` is reached from a Kotlin reference. If the user
removed `d.foo()` and only kept `foo.Derived` as a parameter type,
`Base.java` would still be parsed (because `Derived`'s supertypes are read
during enhancement) — but its *members* are still only enumerated when
something asks for them.

### Why the unification doesn't have to break laziness

The pieces proposed for unification (`sameFileTopLevelClassProvider` same-file
fallback; `JavaInheritedMemberResolver` Phase 1 inner-class walks;
`getResolvedSupertypeClassIds` for source classes) are all things that **are
already triggered today only when somebody is resolving a name**. Nobody calls
`findInheritedInner` speculatively; it is always a "I'm currently converting
this Java type, I need to know what `Inner` resolves to in this scope".
Routing the same call through `symbolProvider.getClassLikeSymbolByClassId`
instead of through `LeanJavaClassFinder` does not change *when* the call
happens — only *who answers it*.

So the two relevant questions are:

- **Does `getClassLikeSymbolByClassId(ClassId(foo, Base))` force more work
  than `JavaClassFinderOverAstImpl.findClass(ClassId(foo, Base))`?** It does
  not, *if* we keep `JavaClassFinderOverAstImpl` plugged into
  `JavaClassFinder` and the `JvmSymbolProvider` route preserves the existing
  class-level laziness. The FIR symbol-provider path ends up calling
  `JavaClassFinder.findClass` anyway (that's what `CombinedJavaClassFinder`
  is for) — it just wraps the result in a `FirJavaClass` symbol. Symbol
  creation does not promote phases.
- **Does asking for `firClass.superTypeRefs` (for inherited-inner-class
  lookup) force resolution of classes that nobody has referenced?** This is
  the genuine risk. Whether it does determines whether the unification's
  Stage 3 (see below) is safe.

---

## The genuine laziness risk: `getResolvedSupertypeClassIds` on source classes

Today's code refuses to walk `superTypeRefs` for source classes — both for
cycle-bounding *and* implicitly because doing so would force the
`SUPER_TYPES` phase. A unified design has to bound the cycle through
`lazyResolveToPhase(SUPER_TYPES)` instead, and through the laziness lens
that has to be tightened.

Two scenarios:

- **Predominantly-Java module, no Kotlin reference to `MidwayClass`.** If
  nobody on the Kotlin side ever mentions `MidwayClass`, then nothing should
  ever ask `getClassLikeSymbolByClassId` for it, so the `SUPER_TYPES` phase
  is never promoted and `MidwayClass.java` may not even be parsed beyond the
  index scan. **That's the case the reviewer's follow-up worries about and
  the unification must preserve it.** It is preserved naturally — the
  unification does not add any speculative walks; it only changes which
  engine answers a question someone *is already asking*.
- **Predominantly-Java module, Kotlin references `Leaf` whose supertype chain
  is `Leaf → Mid → … → Root`.** Today, resolving `Leaf` already forces the
  chain to be walked on the AST side (because `Leaf`'s
  `JavaClassOverAst.supertypes` is read during type conversion). So the work
  done is the same — `Mid`, ..., `Root` get parsed and class-symbol-ified
  either way. The choice is only *whether the walking happens through
  `JavaSupertypeGraph` + `LeanJavaClassFinder` or through
  `firClass.superTypeRefs` + `lazyResolveToPhase(SUPER_TYPES)`*.

The unification does not change *which* classes get touched in a given
compilation. It only changes *how* the cycle-bounding is done. As long as
we (a) never walk `superTypeRefs` speculatively and (b) only promote a
class's `SUPER_TYPES` when something legitimately asked about it, the
laziness contract is preserved.

---

## Five laziness invariants the unification must keep

These should be written down as part of the migration so a future reviewer
(and CI) can check them mechanically:

1. **No speculative `getClassLikeSymbolByClassId` calls.** Every call must be
   on the JLS resolution path of an actual reference encountered in Kotlin
   source (or transitively in a Java declaration that is itself reached from
   Kotlin source). The current `JavaResolutionContext.resolve(...)` already
   satisfies this — it is only entered from `JavaClassifierType.resolve(...)`,
   which is in turn entered from `JavaTypeConversion`, which is in turn
   entered from `FirJavaClass.superTypeRefs` resolution / member type
   resolution.
2. **No walking of `superTypeRefs` for inherited-inner-class lookup outside
   an active resolution.** The only call site is
   `JavaInheritedMemberResolver.findInnerClassFromSupertypes`, which already
   runs on demand. The unified version just changes where the supertype list
   comes from.
3. **`lazyResolveToPhase` is the cycle bound, not eagerness.** When the
   unified `getResolvedSupertypeClassIds(classId)` is asked, the
   implementation must `lazyResolveToPhase(symbol, SUPER_TYPES)` only if it
   actually needs supertypes (i.e. for the inherited-inner-class case). For
   the simple "does this class exist?" path (`tryResolve`) it must stay at
   `lazyResolveToPhase(STATUS)` or just symbol existence — never force
   `SUPER_TYPES`.
4. **The same-package fast path doesn't widen the index.** A
   `LeanJavaClassFinder` short-circuit for `resolveFromSamePackage` checks
   "is `ClassId(pkg, name)` already in the package index?" — and
   `ensurePackageIndexed(pkg)` was already called as part of the *current*
   class's resolution. No new packages are scanned that the FIR path
   wouldn't have scanned anyway.
5. **No eager parsing.** `JavaClassCache.getOrPutIfNotNull` stays the only
   entry point to `parseJavaSourceFile`. The unified resolver must not
   bypass it. (Trivially true if we keep `JavaClassFinderOverAstImpl` in the
   chain — which we do, that's the whole point of the unification preserving
   the source half as a `JavaClassFinder` plugin.)

If those five invariants hold, then **the laziness profile of the unified
code matches the current code on a per-class basis**. On a predominantly-Java
module with N Java classes of which K are reachable from Kotlin, both today's
split and the unified design parse and class-symbol-ify K Java files (plus
their transitive supertype closure within the source set, which is the same
in both).

---

## Three failure modes to guard against

The unification could *accidentally* break laziness in three specific ways.
Each has a mechanical mitigation.

1. **Calling `getClassLikeSymbolByClassId` over a *list* of candidates instead
   of one-by-one.** If the unified `findInheritedInner` decided to "preload
   all inherited classes" via a batched symbol lookup, it would force every
   `ClassId` in the supertype closure into a symbol — including supertypes
   of supertypes that the original AST walk never had to touch. The current
   AST walk is intentionally one-step-at-a-time (BFS guided by visited set);
   the unified version must do the same.

   *Mitigation enforcement update — post-Step-4.5a (`FirSession` injection
   track).* Pre-injection, this failure mode is **structurally** prevented:
   the Java Model has no handle on `FirSession` and therefore physically
   cannot reach `session.symbolProvider`. Once `FirSession` is injected into
   the model
   ([`FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md`](FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md) §§7, 8),
   the structural prevention disappears; the model *can* reach the symbol
   provider, so the invariant becomes a coding rule. Mitigation is then
   three-tiered:

   - **KDoc.** Each model-side resolution-time helper is annotated with the
     "no batched eager lookups" rule.
   - **`AGENT_INSTRUCTIONS.md` rule.** A new bullet under the laziness
     section forbids calling `firSession.symbolProvider.*` from anything
     reachable from cache-population code (`JavaClassCache`,
     `LeanJavaClassFinder.indexFile`, `JavaSupertypeGraph`-population).
   - **Typed `LazySessionAccess` wrapper** (required, not optional). The
     wrapper is a value class around `FirSession` whose surface exposes only
     symbol-provider methods needed by resolution-time code. Cache-population
     components hold AST-shaped inputs only — they never see the wrapper, so
     they cannot reach the symbol provider. This makes the invariant
     **typeable** rather than reviewable; future contributors editing
     cache-population code get a compile-time error rather than a code-review
     comment.

   The wrapper lands as part of Step 4.5a alongside `JavaSupertypeLoopChecker`;
   see the proposal doc §11 for the per-file contract and §12 Q2 for the
   resolution rationale.
2. **Routing type-parameter lookups through FIR.** Type parameters of an
   outer Java class are syntactic — answerable from the AST without any
   resolution. If we accidentally route `findTypeParameter("T")` through a
   FIR query to find the enclosing class first, we'd promote that class to
   `STATUS` phase just to read its type-parameter list. That's why Stage 2
   of the migration explicitly says "type parameters stay AST-resolved" —
   not for performance, but for laziness.
3. **`isTriviallyFlexibleHint` / `isRaw` / cross-file flexibility checks
   asking for full class symbols.** These properties are read as part of
   building a `ConeKotlinType` — i.e. during *every* Java type conversion.
   If the unified version asked
   `toRegularClassSymbol(session)?.typeParameterSymbols` for every read,
   we'd promote every referenced Java class to whatever phase
   `toRegularClassSymbol` triggers on every type conversion, including for
   types we never actually need raw-checking on. Today this only happens in
   the `classifier == null` branch (cross-file types) and the result is not
   cached. Caching the resolved `ClassId` plus its type-param count on the
   `JavaClassifierType` would be a strict improvement (less work, same lazy
   profile).

For each, the mitigation is mechanical: the unified resolver keeps the
**one-class-at-a-time, on-demand-only** access pattern that the current code
happens to enforce as a side effect of going through the AST. We just need
to make it an explicit invariant in the unified version, not an emergent
property.

---

## Staged migration plan, with laziness annotations

The plan is staged so that each step is independently reviewable and
risk-bounded. The annotations after each stage are the laziness implication.

### Stage 1 — Make the duplication structurally obvious

`JavaResolutionContext.resolve(name, tryResolve, getSupertypeClassIds)`
already accepts FIR callbacks; add a corresponding
`getClassLikeSymbol: (ClassId) -> ResolvedClassDescriptor?` that answers
*both* "exists?" and "what is its origin?". Today the symbol-presence check
is wrapped in `tryResolve` and the origin information is **discarded**
(`tryResolve` returns `Boolean`). Threading the resolved symbol through (with
`FirDeclarationOrigin`) lets the resolver make decisions like "this candidate
is a Kotlin class, so the AST-side fast path can't possibly help, skip it"
without changing behavior.

*Laziness impact*: pure refactor, no behavior change. The call is already
made.

### Stage 2 — Narrow the AST-side resolver to its irreducible core

Push everything that *can* be FIR-resolved into FIR. Keep on the AST side
only:

- **Type parameters** (`findTypeParameter`, `findInheritedTypeParameter`) —
  must stay; FIR cannot answer this.
- **`containingClassIds`** computation — purely syntactic, used as an input
  to FIR queries.
- **A single same-file top-level lookup** — `sameFileTopLevelClassProvider`
  against the per-file class cache, *only* as a performance hint. Remove the
  inner-of-supertype walks; those become FIR queries.

In particular, drop `JavaInheritedMemberResolver` Phase 1 in favour of
Phase 2 alone: BFS over `getResolvedSupertypeClassIds(classId)`, regardless
of origin. To make this safe you need Stage 3.

*Rename note*: this lambda was historically called `localClassProvider` —
which has nothing to do with JLS 14.3 local classes (declarations inside a
method or initialiser block), nor with the broader "lexical scope" sense
the rest of `java-direct` uses the word for. As of the prep change that
preceded this stage, it has been renamed to `sameFileTopLevelClassProvider`
(and its underlying `JavaImportResolver.findClassNode` helper renamed to
`findTopLevelClassNode`). The narrow name makes the "perf-only fast path"
claim self-documenting and reserves the word "local" for the JLS 14.3 /
`ClassId.isLocal` sense already used elsewhere in the Kotlin compiler.

*Laziness impact*: improves slightly — fewer syntactic walks per resolution.
Type parameters stay AST-resolved, which is critical: if we accidentally
routed them through FIR, every reference to `T` inside `Outer<T>` would
require FIR to resolve `Outer` to get its type-parameter list, promoting
`Outer` to `STATUS` for an answer that the AST already has for free.

### Stage 3 — Remove the `Java.Source` origin filter from `getResolvedSupertypeClassIds` by using lazy resolution

Replace the current

```kotlin
if (firClass is FirJavaClass && firClass.origin == FirDeclarationOrigin.Java.Source)
    return emptyList()
return firClass.superTypeRefs.mapNotNull { ... }
```

with a lazy-resolved variant that promotes the class to `SUPER_TYPES` phase
before reading. The recursion guard becomes "are we currently resolving this
very class's supertypes?" — which FIR's phase machinery already tracks —
instead of "is this class a Java source class?".

The one place this is genuinely subtle is the *initial* call: when
`Derived`'s own `SUPER_TYPES` phase is in progress and we're computing the
`ClassId` of `Base`. There you can pass `lazyResolveTo = STATUS` instead of
`SUPER_TYPES` (Base only needs to *exist* for the simple-name resolution,
not have its supertypes resolved). For the inherited-inner-class lookup we
*do* need supertypes, but by then we've already moved past `Derived`'s
`SUPER_TYPES` phase, so requesting `SUPER_TYPES` on the candidate class is
safe. The single piece of state that needs to be threaded is "the set of
classes whose `SUPER_TYPES` is currently on the stack", which FIR already
maintains.

*Laziness impact*: **the load-bearing stage**. Before merging, run the
parse-counter / symbol-creation-counter check (see Verification below) on a
predominantly-Java module. If the unified version forces even one extra
Java class to a phase it didn't need to be at, the stage isn't ready.

### Stage 4 — Collapse `findLocalClass` into the unified path

Once Stage 3 is in place, `JavaScopeResolver.findLocalClass` shrinks to:

```kotlin
fun findLocalClass(name: Name): JavaClass? {
    // 1. inner class of containing chain — FIR via getClassLikeSymbol on each containingClassId
    // 2. inherited inner class of containing chain — FIR via getResolvedSupertypeClassIds
    // 3. same-file top-level — sameFileTopLevelClassProvider (perf-only fast path)
}
```

Step 3 stops being mandatory. If you delete it, correctness is preserved
(FIR's same-package query handles it); only performance changes — and only
by the cost of one extra `getClassLikeSymbolByClassId` call per simple
cross-file reference.

*Laziness impact*: keep step 3 *because* it does not widen the index — it is
a cache lookup, not an indexing trigger. Decide empirically whether to keep
it on benchmarks; correctness is the same either way.

### Stage 5 — Origin-agnostic outcome

After Stages 1–4, the AST side answers exactly two questions: *"is `name` a
type parameter in scope?"* and *"give me the `ClassId` chain of the
containing class declaration"*. Everything else flows through FIR's symbol
provider with whichever origin is appropriate. `JavaInheritedMemberResolver`'s
two-phase split, `getResolvedSupertypeClassIds`'s `Java.Source` filter, and
`findLocalClass`'s sibling/inner/inherited fan-out collapse into single
uniform queries.

*Laziness impact*: none new — the conclusion of the previous stages.

---

## Verification under realistic loads

A predominantly-Java module is the right testbed. Concretely:

1. Pick `IntelliJFullPipelineTestsGenerated.testIntellij_platform_externalProcessAuthHelper`
   (or any predominantly-Java module from the IJ test corpus) — it has
   hundreds of Java sources and a handful of Kotlin files.
2. Add a counter to `JavaClassCache.getOrPutIfNotNull` (parse counter) and
   to `JvmSymbolProvider.getClassLikeSymbolByClassId` (symbol-creation
   counter) — both today and after the unification, behind a flag.
3. Confirm that the parse counter is **identical or smaller** under the
   unified design, and that the symbol-creation counter is **bounded by the
   count of distinct ClassIds reachable from Kotlin**, not by the total Java
   class count of the module.

If those numbers don't hold, the unification is leaking eagerness somewhere
and we can locate it before it ships. The symbol-creation counter is the
canary for failure modes 1 and 3 above; the parse counter is the canary for
any accidental indexing widening.

---

## Net assessment

The reviewer's instinct matches the trace evidence and the existing code
comments:

- The split exists for **two reasons**: (a) type parameters genuinely need
  an AST resolver, and (b) we are afraid of FIR re-entry while resolving
  Java-source supertypes.
- Reason (a) does not justify the *whole* AST resolver, only the
  type-parameter part of it.
- Reason (b) is real but solvable in the same way FIR already solves it for
  Kotlin: lazy phases + per-class cycle guards, not per-origin branching.

The unification is sound, and the current split should be treated as **a
temporary compromise** rather than a target architecture. The migration is
non-trivial — the value is mostly in conceptual clarity and removing the
`FirDeclarationOrigin.Java.Source` special-casing from `JavaTypeConversion.kt`
— but it is bounded and stageable. Stages 1 and 2 are mechanical and
risk-free; Stage 3 is the genuinely interesting one where we'd want to
confirm with the FIR/LL-FIR maintainers that asking for `SUPER_TYPES` on a
`FirJavaClass` mid-supertype-resolution of *another* `FirJavaClass` is
bounded the same way it is for `FirRegularClass` (Kotlin) — the FIR
resolver looks like it does, but it's the kind of claim worth validating
before relying on it for real workloads.

The performance concern from the original design (same-package source-class
lookup faster than going through `CombinedJavaClassFinder`) is fixable
independently: a `LeanJavaClassFinder` short-circuit *inside* the unified
`tryResolve` callback — not a separate resolution path. That preserves the
optimisation without the structural duplication.

In short: **yes, conditionally, and the condition is worth automating.**
The unification is compatible with the lazy contract — but only if Stage 3
is implemented by going *through* FIR's existing lazy phase machinery
(`lazyResolveToPhase(SUPER_TYPES)` on demand) rather than around it
(eagerly resolving supertypes to feed a unified BFS). The current AST-side
resolver enforces "on-demand, one-class-at-a-time" as a side effect of its
data structures; the unified version has to enforce it explicitly, with the
five invariants above and the parse-counter / symbol-creation-counter check
as a CI guardrail.

Without that guardrail, the unification is the kind of change that looks
fine in unit tests on tiny corpora and silently regresses compilation time
on large IJ-style modules. With it, it's safe — and a clear improvement in
both code clarity and conceptual coherence over the present split.

---

## Cross-references

- [CLASSIFIER_RESOLUTION_TRACE_2026_05_04.md](CLASSIFIER_RESOLUTION_TRACE_2026_05_04.md)
  — concrete `Derived extends Base` trace and per-property redundancy
  inside the *current* architecture (the "what's already there to optimise"
  perspective; this doc is the "what's the next architecture" perspective).
- [RESOLUTION_PIPELINE.md](RESOLUTION_PIPELINE.md) — abstract pipeline
  layering: AST → resolution context → FIR symbol provider.
- [ARCHITECTURE.md](ARCHITECTURE.md) — module layering and component
  responsibilities.
- `JavaResolutionContext.kt` — `resolve` /
  `resolveSimpleNameToClassIdImpl` / `resolveFromSamePackage`.
- `JavaScopeResolver.kt` — `findLocalClass` and `sameFileTopLevelClassProvider`
  fallback.
- `JavaInheritedMemberResolver.kt` — Phase 1 (AST) + Phase 2 (FIR) split for
  inherited-inner-class resolution.
- `JavaTypeConversion.kt` (shared FIR) — `resolveSymbolBasedClassId`,
  `getResolvedSupertypeClassIds`, and the `FirDeclarationOrigin.Java.Source`
  exclusion comment.
- `JavaClassFinderOverAstImpl.kt` / `LeanJavaClassFinder` — the source-half
  finder that would survive the unification as a `JavaClassFinder` plugin.
- `JavaPackageIndexer.kt` / `JavaClassCache.kt` — index- and parse-level
  laziness; the per-class invariants depend on these staying the only
  parsing entry points.

---

*Created: 2026-05-04 (resolver-unification proposal + laziness-contract refinement)*
