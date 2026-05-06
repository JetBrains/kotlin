# `FirSession` Injection — Java Model Redesign Proposal — 2026-05-05

> **Status / scope.** Companion to
> [`MERGED_REFACTORING_PLAN_2026_05_04.md`](MERGED_REFACTORING_PLAN_2026_05_04.md)
> and [`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md);
> **supersedes** [`UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md`](UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md)
> as the chosen track for closing the resolver-unification residue (L1 + L2). The
> alternatives doc remains in place as documented fallback if `FirSession` injection
> is rejected; in particular, alternatives A and F there are still the candidate L1
> closers in that case, and D + B (its previous recommendation) is subsumed by the
> redesign described here.
>
> **This document does not modify production source.** It captures the architectural
> redesign and a per-file implementation plan. Production source changes land in the
> two implementation iterations the plan describes (Step 4.5a and Step 4.5b in §11).
>
> **Line numbers are anchored to the current `HEAD`** (post-Step-5 of
> `MERGED_REFACTORING_PLAN_2026_05_04.md`); they may shift once implementation lands.
>
> **Revision note (2026-05-06).** §§1–3, 5, 6, 11 of this doc were rewritten to use
> **Shape 1** of the design space — `JavaClassifierType.resolve(...)` and
> `JavaAnnotation.resolveAnnotation(...)` are **deleted from their public interfaces**
> (not narrowed to parameterless overloads). The `classifier: JavaClassifier?` field on
> `JavaClassifierType` and the `classId: ClassId?` field on `JavaAnnotation` — both
> already part of the pre-`java-direct` interface shape — become reliable for *every*
> reference (cross-file too); FIR reads them directly. See §3 ("Why deletion, not
> parameter narrowing") for the rationale. Shape 2 / Shape 3 (parameter narrowing or a
> `java-direct`-internal subinterface) are documented in §12 as fallbacks if Shape 1
> proves infeasible.
>
> **Revision note (2026-05-06, cycle-handling addition).** §§4, 6, 7, 11, 12 were
> further amended to spell out **inheritance-cycle handling** as an explicit concern
> rather than as a side-effect of the timing bug. The proposal now introduces a
> `JavaResolutionContext`-scoped **`JavaSupertypeLoopChecker`** modelled on K1's
> `SupertypeLoopChecker` (see `core/descriptors.jvm/.../context.kt:57`) and FIR's
> `SupertypeComputationStatus.Computing` sentinel
> (see `compiler/fir/resolve/.../FirSupertypesResolution.kt:355,844`); cycles are
> bounded by **three** mechanisms (FIR `Computing` for Kotlin links, the new model-side
> checker for Java links, AST-data immutability for `JavaClass.supertypes`) and surface
> as `CYCLIC_INHERITANCE_HIERARCHY` via `DiagnosticKind.LoopInSupertype` for Java-only
> cycles (which today silently terminate). See new §6.1 for the contract.
>
> **Revision note (2026-05-06, open-questions resolution).** Per the user's answers in
> the issue thread, the four design questions in §12 are now resolved as follows:
> **Q1 — variant C** (a `directSupertypeClassIds()` cache on `FirJavaClass`) is the
> recommended L1-prerequisite, with D (the `FirJavaClass.javaClass` visibility flip)
> demoted to fallback if C proves heavier or causes perf issues; **Q2 — the typed
> `LazySessionAccess` wrapper** is required (failure-mode-1 mitigation moves from
> "optional, recommended" to a hard contract); **Q3 — a shared helper providing a
> minimal `FirSession` impl** is the agreed test-fixture shape; **Q4 — direct
> `FirErrorTypeRef` synthesis** mirroring `breakLoops`'s mechanism is the chosen
> cycle-diagnostic emission path (a small new `FirSession`-scoped
> `JavaSupertypeCycleEdges`-style holder threads recorded edges from the
> `JavaSupertypeLoopChecker` to `FirJavaClass.computeSuperTypeRefsByJavaClass`
> for `LoopInSupertype` → `CYCLIC_INHERITANCE_HIERARCHY`). All four questions are
> now closed; nothing in §12 blocks the implementation iterations from starting
> (modulo the `FirSession`-threading prerequisite, which is itself out of scope —
> see §1).
>
> **Revision note (2026-05-06, merged-plan amendment applied).** The §13 amendment
> (Step 4.5a + Step 4.5b inserts and Step 4 re-classification in
> [`MERGED_REFACTORING_PLAN_2026_05_04.md`](MERGED_REFACTORING_PLAN_2026_05_04.md);
> the failure-mode-1 enforcement update in
> [`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md))
> has been **applied** in the same docs-only iteration that resolves Q4. §13 is
> now a recap of what landed; the merged plan's §5 step list is the authoritative
> execution order. New **§15 "Implementation prerequisites and remaining gaps"**
> records the post-amendment state: nothing else is blocking the Step 4.5a
> iteration except the orthogonal `FirSession`-threading wiring (out of scope per
> §1).
>
> See also: [`AGENT_INSTRUCTIONS.md`](../AGENT_INSTRUCTIONS.md),
> [`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md),
> [`CLASSIFIER_RESOLUTION_TRACE_2026_05_04.md`](CLASSIFIER_RESOLUTION_TRACE_2026_05_04.md),
> [`ITERATION_RESULTS.md`](../ITERATION_RESULTS.md).

---

## 1. Status / scope

This document records the architectural decision to **inject `FirSession` into the
Java Model** rather than continue threading FIR-side callbacks through every
classifier-resolution call. The decision has four consequences that this doc spells
out in detail:

- The model owns the resolution data path. The model holds a `FirSession` of its
  own, and callers (FIR side) no longer construct lambdas to feed back into the
  model.
- The `java-direct`-introduced **`resolve(...)` methods on `JavaClassifierType` and
  `JavaAnnotation` are deleted from their public interfaces**, returning those
  interfaces to their pre-`java-direct` shape. The already-existing `classifier`
  and `classId` fields on those interfaces become reliable for *every* reference;
  FIR reads them directly via the pre-`java-direct` `resolveTypeName` body. See §3.
- The model gains a **`JavaResolutionContext`-scoped `JavaSupertypeLoopChecker`**
  (§6.1) that bounds inheritance cycles on every supertype-walking entry point
  and emits `CYCLIC_INHERITANCE_HIERARCHY` for Java-only cycles. This subsumes
  today's per-call `visited<ClassId>` parameters and the off-by-one
  `findOuterTypeArgsFromHierarchy` skip, and gives the Java arms a typed
  cycle-bound mirroring K1's `SupertypeLoopChecker` and FIR Kotlin's
  `SupertypeComputationStatus.Computing` sentinel.
- Both unification leftovers from `UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md`
  close as natural consequences of the redesign — L1 (drop
  `JavaInheritedMemberResolver`'s Phase 1) closes by construction in Step 4.5a; L2
  (retire `JavaScopeResolver.findLocalClass` + `JavaClassOverAst.findInnerClassInSupertypes`)
  closes in Step 4.5b once the model has direct access to FIR-derived classifier
  data.

**Out of scope for this doc** (and for the implementation iterations described in
§11):

- The **mechanism** by which `FirSession` becomes available inside the Java Model
  (late-init on `JavaClassFinderOverAst`, restructured entry point). This doc states
  it as an **assumption** per the user's instruction. The wiring iteration is its
  own separate piece of work and is not described here.
- LL-FIR semantics and `LLFirJavaSymbolProvider`. Same scope rule as the merged
  plan ([`MERGED_REFACTORING_PLAN_2026_05_04.md` §1](MERGED_REFACTORING_PLAN_2026_05_04.md#1-purpose)):
  this doc describes *compiler-mode* behaviour and the bounded set of FIR-core
  surfaces it touches.

**Previously out of scope, now applied (2026-05-06):** the merged-plan
amendment (Step 4.5a + Step 4.5b inserts and Step 4 re-classification in
[`MERGED_REFACTORING_PLAN_2026_05_04.md`](MERGED_REFACTORING_PLAN_2026_05_04.md))
and the failure-mode-1 enforcement update in
[`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md)
have been **applied** in this same docs-only iteration; see §13 (recap) and
§15 (post-amendment prerequisites for starting Step 4.5a).

---

## 2. Motivation and goal

### What the proposal changes

The Java Model today exposes two FIR-side knowledge points to its callers via
callbacks on a single `resolve(...)` API on `JavaClassifierType` (and a
parallel `tryResolve` callback on `JavaAnnotation.resolveAnnotation(...)`):

1. *"does this `ClassId` exist?"* — `tryResolve: (ClassId) -> Boolean`, today wired
   to `session.symbolProvider.getClassLikeSymbolByClassId(...)` with a builtins
   filter.
2. *"what are the direct supertype `ClassId`s of this class?"* —
   `getSupertypeClassIds: ((ClassId) -> List<ClassId>)?`, today wired to
   `JavaTypeConversion.getResolvedSupertypeClassIds(classId, session)`.

Both lambdas are **pure functions of `FirSession`**. The model has no other input
to them. The callback shape exists *only* because the Java Model historically
cannot see `FirSession` itself; the **`resolve(...)` / `resolveAnnotation(...)`
methods themselves** exist *only* because, pre-injection, FIR cannot reach
`JavaResolutionContext` directly. Both halves of that bridge become unnecessary
once the model holds `FirSession`.

This doc proposes inverting the bridge: the model holds a `FirSession` (assumption
per §1's scope), the callback parameters and the methods that took them **both
vanish from the public interfaces** (`JavaClassifierType.resolve` and
`JavaAnnotation.resolveAnnotation`), and FIR returns to the pre-`java-direct`
shape — `JavaTypeConversion.resolveTypeName` reads `classifier?.classId` directly
(falling back to `findClassIdByFqNameString` then `ClassId.topLevel`), exactly as
it did before the unification branch.

### Why now

Three things changed since the unification track started, all of which point in the
same direction:

- **Steps 2–5 of the merged plan landed**, demonstrating that the existing callback
  layer can be *driven* origin-agnostically (Stage 4 collapsed `findLocalClass` out
  of the `ClassId`-resolution path) but **not silently shrunk**. Two leftovers
  remain (L1 + L2, see `UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md` §1) and
  every phase-light alternative there boils down to a workaround for one specific
  pathology of the callback shape.
- **The trip-wire pair (`testJ_k_complex`, `testMapMethodsImplementedInJava`)
  located the timing bug** (§4 below). The bug is read-side; in the callback shape
  the *only* way to fix it is to push fix-up logic into the callback's body, which
  is the wrong place to encode it (it would then ride on every consumer of the
  callback, not just the BFS that triggers it).
- **Step 6 (PSI Phase 2) is approaching**. Step 6's central invariant is that
  `session.symbolProvider` becomes the canonical origin-agnostic classifier entry
  ([`MERGED_REFACTORING_PLAN_2026_05_04.md` §5 Step 6](MERGED_REFACTORING_PLAN_2026_05_04.md#5-merged-execution-order)).
  The proposal *operationalises* that invariant inside the Java Model rather than
  asking the Java Model to keep avoiding `session.symbolProvider`.

### What the goal is

Stated as four invariants, this is the post-proposal architectural picture:

1. **One deletion drives the whole thing.** `JavaClassifierType.resolve(...)` and
   `JavaAnnotation.resolveAnnotation(...)` are **removed from the public interfaces**;
   the `classifier: JavaClassifier?` and `classId: ClassId?` fields they
   compete with become reliable for every reference. Everything else falls out from
   this. (§3.)
2. **L1 and L2 close naturally — and together.** L1 closes in Step 4.5a
   (`resolve(...)` deletion + callback collapse + Phase 1 drop) because the
   model now dispatches per class to AST data for Java origins, never reading
   `FirJavaClass.superTypeRefs` for the BFS. L2 closes in Step 4.5b because
   the FIR-derived `JavaClass`-shaped view replaces the AST-side fast path
   that survived Stage 4. (§§5, 10, 11.)
3. **Step 6 is strengthened, not just compatible.** The model's new data path —
   `firSession.symbolProvider.getClassLikeSymbolByClassId(...)` for cross-origin
   queries — is *exactly* the post-Step-6 canonical entry. The deletion of
   `CombinedJavaClassFinder` / `BinaryJavaClassFinder` removes a path the model
   already isn't using. (§9.)
4. **The five laziness invariants and three failure modes are upheld**, but
   invariants 1/2/3 move from *structural* enforcement (model has no `FirSession`)
   to *policy* enforcement (KDoc + agent-instruction rule + optional typed
   `LazySessionAccess` wrapper). The risk delta is acknowledged and mitigated.
   (§§7, 8.)

The unification doc's headline goal — *"classifier resolution goes through one
origin-agnostic FIR path"* and *"the AST-side resolver retains only type parameters
+ `containingClassIds` + the same-file fast path"*
([`MERGED_REFACTORING_PLAN_2026_05_04.md` §3](MERGED_REFACTORING_PLAN_2026_05_04.md#3-expected-results),
bullets 3 and 4) — becomes **literally** true post-Step-4.6 (§10 below).

---

## 3. The single load-bearing change: deletion of `resolve(...)`

Everything else in the proposal is bookkeeping. The architectural change is the
**deletion of `JavaClassifierType.resolve(...)`** (and symmetrically
`JavaAnnotation.resolveAnnotation(...)`) from the `core/compiler.common.jvm`
public interface, and a return of the `JavaTypeConversion` resolver to its
pre-`java-direct` body.

### Before

`core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaTypes.kt:85`:

```kotlin
interface JavaClassifierType : JavaType {
    val classifier: JavaClassifier?
    val typeArguments: List<JavaType?>
    val isRaw: Boolean
    val classifierQualifiedName: String
    val presentableText: String
    // ...

    /**
     * Resolves the type to a ClassId using the provided callback.
     * @param tryResolve callback that checks if a ClassId exists. Returns true if found.
     * @param getSupertypeClassIds optional callback that returns the direct supertype ClassIds
     *        for a given ClassId. Used by java-direct to walk supertype chains transitively.
     */
    fun resolve(
        tryResolve: (ClassId) -> Boolean,
        getSupertypeClassIds: ((ClassId) -> List<ClassId>)? = null,
    ): ClassId? = null
}
```

`core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaElements.kt:38`:

```kotlin
interface JavaAnnotation : JavaElement {
    val arguments: Collection<JavaAnnotationArgument>
    val classId: ClassId?
    // ...
    fun resolveAnnotation(tryResolve: (ClassId) -> Boolean): ClassId? = classId
}
```

The sole non-trivial production call site,
`compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt:414`:

```kotlin
private fun JavaClassifierType.resolveSymbolBasedClassId(session: FirSession): ClassId? = resolve(
    tryResolve = { candidateClassId ->
        val symbol = session.symbolProvider.getClassLikeSymbolByClassId(candidateClassId)
        symbol != null && symbol.origin != FirDeclarationOrigin.BuiltIns
    },
    getSupertypeClassIds = { classId -> getResolvedSupertypeClassIds(classId, session) },
)
```

Both lambdas have `FirSession` as their only input. They have no other state.

### After

```kotlin
// javaTypes.kt:85 — pre-java-direct shape, restored.
interface JavaClassifierType : JavaType {
    val classifier: JavaClassifier?           // resolved on demand by the model
    val typeArguments: List<JavaType?>
    val isRaw: Boolean
    val classifierQualifiedName: String
    val presentableText: String
    val containingClassIds: List<ClassId>     // already there post-Step-2
    // No `resolve(...)`. No callbacks.
}

// javaElements.kt:38 — pre-java-direct shape, restored.
interface JavaAnnotation : JavaElement {
    val arguments: Collection<JavaAnnotationArgument>
    val classId: ClassId?
    // ...
    // No `resolveAnnotation(...)`. No callback.
}
```

`JavaTypeConversion` returns to its pre-`java-direct` shape:

```kotlin
// compiler/fir/fir-jvm/.../JavaTypeConversion.kt — pre-java-direct body, restored.
private fun resolveTypeName(name: String, javaType: JavaClassifierType, session: FirSession): ClassId =
    javaType.classifier?.classId
        ?: findClassIdByFqNameString(name, session)
        ?: ClassId.topLevel(FqName(name))
```

`resolveSymbolBasedClassId` (lines 414–420) is **deleted** outright; the lambda
construction goes with it. The `getResolvedSupertypeClassIds(classId, session)`
helper at `JavaTypeConversion.kt:451` is no longer called from the
classifier-resolution path; it is retained (or inlined) only for
`findTypeArgsForClassInHierarchy` at line 517, which is FIR-internal
type-argument substitution math and is *not* on the BFS hot path.

The annotation side is symmetric — `JavaAnnotationOverAst.resolveAnnotation(tryResolve)`
(line 72) and `JavaEnumValueAnnotationArgumentOverAst.resolveEnumClass(tryResolve)`
(line 285) are deleted; their FIR-side callers now read `JavaAnnotation.classId`
directly (it is already populated by the model under injection).

### Where the resolution moves to

Resolution doesn't disappear — it moves *into the model* where it always
belonged structurally. Concretely:

- **`JavaClassifierTypeOverAst.classifier`** (already on the interface; today
  populated for type parameters / local classes / multi-part navigation in
  `computeClassifier()` at `JavaTypeOverAst.kt:106–130`) is **extended to handle
  cross-file references** by consulting `firSession.symbolProvider` with the
  same per-origin dispatch described in §6. Today's null-fallthrough into
  `resolve(callback)` is replaced by the materialiser computing the answer
  itself.
- **`JavaAnnotationOverAst.classId`** is similarly populated by the model
  on demand using `firSession.symbolProvider`, so `JavaAnnotation.classId` is
  reliable for every annotation reference.
- **The model's `JavaResolutionContext.resolve(name)`** becomes a private
  internal helper backing `computeClassifier()` and the annotation `classId`
  materialiser; it has no callbacks because it directly uses `firSession`.

The trivial `JavaClassifierTypeForEnumEntry.resolve()` override at
`JavaTypeOverAst.kt:336` is **deleted entirely** — the type already sets
`classifier = enumClass` (line 326), so `classifier.classId` returns the same
`ClassId.topLevel(enumClass.fqName)` that `resolve(...)` was hand-rolling.

### Why deletion, not parameter narrowing

A first-pass framing of this proposal kept `resolve(...)` on the interface but
simply removed its callback parameters. That framing is incoherent. The
callbacks (`tryResolve`, `getSupertypeClassIds`) and the method itself
(`resolve(...)`) are **two halves of the same bridge** between FIR and the
Java Model:

| Today's leg | Reason it exists | Post-injection |
|---|---|---|
| Callbacks (`tryResolve`, `getSupertypeClassIds`) | Model can't see `FirSession` | Gone — model owns `FirSession` |
| `resolve(...)` on `JavaClassifierType` | FIR can't reach `JavaResolutionContext` | **Should also go** — FIR can read `classifier?.classId` |

Removing only the callbacks while keeping the method narrows the API but does
not address the actual `java-direct`-introduced artefact. The pre-`java-direct`
interface had no `resolve(...)`; PSI-backed and binary-backed implementations
returned `null` from the default impl and let `JavaTypeConversion`'s
`findClassIdByFqNameString` fallback do the work. The only implementation that
*ever* did meaningful work in `resolve(...)` is `JavaClassifierTypeOverAst`,
and that work belongs structurally inside the model — not on a method
projecting it onto a `core/compiler.common.jvm` public surface.

Three additional reasons:

1. **The implementation diff is *shorter*** under deletion than under parameter
   narrowing. Deletion across 9+ override sites is a one-pattern removal;
   parameter-narrowing across the same sites is a per-site signature edit.
2. **The PSI implementation already says "I don't do `resolve`."** The
   default `null` impl on the interface is the smoking gun — `resolve(...)` is a
   `java-direct`-private concern stuffed into a shared interface.
3. **`classifier` already exists.** It is on the interface today, it is
   populated for the same-file / inherited-inner cases, and extending its
   population to cross-file references is exactly the work the
   `firSession`-injection enables (§6 below). The deletion is not a new
   contract; it is a *retirement* of a parallel one.

Shape 2 (move `resolve(...)` to a `java-direct`-private subinterface) and
Shape 3 (FIR calls `JavaResolutionContext` directly via an `is`-check) are
documented in §12 as fallback options if Shape 1 proves infeasible during
implementation. Shape 1 is the recommended path because it most closely
restores the pre-`java-direct` separation of concerns.

### Why this is the load-bearing change

Once `resolve(...)` is deleted, every consumer site collapses to a property
read or a model-internal call. `JavaResolutionContext.resolve`,
`findInheritedNestedClass`, `JavaTypeOverAst.JavaClassifierTypeOverAst.resolve`,
`JavaAnnotationOverAst.resolveAnnotation`, `JavaAnnotationOverAst.resolveEnumClass`,
`JavaInheritedMemberResolver.resolveInheritedInnerClassToClassId` either lose
their public interface surface (the override is deleted) or become private
helpers backed by `firSession`. From the FIR side, the model returns resolved
entities (`ClassId` for classifier resolution via `classifier?.classId`,
implicit `JavaClass`-shaped views for the inherited-inner case once L2 closes)
and nothing else.

§5 enumerates each consumer site and what becomes of it.

---

## 4. Recursive supertype walks: the timing bug and the cycle bound

The phase-light alternatives in
[`UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md` §2](UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md#2-the-timing-bug-cleanly-stated)
analyse the **timing bug** at length; this section recovers a self-contained
read-side characterisation so the proposal reads on its own. The full derivation,
including the cycle-bound and eager-driver-ordering distinctions, lives in §2 of
the alternatives doc and is not repeated here.

The section also makes the **cycle concern** explicit. The deeper structural fact
behind the trip-wire pair is not just that `lazyResolveToPhase(SUPER_TYPES)` is a
no-op in compiler mode — it is that **resolving any Java classifier may transitively
need supertype information for that classifier's own supertypes, and that
transitive walk can cycle** (directly via `A extends A`, indirectly via `A → B → A`,
or through type-argument projections like `A<X extends A<X>>`). Kotlin's FIR avoids
the problem with a dedicated supertype-resolution phase plus a typed
`SupertypeComputationStatus.Computing` sentinel and a separate `breakLoops` pass
([`FirSupertypesResolution.kt:355,844`](../../fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirSupertypesResolution.kt));
K1's Java descriptors mirror that with an injected `SupertypeLoopChecker`
([`core/descriptors.jvm/.../context.kt:57`](../../../core/descriptors.jvm/src/org/jetbrains/kotlin/load/java/lazy/context.kt)).
The Java Model has neither today; the redesign adds the equivalent (§6.1).

### Three cycle bounds the redesign relies on

Under injection, every supertype walk is bounded by **one of three** mechanisms,
dispatched per origin (matching the routing table in §6):

1. **FIR's `SupertypeComputationStatus.Computing` sentinel** — for the **Kotlin /
   built-in / deserialized** arm. Honest in compiler mode (the eager driver finishes
   `SUPER_TYPES` before Java member conversion runs for non-`FirJavaClass`).
2. **AST-data immutability** — for the **source Java** and **binary Java** arms.
   `JavaClass.supertypes` is materialised at parse / class-file load and then
   immutable; reading it cannot trigger re-entrant resolution.
3. **The new `JavaResolutionContext`-scoped `JavaSupertypeLoopChecker`** — a
   model-side active-`ClassId` set, scoped to the resolution context, that wraps
   every supertype-walking entry point and terminates re-entry with a default
   value. This is the model-side analogue of K1's `SupertypeLoopChecker` and is
   what catches Java-only cycles (`A → B → A`-style) that today's BFS swallows
   via per-call `visited<ClassId>` parameters. See §6.1 for the contract.

Mechanism 1 carries the **cycle bound for the Kotlin arm**; mechanisms 2 and 3
together carry it for the Java arms. Mechanism 3 also subsumes the off-by-one
skip in `JavaTypeConversion.findOuterTypeArgsFromHierarchy` (line 481, comment:
*"Accessing its superTypeRefs would cause infinite recursion"*) — that hack
exists today only because no per-class active-resolution sentinel covers
`FirJavaClass` in compiler mode.

### Read-side characterisation

In **compiler mode**, `JavaInheritedMemberResolver`'s Phase 2 BFS reads the direct
supertype `ClassId`s of a class via the `getSupertypeClassIds` callback, today
wired to `JavaTypeConversion.getResolvedSupertypeClassIds(classId, session)`. That
callback does, in three steps:

1. resolve `classId` to a `FirRegularClassSymbol` via
   `session.symbolProvider.getClassLikeSymbolByClassId`;
2. call `classSymbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)`;
3. read `classSymbol.fir.superTypeRefs` and extract a `ClassId` from each.

In compiler mode, `FirLazyDeclarationResolver` is a no-op stub: classes are
assumed to already be at their final phase by the time queries arrive.
`lazyResolveToPhase(SUPER_TYPES)` therefore no-ops *unconditionally*, regardless
of the symbol's actual phase. This is harmless in two of three cases — when the
symbol is at or above `SUPER_TYPES` (eager driver finished it) and when the symbol
is on the active resolution stack (cycle bound, same as LL-FIR). It is **wrong**
in the third case: when the symbol is below `SUPER_TYPES` and **off** the active
stack. In that case, `superTypeRefs` is observable (it's a `lazy {}` delegate on
`FirJavaClass`, see
[`FirJavaClass.kt:115`](../../fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/declarations/FirJavaClass.kt)),
so the read returns a value — but that value may be **partial**: the lazy delegate
fired against not-yet-final `nonEnhancedSuperTypes`, or it has not fired and we
see an empty list.

### One-sentence summary

> `lazyResolveToPhase(SUPER_TYPES)` is a no-op in compiler mode regardless of the
> symbol's actual phase, so the subsequent read of `classSymbol.fir.superTypeRefs`
> may observe a partially-populated lazy list whenever the symbol is below
> `SUPER_TYPES` and off the active resolution stack.

### Why injection sidesteps the bug rather than fixing it in place

The proposal does **not** make `lazyResolveToPhase(SUPER_TYPES)` honest in compiler
mode (that would be Approach A from
[`UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md`](UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md);
the user has rejected A as an FIR-semantics change). Instead, the model — which
under injection knows the class's origin without going through FIR — simply
**stops reading `superTypeRefs` for the Java arms**:

- For source Java classes, the model walks its own AST (`JavaClass.supertypes`)
  directly. No FIR phase is involved.
- For binary Java classes, the model reads supertype `ClassId`s from a new
  **`directSupertypeClassIds()` cache on `FirJavaClass`** populated from
  `javaClass.supertypes` at construction time (variant **C** of the alternatives
  doc; see §11 for the per-file change). No `superTypeRefs` enhancement runs.
  Variant D (a one-line visibility flip on `FirJavaClass.javaClass`) is retained
  as fallback in §12 Q1 if C proves heavier or causes perf issues.
- For Kotlin / built-in / deserialized supertypes, the model still calls
  `lazyResolveToPhase(SUPER_TYPES)` followed by `superTypeRefs` — but in compiler
  mode the eager `FirSupertypeResolverTransformer` finishes those classes before
  Java member conversion runs, so the call is honest there.

The trip-wires `testJ_k_complex` and `testMapMethodsImplementedInJava` are exactly
the cases where the BFS asks for the supertypes of a `FirJavaClass` whose own
`SUPER_TYPES` resolution is in flight; the proposal removes that read entirely
(both arms read AST data instead). The bug becomes structurally inaccessible.

§6 spells out the per-origin routing as concrete pseudocode.

---

## 5. Deletion inventory

This section enumerates every public-interface method and override that
disappears under Shape 1, plus what (if anything) replaces it inside the model.
The table is exhaustive for production sources; test fixtures are listed
separately.

### Public-interface methods deleted

The two methods themselves are removed from the `core/compiler.common.jvm`
public surface, alongside the callback parameters they took:

| Method | File / line | What disappears | What replaces it |
|---|---|---|---|
| `JavaClassifierType.resolve(tryResolve, getSupertypeClassIds): ClassId?` | `core/compiler.common.jvm/src/.../javaTypes.kt:85` | The method (with its default `null` impl), both callback parameters, and the KDoc that documents them. | `JavaClassifierType.classifier?.classId` — already on the interface (line 59); becomes reliable for every reference under injection. |
| `JavaAnnotation.resolveAnnotation(tryResolve): ClassId?` | `core/compiler.common.jvm/src/.../javaElements.kt:67` | The method (with its default `classId` impl) and the callback parameter. | `JavaAnnotation.classId` — already on the interface (line 40); becomes reliable for every annotation reference under injection. |
| Stage-1 plumbing on `JavaResolutionContext.resolve(...)`: `getClassLikeSymbol: ((ClassId) -> JavaResolvedClassLikeSymbol?)?` | `compiler/java-direct/.../resolution/JavaResolutionContext.kt` (Step-2 Stage-1 addition; never wired by any FIR caller) | The third callback, the `JavaResolvedClassOrigin` enum, and the `JavaResolvedClassLikeSymbol` data class. | Direct `firSession.symbolProvider.getClassLikeSymbolByClassId(...)` access from inside the model. The dead-plumbing data class and enum go with the callback. |

### Override sites deleted

Every override of the deleted interface methods becomes itself deleted (no
"narrowed" replacement remains, because the parent declaration is gone):

| Site | File / line | What it does today | Outcome |
|---|---|---|---|
| `JavaClassifierTypeOverAst.resolve(tryResolve, getSupertypeClassIds)` | `compiler/java-direct/.../model/JavaTypeOverAst.kt:305` | One-line forward to `resolutionContext.resolve(rawTypeName, tryResolve, getSupertypeClassIds)`. | **Deleted.** The work moves into `JavaClassifierTypeOverAst.classifier`'s materialiser (`computeClassifier()` at line 106), which is extended to handle cross-file references via `firSession`. |
| `JavaClassifierTypeForEnumEntry.resolve(tryResolve, getSupertypeClassIds)` | `JavaTypeOverAst.kt:336` | Hand-rolls `ClassId.topLevel(enumClass.fqName)` and probes via `tryResolve`. | **Deleted.** The type already sets `classifier = enumClass` (line 326); `classifier.classId` returns the same value with no probe needed. |
| Three other trivial `resolve(...)` overrides | `JavaTypeOverAst.kt:656, 672, 693` | Default `null` impls (no-op overrides). | **Deleted.** Parent declaration is gone; no override needed. |
| `JavaAnnotationOverAst.resolveAnnotation(tryResolve)` | `compiler/java-direct/.../model/JavaAnnotationOverAst.kt:72` | Resolves annotation reference via `resolutionContext.resolve(reference, tryResolve)` (cross-file fallback). | **Deleted.** The work moves into `JavaAnnotationOverAst.classId`'s materialiser, which under injection consults `firSession.symbolProvider` for cross-file cases. |
| `JavaEnumValueAnnotationArgumentOverAst.resolveEnumClass(tryResolve)` | `JavaAnnotationOverAst.kt:285` | Resolves enum-value annotation argument's enum class via `resolutionContext.resolve(className, tryResolve)`. | **Deleted.** The annotation argument exposes its resolved enum class through a (similarly materialised) `JavaClassifier?`-shaped accessor; the FIR-side caller reads that directly. |

### Consumer methods that lose their callback parameters

These are model-internal entry points whose **public** surface stays but whose
**callback parameters** disappear (the callbacks are no longer needed because
the model has `firSession`):

| Site | File / line | Today's signature (abbreviated) | Post-injection signature |
|---|---|---|---|
| `JavaResolutionContext.resolve(name, ...)` | `compiler/java-direct/.../resolution/JavaResolutionContext.kt` | `fun resolve(name, tryResolve, getSupertypeClassIds, getClassLikeSymbol): ClassId?` | `fun resolve(name): ClassId?` — uses internal `tryResolve` / `directSupertypeClassIds` helpers backed by `firSession`. **Becomes the model-internal helper for `computeClassifier()` and the annotation `classId` materialiser.** |
| `JavaResolutionContext.findInheritedNestedClass(...)` | `JavaResolutionContext.kt:91` | `private fun findInheritedNestedClass(outerClassId, nestedName, tryResolve, getSupertypeClassIds, visited): ClassId?` | `private fun findInheritedNestedClass(outerClassId, nestedName, visited): ClassId?` |
| `JavaInheritedMemberResolver.resolveInheritedInnerClassToClassId(...)` | `JavaInheritedMemberResolver.kt:127` | `fun resolveInheritedInnerClassToClassId(outerClassId, nestedName, tryResolve, getSupertypeClassIds): ClassId?` | `fun resolveInheritedInnerClassToClassId(outerClassId, nestedName): ClassId?` |

### What the model gains in exchange

A small set of **private helpers** on `JavaResolutionContext` (or a sibling
utility in `compiler/java-direct/.../resolution/`):

- `tryResolve(classId: ClassId): Boolean` — un-lambda-ised version of today's
  `tryResolve` lambda; same `getClassLikeSymbolByClassId` + builtins-filter body,
  now using `firSession`. Called from `resolve(name)` and from the inherited-inner BFS.
- `directSupertypeClassIds(classId: ClassId): List<ClassId>` — the per-origin
  dispatcher (§6 below). Replaces today's `getSupertypeClassIds` callback /
  `JavaTypeConversion.getResolvedSupertypeClassIds`.
- `LazySessionAccess` — a **typed wrapper** holding `firSession` that is **only**
  reachable from resolution-time code, not from index/cache-population code.
  Adopted as the failure-mode-1 mitigation per the user's resolution of §12 Q2;
  the wrapper makes the laziness invariant typeable rather than reviewable. See
  §8 mode 1 for the contract.

And a new model-side cycle-bound primitive:

- `JavaSupertypeLoopChecker` — a per-`JavaResolutionContext`, thread-local
  active-`ClassId` set that wraps every supertype-walking entry point
  (`directSupertypeClassIds`, `directSupertypes`, `findInheritedNestedClass`'s
  recursive descent, and the BFS in `JavaInheritedMemberResolver`). On re-entry
  to a `ClassId` already on the set, the checker returns a default value (typically
  `emptyList()`) and records the offending edge for diagnostic emission. See §6.1.

And one extension to existing materialisers:

- `JavaClassifierTypeOverAst.computeClassifier()` (`JavaTypeOverAst.kt:106`)
  gains a cross-file branch: when the existing same-file / inherited-inner
  walk returns `null`, the materialiser consults `firSession.symbolProvider`
  via `JavaResolutionContext.resolve(rawTypeName)` and wraps the resulting
  `ClassId` in a thin `JavaClass`-shaped adapter (Step 4.5b builds the adapter;
  Step 4.5a may temporarily synthesize a minimal `JavaClassifier` with only a
  `classId` populated, since `JavaTypeConversion.resolveTypeName` only reads
  `classifier.classId`).
- `JavaAnnotationOverAst.classId` becomes lazily materialised: same pattern as
  `computeClassifier()`, restricted to annotation-class FQN resolution.

### Test fixtures (out of model surface but on the edit path)

Three call sites hand-roll `tryResolve` directly against a synthesized class set
rather than going through the model's `JavaResolutionContext`:

| Site | File / line | Fixture shape |
|---|---|---|
| `paramType.resolve(tryResolve = { ... })` | `compiler/java-direct/test/.../JavaParsingMembersTest.kt:160` | hand-rolled `(ClassId) -> Boolean` over a fixed set |
| `fieldType.resolve(tryResolve = { ... })` | `JavaParsingTypeResolutionTest.kt:147` | same |
| `returnType.resolve(tryResolve = { ... })` | `JavaParsingTypeResolutionTest.kt:315` | same |

Under deletion these three call sites become property reads:

```kotlin
val resolved: ClassId? = paramType.classifier?.classId  // model uses fixture's stubbed FirSession
```

The tests must construct a Java Model whose `firSession` is stubbed (typically
a minimal `FirSession` with a custom `symbolProvider` delegating to the test's
class set) so that `computeClassifier()`'s cross-file branch can answer. The
fixture migration is non-trivial; §11 scopes it as part of Step 4.5a, and §12
lists "test-fixture shape for stubbed `FirSession`" as one of the three open
design questions.

### FIR-side simplification

Beyond the call-site collapse, the FIR side loses three things on net:

- The lambda construction in `JavaTypeConversion.resolveSymbolBasedClassId`
  (`JavaTypeConversion.kt:414–420`).
- `JavaTypeConversion.getResolvedSupertypeClassIds(classId, session)` as a
  callback target (lines 451–457). The function may stay as an FIR-internal
  helper if `findTypeArgsForClassInHierarchy` (line 517) still benefits, or be
  inlined there; either way it stops feeding the model.
- The FIR-side wrapper that builds the `getSupertypeClassIds` callback for
  `JavaInheritedMemberResolver`'s Phase 2 BFS — a small amount of "callback
  choreography" code that disappears with Phase 1 / Phase 2 collapse (§11).

What the FIR side **keeps**:

- The `JavaTypeConversion` core (Java-type → cone-type conversion), exactly as it
  was before the unification branch.
- `findTypeArgsForClassInHierarchy` and `findOuterTypeArgsFromHierarchy`, since
  both serve FIR-internal substitution math and live behind `lazyResolveToPhase`
  calls that are *not* on the BFS hot path (and therefore not affected by the
  timing bug — those `lazyResolveToPhase(SUPER_TYPES)` calls in
  `findTypeArgsForClassInHierarchy` are skipping outer-class indices that have
  already had their supertypes resolved by FIR's bottom-up driver).

That maps onto the *"return to the state before this branch"* phrasing from the
preceding planning round: the **callback-feeding** layer goes; the
**type-conversion** layer stays.

---

## 6. Supertype routing per origin

The crux of the redesign is that, with `firSession` in hand, the model can
decide *per class* where to read direct-supertype data from. The dispatcher
replaces today's `getResolvedSupertypeClassIds(classId, session)` callback;
it lives as a private helper inside the model (typically on
`JavaResolutionContext` or a sibling resolution utility) and is invoked from
`JavaInheritedMemberResolver`'s collapsed BFS — *not* from a public
`resolve(...)` method, which no longer exists under Shape 1 (§3).

The dispatcher is wrapped in `JavaSupertypeLoopChecker.guarded(classId, default = emptyList()) { ... }`
so that direct (`A extends A`) and indirect (`A → B → A`) Java-side cycles
terminate with a logged edge rather than recursing or dead-locking the lazy
delegate. The checker contract is in §6.1; the wrapping shape is the dispatcher
below.

The BFS itself reaches the dispatcher because
`JavaClassifierTypeOverAst.computeClassifier()` (the materialiser of the
`classifier: JavaClassifier?` interface field) consults
`JavaResolutionContext.resolve(rawTypeName)` for cross-file references, which
in turn delegates to `JavaInheritedMemberResolver.resolveInheritedInnerClassToClassId(...)`
for inherited-inner queries. The dispatcher fires once per BFS step.

```kotlin
private fun directSupertypeClassIds(classId: ClassId): List<ClassId> =
    loopChecker.guarded(classId, default = emptyList()) {
        // 1. Source Java arm: walk our own AST. Supertype names are syntactically
        //    knowable; no FIR phase is involved. classFinder here is the
        //    java-direct-side JavaClassFinder (post-Step-6, source-only).
        classFinder.findClass(classId)?.let { javaClass ->
            return@guarded resolveSupertypeNames(javaClass.supertypes, javaClass)
        }

        // 2. Look up the FIR symbol — the model's only handle for non-source-Java
        //    classes (binary Java, Kotlin, deserialized).
        val symbol = firSession.symbolProvider.getClassLikeSymbolByClassId(classId)
            ?: return@guarded emptyList()
        val firClass = symbol.fir as? FirRegularClass ?: return@guarded emptyList()

        // 3. Binary Java arm: read the new pre-resolved cache on FirJavaClass
        //    (variant C of the alternatives doc; see §11 for the per-file change).
        //    The cache is populated from javaClass.supertypes at FirJavaClass
        //    construction time and exposes the supertype ClassIds without going
        //    through the lazy `superTypeRefs` enhancement. Variant D (one-line
        //    visibility flip on FirJavaClass.javaClass) is the §12 Q1 fallback if
        //    C proves heavier or causes perf issues.
        if (firClass is FirJavaClass) {
            return@guarded firClass.directSupertypeClassIds()
        }

        // 4. Kotlin / built-in / deserialized arm: FIR's lazy-phase model is honest
        //    here in compiler mode (eager driver finishes SUPER_TYPES before Java
        //    member conversion runs for non-FirJavaClass classes), so the
        //    lazyResolveToPhase(SUPER_TYPES) + superTypeRefs read is correct.
        //    Cycles on this arm are bounded by FIR's SupertypeComputationStatus.Computing
        //    sentinel, not by the model-side checker (which is for Java arms).
        symbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
        firClass.superTypeRefs.mapNotNull { ref ->
            (ref.coneTypeOrNull as? ConeClassLikeType)?.lookupTag?.classId
        }
    }

private fun resolveSupertypeNames(
    supertypes: Collection<JavaClassifierType>,
    enclosing: JavaClass,
): List<ClassId> = supertypes.mapNotNull { supertype ->
    // Read the materialised `classifier` field directly. Under Shape 1 the
    // public `JavaClassifierType.resolve(...)` method is gone; resolution
    // happens inside `JavaClassifierTypeOverAst.computeClassifier()` and the
    // result is cached on the `classifier` field.
    (supertype.classifier as? JavaClass)?.classId
}
```

### Notes on the dispatcher

- **No phase reads on Java arms.** Steps 1 and 3 never touch
  `firClass.superTypeRefs` and never call `lazyResolveToPhase` on a `FirJavaClass`.
  That is the structural reason the timing bug (§4) cannot fire under injection.
- **Cycles on Java arms are bounded by `loopChecker.guarded(...)`.** Direct
  self-cycles (`A extends A`) and indirect cycles (`A → B → A`) terminate at
  the second entry; the offending `(parentClassId, supertypeClassId)` edge is
  recorded for diagnostic emission (§6.1).
- **`resolveSupertypeNames` is a property read into the materialised
  `classifier` field.** Each `JavaClassifierType` from the AST has its
  `classifier: JavaClassifier?` populated by `computeClassifier()`, which
  uses the same dispatcher for *its* nested-supertype probes, etc. The
  recursion bound is the new `JavaSupertypeLoopChecker`, not a per-call
  `visited<ClassId>` parameter — that's a strict simplification of today's
  ad-hoc bookkeeping. **Note:** the recursive read is a *property access*, not
  a method call — caching on the `classifier` field memoises the answer for
  repeated reads of the same type reference (e.g., when the same supertype
  name appears in multiple positions).
- **Cross-origin chains compose correctly.** If a Java class extends a Kotlin
  class which itself extends a Java class, the BFS walks the first Java arm via
  AST (step 1 or 3), hops to FIR for the Kotlin arm (step 4), then comes back to
  AST when the chain re-enters Java. The dispatcher is per-class, so the per-hop
  dispatch is the only thing that needs to happen — there is no "top-level
  origin" choice to get wrong.
- **Binary `JavaClass.supertypes` are not enhanced.** AST-side `JavaClass`
  instances (both source `JavaClassOverAst` and binary `BinaryJavaClass`) carry
  their supertype names as written, with no `@NotNull` / `@Nullable` annotations
  applied and no `Mutable*` collection mappings. The BFS only needs `ClassId`s,
  where enhancement is irrelevant — this is documented as a hazard in §11's KDoc
  for the new `directSupertypeClassIds()` cache on `FirJavaClass` (variant C of
  §12 Q1; also applies to the fallback variant D if it ever needs to be taken).
- **Builtins filtering moves into the model.** Today's `tryResolve` lambda
  rejects `FirDeclarationOrigin.BuiltIns` (PSI-parity for stdlib classes when
  stdlib is on the classpath); the model's `tryResolve(classId)` helper applies
  the same filter. The dispatcher above does *not* apply it — supertype walks
  legitimately need to traverse builtin links — which matches today's behaviour
  (the existing callback's body in `JavaTypeConversion.kt:419` does not filter
  builtins from supertype reads either).

### Cross-origin re-entry

The `Java → Kotlin → Java` chain is the worked example. `KJKComplexHierarchyNestedLoop.kt`
(in `compiler/fir/analysis-tests/testData/resolveWithStdlib/problems/`) is the
motivating test data; it explicitly hits the cross-origin reduction. Walking
through the dispatcher with the chain `Derived(Java) → Mid(Kotlin) → Base(Java)`
where `Base` re-enters at `Derived`:

- `directSupertypeClassIds(Derived)` → enters via `loopChecker.guarded(Derived, ...)`,
  active set = `{Derived}`.
- Step 1 hits (source Java); `resolveSupertypeNames` reads `Derived.supertypes` =
  `[Mid]`. Returns `[Mid]`. Active set pops back to `{}`.
- BFS recurses to `Mid`. `directSupertypeClassIds(Mid)` → enters via
  `loopChecker.guarded(Mid, ...)`, active set = `{Mid}`.
- Step 1 / Step 3 miss (Mid is Kotlin). Step 4: `lazyResolveToPhase(SUPER_TYPES)`
  on `Mid` is honest because `Mid` is a `FirRegularClass`-Kotlin (FIR's eager
  driver finished it). The read returns `[Base]`. Returns `[Base]`. Active
  set pops.
- BFS recurses to `Base`. `directSupertypeClassIds(Base)` → enters via
  `loopChecker.guarded(Base, ...)`, active set = `{Base}`.
- Step 1 hits; `resolveSupertypeNames` reads `Base.supertypes`. If `Base.supertypes`
  syntactically references `Derived` (the cycle), the recursive
  `directSupertypeClassIds(Derived)` enters `loopChecker.guarded(Derived, ...)` —
  active set is `{Base}`, so `Derived` is added cleanly and the walk continues;
  the cycle is caught at the **next** descent (`directSupertypeClassIds(Base)`'s
  re-entry), not here. This is correct because the re-entry is what marks the
  cycle, not the first visit.

The contract: the active set lives **per-thread, per-`JavaResolutionContext`**.
A Kotlin lookup that bounces back through `firSession.symbolProvider` and
ultimately re-asks the model for a Java class will hit the active set on the
*same thread* — provided the model never spawns supertype walks on a different
thread. This is documented as an invariant on the checker's KDoc (§6.1) and
in a `@VisibleForDocumentation` comment in `JavaResolutionContext`.

---

## 6.1 Inheritance-cycle handling

### Background

Kotlin's FIR handles cycles by structure: `FirSupertypeResolverProcessor` runs as
a dedicated, eager pass before any consumer reads `superTypeRefs`; per-class
status is tracked in `SupertypeComputationStatus` (`NotComputed` / `Computing` /
`Computed`); on re-entry to a `Computing` class the resolver returns a synthesised
`FirErrorTypeRef` with `DiagnosticKind.LoopInSupertype`
(see [`FirSupertypesResolution.kt:355,825`](../../fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirSupertypesResolution.kt));
then `breakLoops` (line 844) runs a DFS over the result and substitutes error
types onto the offending edges. Two distinct responsibilities — **terminate**
the walk, then **report** the diagnostic.

K1's Java descriptors mirror that with the `SupertypeLoopChecker` injected into
`JavaResolverComponents`
([`core/descriptors.jvm/.../context.kt:57`](../../../core/descriptors.jvm/src/org/jetbrains/kotlin/load/java/lazy/context.kt));
`LazyJavaClassDescriptor` and `LazyJavaTypeParameterDescriptor` invoke it inside
their `getSupertypes()` materialisation. Same two responsibilities.

The Java Model has neither today. Pre-injection, cycles are caught only by
per-call `visited<ClassId>` parameters in `JavaInheritedMemberResolver`'s BFS
and its analogues in `JavaSupertypeGraph` and `findInnerClassFromSupertypes`.
Those terminate the walk but never emit a diagnostic — Java-only inheritance
cycles silently truncate to whatever the `visited` set caught.

### Contract

`JavaSupertypeLoopChecker` is a per-`JavaResolutionContext` component that owns
a thread-local active-`ClassId` set and provides one primitive:

```kotlin
internal class JavaSupertypeLoopChecker {
    private val resolving = ThreadLocal.withInitial { mutableSetOf<ClassId>() }
    private val cycleEdges = mutableListOf<Pair<ClassId, ClassId>>()  // parent → supertype

    inline fun <R> guarded(classId: ClassId, default: R, block: () -> R): R {
        val active = resolving.get()
        if (!active.add(classId)) {
            // Cycle detected. Record the offending edge for diagnostic emission;
            // terminate the walk with `default`.
            recordCycleEdge(active.last(), classId)
            return default
        }
        try { return block() } finally { active.remove(classId) }
    }

    fun consumeCycleEdges(): List<Pair<ClassId, ClassId>> = ...
}
```

Responsibilities:

1. **Terminate.** On re-entry to a `ClassId` already on the active set,
   `guarded` returns `default` (typically `emptyList()` for the dispatcher,
   or `null` for `findInheritedNestedClass`'s recursive descent). Subsumes:
   - The `visited<ClassId>` parameter in `JavaInheritedMemberResolver.resolveInheritedInnerClassToClassId`
     (line 127).
   - The `visited<JavaClass>` set in `JavaInheritedMemberResolver.findInnerClassFromSupertypes`.
   - The per-call `visited` in `JavaSupertypeGraph.collectInheritedInnerClasses`.
   - The `visited<ClassId>` parameter in
     `JavaTypeConversion.findTypeArgsForClassInHierarchy` (if and when that
     function migrates into the model; it stays FIR-side under Step 4.5a).
   - The off-by-one `i in 1 until containingClassIds.size` skip in
     `JavaTypeConversion.findOuterTypeArgsFromHierarchy:481` — the structural
     prevention there exists today only because no per-class active-resolution
     sentinel covers `FirJavaClass` in compiler mode.
2. **Report.** On detection, the offending `(parentClassId, supertypeClassId)`
   edge is recorded. The cycle-reporting mechanism — resolved per **§12 Q4**
   to **direct `FirErrorTypeRef` synthesis** mirroring `breakLoops`'s
   substitution mechanism — feeds the recorded edges into
   `FirJavaClass.computeSuperTypeRefsByJavaClass`'s output (or into the
   `directSupertypeClassIds()` cache landed under §12 Q1 / variant C, depending
   on which location owns the supertype list at diagnostic-emission time):
   for each edge a `FirErrorTypeRef` carrying
   `ConeSimpleDiagnostic(LoopInSupertype)` is substituted in place of the
   offending supertype. The existing
   `coneDiagnosticToFirDiagnostic.kt:1048` mapping turns that into
   `CYCLIC_INHERITANCE_HIERARCHY`. The wiring point that exposes the recorded
   edges to `FirJavaClass`'s computation site (a small amount of new plumbing
   between `JavaResolutionContext` and `FirJavaFacade`) is the remaining
   implementation decision; the fallback (a separate FIR-side checker that
   consumes the edges after the BFS returns) is documented in §12 Q4 for
   reference but is not the chosen path.

### Coverage delta

Where the proposal lands today vs. with the loop checker added:

| Cycle scenario | Pre-redesign | Proposal as written (no checker) | Proposal + `JavaSupertypeLoopChecker` |
|---|---|---|---|
| Direct Java cycle (`A extends A`) inside BFS | terminated by `visited` set; no diagnostic | terminated by `visited` set; no diagnostic | terminated by checker; **diagnostic emitted** |
| Indirect Java cycle (`A → B → A`) inside BFS | terminated by `visited` set; no diagnostic | terminated by `visited` set; no diagnostic | terminated by checker; **diagnostic emitted** |
| Direct Java cycle in cross-file `classifier` materialiser | does not arise (classifier is source-only pre-injection) | **NOT bounded** — re-enters `firSession.symbolProvider` | terminated by checker |
| Indirect Java cycle in cross-file `classifier` materialiser | does not arise | **NOT bounded** | terminated by checker |
| `Java → Kotlin → Java` re-entry | bounded by Phase-1's `visited<ClassId>` *and* FIR's `Computing` sentinel | bounded by FIR's `Computing` *and* `visited<ClassId>` (per BFS) | bounded by FIR's `Computing` *and* checker (across all surfaces) |
| `findOuterTypeArgsFromHierarchy` self-edge | bounded by ad-hoc `i in 1 until` skip | unchanged | replaceable by checker (cleaner; deferred to a follow-up if/when the function migrates into the model) |
| Diagnostic emission for Java cycles | none | none | **uniform emission via `LoopInSupertype` → `CYCLIC_INHERITANCE_HIERARCHY`** |

The checker turns the proposal from "closes the unification leftovers but
regresses cycle-bound coverage on the new cross-file `classifier` materialiser"
into **strictly stronger than today** — same BFS coverage, plus the new
classifier path is bounded, plus diagnostic parity with Kotlin.

---

## 7. Laziness invariants — re-audit under injection

The five invariants from
[`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md` §"Five laziness invariants"](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md#five-laziness-invariants-the-unification-must-keep)
all still apply. What changes is **where they're enforced**: invariants that were
*structurally* inaccessible to violation (because the model had no `FirSession`)
become *policy*-enforced (KDoc + agent-instruction rule + optional typed
wrapper). The risk delta is per-invariant:

| Invariant | Today's enforcement | Post-injection enforcement | Risk delta | Mitigation |
|---|---|---|---|---|
| **1. No speculative `getClassLikeSymbolByClassId` calls.** Every probe must be on the JLS resolution path of an actual reference. | Structural — model has no `FirSession`. | Policy — model *can* call `firSession.symbolProvider`; rule moves to KDoc / agent instructions. | **↑** | KDoc on `tryResolve` and `directSupertypeClassIds` helpers; agent-instructions update; **required typed `LazySessionAccess` wrapper** (per §12 Q2) so cache-population code physically cannot reach the symbol provider — see §8 mode 1. |
| **2. No walking of `superTypeRefs` for inherited-inner-class lookup outside an active resolution.** | Structural — same. | Structural — same: `directSupertypeClassIds` is invoked only from `JavaInheritedMemberResolver`'s BFS, which is itself reached only from `JavaClassifierTypeOverAst.computeClassifier()` (the `classifier` materialiser), which is reached only from FIR's active resolution via `JavaTypeConversion.resolveTypeName`'s `classifier?.classId` read. **Improved** for the Java arms specifically: they no longer read `superTypeRefs` at all (§6 steps 1 and 3). | **↓ (improved)** | None needed; structural. |
| **3. `lazyResolveToPhase` is the cycle bound, not eagerness.** `tryResolve` must never internally trigger a `SUPER_TYPES` advance. | Structural — `tryResolve`'s body lives FIR-side and is hand-written to call only `getClassLikeSymbolByClassId`. | Policy — same body, now in the model. KDoc on `tryResolve(classId)` helper records "must not advance phase beyond what `getClassLikeSymbolByClassId` already does". The model's supertype-walk (`directSupertypeClassIds`) is still the *only* call site that calls `lazyResolveToPhase(SUPER_TYPES)` — and only on the Kotlin / built-in / deserialized arm (§6 step 4). | **↑** (moved from "can't" to "must not") | KDoc + agent-instructions rule; the tight call-site distribution (one helper per role) makes the rule mechanical to check. |
| **4. Cycles are bounded.** Re-entry to an already-resolving class must terminate cleanly (no deadlock, no partial reads, no silent truncation). | Three loosely-coordinated mechanisms: FIR's `SupertypeComputationStatus.Computing` for Kotlin links; per-call `visited<ClassId>` sets for Java links inside `JavaInheritedMemberResolver` / `JavaSupertypeGraph`; and `lazyResolveToPhase`'s no-op-on-active-stack semantics for `FirJavaClass` (the fragile leg, §4). Java-only cycles silently truncate; no diagnostic emitted. | Three **typed and complementary** mechanisms: FIR `Computing` for Kotlin (unchanged); **AST-data immutability** for `JavaClass.supertypes` reads (always safe — immutable, materialised at parse time); and the new **`JavaSupertypeLoopChecker`** for Java links (per-`JavaResolutionContext`, thread-local active-`ClassId` set; §6.1). The cross-file `classifier` materialiser introduced by injection is bounded by the new checker; **invariant 4 becomes strictly stronger** because Java-only cycles now emit `CYCLIC_INHERITANCE_HIERARCHY` (§6.1's Q4 mechanism) rather than silently truncating. | **↑ for the new cross-file surface; ↓ overall** | The `JavaSupertypeLoopChecker` (§6.1); §11 adds it as a per-file row in Step 4.5a and a `KJKComplexHierarchyNestedLoop.kt` + pure-Java-cycle entry in the validation gate. |
| **5. The same-package fast path doesn't widen the index.** | Structural — the package-index check is a `LeanJavaClassFinder` short-circuit, not an FIR call. | Structural — same. The model's same-package fast path is unchanged by injection (it never went through FIR). | **flat** | None needed. |
| **6. No eager parsing.** `JavaClassCache.getOrPutIfNotNull` stays the only entry point to `parseJavaSourceFile`. | Structural — `JavaClassFinderOverAstImpl` is in the chain. | Structural — same. The model's parsing primitive is unchanged. The only new code (the dispatcher) calls `classFinder.findClass(...)`, which is `JavaClassCache.getOrPutIfNotNull` under the hood. | **flat** | None needed. |

### What moves from structural to policy — and why

Invariants 1 and 3 carry the bulk of the risk delta. Today, anyone editing the
Java Model cannot accidentally introduce a `getClassLikeSymbolByClassId` call
inside `LeanJavaClassFinder.indexFile` (or any other cache-population code) —
the type system rules it out, because `LeanJavaClassFinder` doesn't see FIR.
Post-injection, the type system permits it; only KDoc, code review, and (if
adopted) the typed `LazySessionAccess` wrapper rule it out.

The mitigation has three tiers, and per the user's resolution of §12 Q2 **all
three are adopted** (the typed wrapper is no longer optional):

1. **KDoc on every helper** that holds `firSession` declares its laziness
   contract (e.g., on `tryResolve`: *"Must call only `getClassLikeSymbolByClassId`
   plus the builtins filter; no `lazyResolveToPhase` advance."*).
2. **`AGENT_INSTRUCTIONS.md`-side rule** — a new bullet in the laziness section:
   *"Do not call `firSession.symbolProvider` from cache-population code (anything
   reachable from `JavaClassCache` / `LeanJavaClassFinder.indexFile` /
   `JavaSupertypeGraph`-population). Resolution-time code only."*
3. **Required typed wrapper** — `LazySessionAccess` is a value class around
   `FirSession` constructed only at resolution-context boundaries; the symbol
   provider is reachable only through a method on this wrapper. Code that does
   not see the wrapper cannot reach the symbol provider. This makes the
   invariant **typeable** rather than merely reviewable. See §8 mode 1.

The wrapper is now part of the Step 4.5a deliverable (§11); the implementation
iteration cannot defer it to a follow-up. Tier 1 + Tier 2 alone are insufficient
because they rely on code review forever after; Tier 3 makes the failure mode
detectable at compile time. See §12 Q2 for the rationale and §11 for the
per-file landing point.

### Per-class laziness profile is unchanged

The unification doc's bottom-line claim — *"on a per-class basis, the laziness
profile of the unified code matches the current code"* — is preserved. The
proposal does not add any new probes; it relocates existing ones.

- Today: every classifier resolution does N `getClassLikeSymbolByClassId` probes
  (one per candidate `ClassId`) plus 1 `getResolvedSupertypeClassIds` per
  inherited-inner BFS step.
- Post-injection: every classifier resolution does N `getClassLikeSymbolByClassId`
  probes (same call site, now in the model) plus 1 `directSupertypeClassIds` per
  inherited-inner BFS step. The Java arms of the latter avoid a
  `lazyResolveToPhase(SUPER_TYPES)` no-op + a `superTypeRefs` lazy-delegate
  enhancement read; the Kotlin arm is unchanged. **Net structurally cheaper or
  equal.**

---

## 8. Three failure modes — re-audit under injection

The three failure modes from
[`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md` §"Three failure modes"](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md#three-failure-modes-to-guard-against)
all still apply. The risk profile per mode:

### Mode 1 — Eager batched symbol lookups inside cache population

**Risk: ↑.** Today, the model cannot execute a batched
`getClassLikeSymbolByClassId` call inside `JavaClassCache` /
`LeanJavaClassFinder.indexFile` because the model has no `FirSession`.
Post-injection, the structural prevention is gone; it is replaced by policy.

The pathology to guard against is unchanged: a future change to
`findInheritedInner` (or any cache-population path) that "preloads" all
inherited classes via a batched symbol lookup would force every `ClassId` in
the supertype closure into a symbol — including supertypes of supertypes that
the original AST walk never had to touch. The current AST walk is
intentionally one-step-at-a-time (BFS guided by visited set); the model's
post-injection BFS must do the same.

**Mitigation — three tiers, all three adopted (per §12 Q2 resolution).**

1. **KDoc on `JavaCacheCache`-and-friends entry points** declaring the no-FIR rule.
2. **`AGENT_INSTRUCTIONS.md` rule** explicitly forbidding `firSession.symbolProvider`
   calls from cache-population code.
3. **Required typed `LazySessionAccess` wrapper.** Sketch:

   ```kotlin
   /**
    * Capability token for resolution-time access to FirSession.
    *
    * Held by JavaResolutionContext and passed only along resolution-time code
    * paths; cache-population code (JavaClassCache, LeanJavaClassFinder.indexFile,
    * JavaSupertypeGraph collection) does not see this type and therefore
    * cannot reach the symbol provider.
    */
   @JvmInline
   value class LazySessionAccess(private val session: FirSession) {
       fun classLikeSymbol(classId: ClassId): FirClassLikeSymbol<*>? =
           session.symbolProvider.getClassLikeSymbolByClassId(classId)
       fun lazyResolveSupertypes(symbol: FirRegularClassSymbol) {
           symbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
       }
       // ... no other surface exposed.
   }
   ```

   This makes failure-mode 1 **typeable**: a reviewer-and-CI-checkable invariant
   rather than a code-review one.

### Mode 2 — Routing type-parameter lookups through FIR

**Risk: flat.** Type parameters of an outer Java class are syntactic — they live
on `JavaClass.typeParameters` and are answerable from the AST without any
resolution. The model's post-injection `findTypeParameter` /
`findInheritedTypeParameter` are unchanged by injection; both still go through
the AST.

**Mitigation.** Same as today: `JavaScopeResolver`'s `findTypeParameter` /
`findInheritedTypeParameter` stay AST-resolved (Stage 2 of the unification
doc's migration plan covers this). KDoc on those helpers re-states the
invariant.

### Mode 3 — Cross-file flexibility checks asking for full class symbols

**Risk: flat or improved.** `isTriviallyFlexibleHint` / `isRaw` /
`containingClassIds` are read as part of every Java type conversion. Today they
go through the AST for source classes and through the FIR symbol for cross-file
references. Post-injection, the model has the same access pattern — it can
choose the cheaper path per origin.

The unification doc's specific recommendation — *"caching the resolved `ClassId`
plus its type-param count on the `JavaClassifierType` would be a strict
improvement (less work, same lazy profile)"* — is **easier** under injection
because the model now has direct access to `firSession.symbolProvider` and can
populate the cache in one place rather than threading a callback in.

**Mitigation.** Optional cache as the unification doc recommends; not required
by this proposal.

### Summary table

| Failure mode | Risk under injection | Mitigation tier | Notes |
|---|---|---|---|
| 1. Batched eager symbol lookups in cache population | ↑ | KDoc + agent-instructions + **required** typed `LazySessionAccess` (per §12 Q2) | Was structural; becomes policy. The wrapper makes it typeable again. |
| 2. Type-parameter lookups routed through FIR | flat | KDoc; same as today | Stage 2 of the unification doc's plan covers it; injection does not change the access pattern. |
| 3. Cross-file flex checks asking for full symbols | flat or ↓ | Optional cache (already recommended pre-injection) | Injection makes the cache *easier* to implement; not a regression. |

---

## 9. Step-6 (PSI Phase 2) compatibility

The proposal *strengthens* Step 6 of `MERGED_REFACTORING_PLAN_2026_05_04.md`
([§5 Step 6](MERGED_REFACTORING_PLAN_2026_05_04.md#5-merged-execution-order)) —
it does not just leave it compatible. The reasoning mirrors §4 of
[`UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md`](UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md#4-compatibility-with-step-6-psi-phase-2)
but moves further along the spectrum: under injection, the model **uses** what
Step 6 makes canonical.

### Step 6's invariants, restated

Step 6 delivers four invariants relevant to this proposal:

- `JavaSymbolProvider` becomes source-only. Binary Java lookups move to
  `JvmClassFileBasedSymbolProvider` (a *symbol provider*, not a `JavaClassFinder`).
- `CombinedJavaClassFinder` and `BinaryJavaClassFinder` are deleted. The only
  surviving `JavaClassFinder` is the source-side one (AST-backed by default).
- `FirJavaFacade.classFinder` becomes the source-only finder.
- `session.symbolProvider` becomes the canonical origin-agnostic classifier
  entry.

### The proposal's data path is *exactly* the post-Step-6 canonical entry

`directSupertypeClassIds(classId)` (§6) dispatches per origin:

- **Source Java arm** uses `classFinder.findClass(...)` — which post-Step-6 is
  the source-only AST finder. Unchanged.
- **Binary Java arm** uses `firSession.symbolProvider.getClassLikeSymbolByClassId(...)?.fir as? FirJavaClass`'s
  `javaClass` field. The route from a `ClassId` to a `BinaryJavaClass` is *not*
  through any `JavaClassFinder` — it is through the symbol provider, which is
  exactly Step 6's deletion of `CombinedJavaClassFinder` /
  `BinaryJavaClassFinder` translated into the model.
- **Kotlin / built-in / deserialized arm** uses `firSession.symbolProvider`
  directly. Unchanged by Step 6 (`session.symbolProvider` is *the* canonical
  entry there).

When Step 6 lands, the model already isn't using anything Step 6 deletes. The
indirect callers enumerated in
[`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` §1.5](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md#1.5)
plug into a fully-unified resolver that already goes through `session.symbolProvider`.
The merged-plan §6 third-bullet rationale ([*"audit becomes a propagation, not
an invention"*](MERGED_REFACTORING_PLAN_2026_05_04.md#6-rationale-for-this-execution-order))
gets *stronger* under injection because there is no L2 residue left for the
indirect-caller audit to navigate around.

### Contrast with E (rejected by §4 of the alternatives doc)

Approach E in
[`UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md` §3](UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md#3-the-five-phase-light-alternatives)
proposed widening `LeanJavaClassFinder.collectInheritedInnerClasses` to walk
both source and binary `JavaClass` instances — making the BFS origin-agnostic
*on the AST side*. That approach is structurally incompatible with Step 6: it
requires a binary-side `JavaClassFinder` arm (`CombinedJavaClassFinder`'s
fall-through into `BinaryJavaClassFinder`), which Step 6 deletes. See
[`UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md` §4 "E — origin-agnostic AST-side BFS"](UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md#e--origin-agnostic-ast-side-bfs-in-leanjavaclassfindercollectinheritedinnerclasses)
for the four `JavaSupertypeGraph` source-only primitives that block E.

The proposal makes E's structural dependency **vacuous**: the BFS is no longer
"on the AST side"; it is in `JavaInheritedMemberResolver`, which under injection
dispatches per class to AST data (Java arms) or to the symbol provider (Kotlin
arm). No binary-side `JavaClassFinder` arm is involved, so Step 6's deletion
does not affect the BFS at all.

### Compatibility verdict

| Step 6 deliverable | Effect on this proposal |
|---|---|
| `JavaSymbolProvider` becomes source-only | None — the proposal's binary arm goes through `firSession.symbolProvider`, not `JavaSymbolProvider`. |
| `CombinedJavaClassFinder` + `BinaryJavaClassFinder` deleted | None — the proposal does not use either. The Java arms read AST data directly (source) or via `FirJavaClass.javaClass` (binary). |
| `FirJavaFacade.classFinder` becomes source-only | None — the proposal's source arm uses the source finder; the binary arm does not need a finder. |
| `session.symbolProvider` becomes canonical | **Aligned** — the proposal's binary / Kotlin / built-in / deserialized arms all already go through `firSession.symbolProvider`. |

Verdict: **strengthened, not just compatible**. The post-Step-6 architecture is
the natural target of the proposal; the model's data path is what Step 6
operationalises across the rest of the compiler.

---

## 10. What L1 / L2 / unification headline look like post-proposal

Mapping the redesign onto the leftovers from §1 of
[`UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md`](UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md#1-what-is-left-to-close):

| Item | Status pre-proposal (post-Step-5) | Closure mechanism | Lands in |
|---|---|---|---|
| **L1 — drop `JavaInheritedMemberResolver` Phase 1.** | Open. Phase 1 (source-index walk) and Phase 2 (FIR-callback walk) coexist; Phase 1 is load-bearing because Phase 2 hits the timing bug (§4) for cross-file `JavaSource → JavaSource → …` chains. | The two-phase BFS collapses into a **single per-class-dispatched origin-agnostic loop** that uses `directSupertypeClassIds(classId)` (§6). Phase 1's source-only walk reduces to a **fast path inside the new loop** for source classes (it is structurally the same as step 1 of the dispatcher). The timing bug cannot fire because the Java arms never read `superTypeRefs`. | Step 4.5a (§11). |
| **L2 — retire `JavaScopeResolver.findLocalClass` + `JavaClassOverAst.findInnerClassInSupertypes`.** | Open. After Step 4 these survive as a **structural-`JavaClass` fast path** for `JavaTypeOverAst.computeClassifier` / `JavaClassCache` / `ConstantEvaluator`, because `getClassLikeSymbol` alone does not yield a `JavaClass` with a full outer-chain. | The model's classifier-resolution path consumes a `JavaClass`-shaped view derived from `firClass.javaClass` for `FirJavaClass` (source and binary alike) or a thin adapter over `FirRegularClass` for Kotlin. This is Approach B from `UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md`, *but vastly simpler under injection*: the adapter is a **passthrough** for `FirJavaClass` (we already have the `JavaClass`) and only constructs a structural view for the rare Kotlin-classifier case. | Step 4.5b (§11). |
| **Unification headline** — *"classifier resolution goes through one origin-agnostic FIR path"* and *"the AST-side resolver retains only type parameters + `containingClassIds` + the same-file fast path"*. | Half-true. The `ClassId`-resolution axis is unified (Step 4); the structural-`JavaClass` axis is not (L2 above). | Both axes converge on `firSession.symbolProvider`. Post-Step-4.6 the AST-side resolver shrinks to `findTypeParameter` + `findInheritedTypeParameter` + `getContainingClassIds()` + the same-file fast path — **literally** what the merged plan §3 promises. | Post-Step-4.6 (§11). |

### Why L1 and L2 close together

L1 and L2 are entangled because both ultimately need a way to read a class's
direct supertypes without depending on the FIR phase clock being honest. Under
injection:

- L1's BFS reads `directSupertypeClassIds(classId)` (the `ClassId`s only).
- L2's adapter reads the same `JavaClass.supertypes` (full structural data,
  including `JavaClassifierType.typeArguments` for substitution math).

Both are answered from the same data source — `firClass.javaClass.supertypes`
for `FirJavaClass` and `classFinder.findClass(...)` for the source case. The
implementation iteration that closes L1 (Step 4.5a) puts the dispatcher in
place; the iteration that closes L2 (Step 4.5b) extends it to return structural
data instead of just `ClassId`s.

---

## 11. Implementation plan

Two implementation iterations. Each has a per-file change table, an explicit
validation gate, and a perf-counter expectation. The iterations are sequenced
so each gate run isolates a single redesign's effect (mirroring the merged
plan's "each gate run isolates a single redesign's effect" invariant —
[`MERGED_REFACTORING_PLAN_2026_05_04.md` §6](MERGED_REFACTORING_PLAN_2026_05_04.md#6-rationale-for-this-execution-order)).

> **Pre-requisite (out of scope for both steps).** `FirSession` is available
> inside the Java Model. The mechanism is left to a separate iteration (likely
> late-init on `JavaClassFinderOverAst` first, restructured entry point later).
> Steps 4.5a and 4.5b assume the wiring is in place.

### Step 4.5a — `FirSession` injection + `resolve(...)` deletion + drop Phase 1 (closes L1)

**Goal.** The load-bearing **deletion** (§3) lands: `JavaClassifierType.resolve(...)`
and `JavaAnnotation.resolveAnnotation(...)` are removed from the public
interfaces; the `classifier` and `classId` fields become reliable for every
reference; every override site (§5) is **deleted**;
`JavaInheritedMemberResolver`'s two-phase BFS collapses into a
per-class-dispatched origin-agnostic loop using `directSupertypeClassIds` (§6)
wrapped in `JavaSupertypeLoopChecker.guarded(...)` (§6.1);
`FirJavaClass` gains a new `directSupertypeClassIds()` cache (variant **C** of
§12 Q1; `private val javaClass` stays `private`); the typed `LazySessionAccess`
wrapper is introduced as the failure-mode-1 mitigation (§12 Q2); the dead
`JavaResolvedClassOrigin` / `JavaResolvedClassLikeSymbol` plumbing is deleted;
`JavaTypeConversion` returns to its pre-`java-direct` body.

**Per-file changes.**

| File | Changes |
|---|---|
| `core/compiler.common.jvm/src/.../load/java/structure/javaTypes.kt` | **Delete** `JavaClassifierType.resolve(tryResolve, getSupertypeClassIds): ClassId?` (lines 70–88, including its KDoc and default impl). The `classifier: JavaClassifier?` field (line 59) and `containingClassIds: List<ClassId>` field (line 116) are unchanged. New short KDoc on `classifier` records that under injection it is reliable for every reference (cross-file too). |
| `core/compiler.common.jvm/src/.../load/java/structure/javaElements.kt` | **Delete** `JavaAnnotation.resolveAnnotation(tryResolve): ClassId?` (lines 60–67, including its KDoc and default `classId` impl). The `classId: ClassId?` field (line 40) is unchanged. New short KDoc on `classId` records the post-injection reliability invariant. |
| `core/compiler.common.jvm/src/.../load/java/structure/annotationArguments.kt` | **Delete** `JavaEnumValueAnnotationArgument.resolveEnumClass(tryResolve): ClassId?` (and its KDoc), if present. The annotation argument exposes its resolved enum class through a `JavaClassifier?`-shaped accessor (already present, similarly materialised). |
| `compiler/java-direct/src/.../resolution/JavaResolutionContext.kt` | **Drop callbacks** from `resolve(name, ...)` and `findInheritedNestedClass(...)` (line 91). Add private helpers `tryResolve(classId): Boolean` and `directSupertypeClassIds(classId): List<ClassId>` backed by `firSession` (the latter wrapped in `loopChecker.guarded(...)` per §6). The context **owns** a `JavaSupertypeLoopChecker` instance and threads it through every supertype-walking entry point (BFS, cross-file `classifier` materialiser). KDoc on each helper records its laziness contract (§7 mitigation). The context holds a `LazySessionAccess` (not a raw `FirSession`) per §12 Q2; cache-population components never see the wrapper. `JavaResolutionContext.resolve(name)` becomes the model-internal helper consumed by `JavaClassifierTypeOverAst.computeClassifier()` (cross-file branch) and the annotation `classId` materialiser. |
| **New file** — `compiler/java-direct/src/.../resolution/JavaSupertypeLoopChecker.kt` | New per-`JavaResolutionContext` component owning a thread-local active-`ClassId` set and a recorded list of cycle edges. Provides `inline fun <R> guarded(classId: ClassId, default: R, block: () -> R): R`. Subsumes the existing per-call `visited<ClassId>` parameters in `JavaInheritedMemberResolver.resolveInheritedInnerClassToClassId`, `JavaInheritedMemberResolver.findInnerClassFromSupertypes` (`visited<JavaClass>` collapses to `ClassId` keying), and the per-call `visited` in `JavaSupertypeGraph.collectInheritedInnerClasses`. Diagnostic emission for recorded cycle edges follows §12 Q4 (recommended: direct `FirErrorTypeRef` synthesis mirroring `breakLoops`). KDoc records the cycle-bound contract (§6.1) and the per-thread / per-`JavaResolutionContext` scope invariant. |
| **New file** — `compiler/java-direct/src/.../resolution/LazySessionAccess.kt` | Value class around `FirSession` exposing only the symbol-provider surface needed by resolution-time code (`classLikeSymbol(classId)` and `lazyResolveSupertypes(symbol)`); cache-population code (anything reachable from `JavaClassCache` / `LeanJavaClassFinder.indexFile` / `JavaSupertypeGraph`-population) does not see this type and therefore cannot reach `firSession.symbolProvider`. Required typed wrapper per §12 Q2; sketch in §8 mode 1. KDoc cross-references invariants 1, 2, 3 of §7. |
| `compiler/java-direct/src/.../model/JavaTypeOverAst.kt` | **Delete** `JavaClassifierTypeOverAst.resolve(...)` (lines 305–310). **Delete** `JavaClassifierTypeForEnumEntry.resolve(...)` (lines 336–340) — `classifier = enumClass` (line 326) already provides the same `classId`. **Delete** the three remaining trivial `resolve(...)` overrides at lines 656, 672, 693. **Extend** `computeClassifier()` (lines 106–130) with a cross-file branch: when the existing same-file / inherited-inner walk returns `null`, consult `JavaResolutionContext.resolve(rawTypeName)` and wrap the resulting `ClassId` in a thin `JavaClassifier` (Step 4.5a may use a minimal `JavaClassifier` shape with only `classId` populated; Step 4.5b replaces it with the full `JavaClass`-shaped adapter). |
| `compiler/java-direct/src/.../model/JavaAnnotationOverAst.kt` | **Delete** `JavaAnnotationOverAst.resolveAnnotation(...)` (line 72) and `JavaEnumValueAnnotationArgumentOverAst.resolveEnumClass(...)` (line 285). **Make `classId` a lazy property** that consults `JavaResolutionContext.resolve(reference)` for cross-file annotation references (mirroring `computeClassifier()`'s materialiser). |
| `compiler/java-direct/src/.../resolution/JavaInheritedMemberResolver.kt` | **Drop callback parameters** from `resolveInheritedInnerClassToClassId(...)` (line 127). The two-phase BFS collapses into a single per-class-dispatched origin-agnostic loop using `JavaResolutionContext.directSupertypeClassIds(...)`. Phase 1's `LeanJavaClassFinder.collectInheritedInnerClasses` source-index walk reduces to a **fast path inside the loop** (when the dispatcher's source arm hits, no FIR query is needed). The Stage-2b deferral KDoc block is removed; replaced by a "post-Step-4.5a" KDoc explaining the new single-loop shape. |
| `compiler/fir/fir-jvm/src/.../JavaTypeConversion.kt` | **Delete** `resolveSymbolBasedClassId(session)` (lines 414–420) outright. **Restore** `resolveTypeName` (line 422) to its pre-`java-direct` body: `javaType.classifier?.classId ?: findClassIdByFqNameString(name, session) ?: ClassId.topLevel(FqName(name))`. **Delete** `getResolvedSupertypeClassIds(classId, session)` (lines 451–457) if no FIR-internal caller remains; otherwise keep it as a private helper for `findTypeArgsForClassInHierarchy` (line 517) and remove its `@VisibleForTesting`-equivalent / model-facing exposure. KDoc on the surviving `findTypeArgsForClassInHierarchy` records that the `lazyResolveToPhase(SUPER_TYPES)` it still calls is for outer-chain *substitution math*, not the BFS hot path. The annotation conversion paths similarly read `JavaAnnotation.classId` directly instead of calling `resolveAnnotation(...)`. |
| `compiler/fir/fir-jvm/src/.../declarations/FirJavaClass.kt` | **Add** `directSupertypeClassIds(): List<ClassId>` — a lazily-computed cache populated from `javaClass?.supertypes` at first read (using the same lexical scope `LeanJavaClassFinder` already wields: containing-class chain + imports + package; no FIR phases involved). **Variant C** of `UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md` §3. The `private val javaClass: JavaClass?` field at line 45 stays `private` per the user's resolution of §12 Q1; the new accessor is the *only* surface the model needs. KDoc on the new accessor documents the cycle-bound (`directSupertypeClassIds()` is referentially transparent and never re-enters FIR resolution), the **"AST supertypes are not enhanced"** hazard (the cache holds `ClassId`s only — no `@NotNull` / `@Nullable` / `Mutable*` decisions are encoded), and the population strategy. **Fallback (variant D)**: if the lazy cache proves heavier than expected, the implementation iteration may pivot to a one-line visibility flip on `javaClass` (line 45: `private val` → `internal val`); §12 Q1 records D as the explicit fallback. This is the **single FIR-core change** outside `compiler/fir/fir-jvm/.../JavaTypeConversion.kt`; reviewer audience is the FIR-core maintainer. |
| `compiler/java-direct/src/.../resolution/JavaResolvedClassLikeSymbol.kt` | **Deleted.** `JavaResolvedClassOrigin` enum + `JavaResolvedClassLikeSymbol` data class were added in Step 2's Stage 1, never consumed; subsumed by direct `firSession.symbolProvider` access. Removing them avoids carrying dead plumbing into the post-injection world. |
| `compiler/java-direct/test/.../JavaParsingMembersTest.kt` (line 160), `JavaParsingTypeResolutionTest.kt` (lines 147, 315) | The three hand-rolled `tryResolve = { ... }` call sites become **property reads**: `paramType.classifier?.classId` / `fieldType.classifier?.classId` / `returnType.classifier?.classId`. The three tests construct a Java Model whose `firSession` is stubbed (a minimal `FirSession` with a custom `symbolProvider` delegating to the test's class set) so that `computeClassifier()`'s cross-file branch can answer. The fixture work is non-trivial; one approach (recommended) is to extract a `JavaParsingTestFixture` helper that builds the stub session once and is shared by both test classes. Alternative shapes are listed in §12 (open design questions). |

**Validation gate.**

- Full `JavaUsingAst*` matrix unchanged: `JavaUsingAstPhasedTestGenerated`,
  `JavaUsingAstBoxTestGenerated`, expected 2693 / 2693 passing.
- Trip-wire pair green: `Tests.Generics.InnerClasses.testJ_k_complex` and
  `Tests.J_k.CollectionOverrides.testMapMethodsImplementedInJava`.
- **Cross-origin re-entry trip-wire green:** `KJKComplexHierarchyNestedLoop.kt`
  (in `compiler/fir/analysis-tests/testData/resolveWithStdlib/problems/`) —
  walks the `Java → Kotlin → Java` chain that motivates §6.1's checker. Must
  not deadlock or report a partial supertype list.
- **Java-only inheritance-cycle regression:** new diagnostics test cases
  cover (a) direct self-cycle (`class A extends A {}`) and (b) indirect cycle
  (`class A extends B {}; class B extends A {}`) in pure Java code under the
  `JavaUsingAst*` matrix. Both must produce `CYCLIC_INHERITANCE_HIERARCHY`
  (via `DiagnosticKind.LoopInSupertype` per §6.1 / §12 Q4) and **not deadlock**
  the lazy delegate. This is the regression gate for Java-only cycles that
  today silently truncate.
- `git diff` review per `AGENT_INSTRUCTIONS.md` rule 4: every changed file
  passes the "could a linter or compiler check this?" question; KDoc updates
  are noted in the iteration log.

**Perf-counter expectation.**

- **Parse counter.** Unchanged. The change does not parse anything new; the
  source-arm `classFinder.findClass(...)` calls go through the same
  `JavaClassCache.getOrPutIfNotNull` entry point used today.
- **Symbol-creation counter.** Same call distribution as today
  (`getClassLikeSymbolByClassId` per candidate `ClassId`); the body of the
  `tryResolve` lambda moves into the model but the call shape and frequency
  are identical.
- **Net delta.** Structurally a no-op for parse counts. **Net cheaper** for
  the inherited-inner BFS path because the Java arms avoid a
  `lazyResolveToPhase(SUPER_TYPES)` no-op + a `superTypeRefs` lazy-delegate
  enhancement read; this is not directly observable on the pre-existing
  parse-counter / symbol-creation-counter harness but should be visible on
  any read-side counter that distinguishes `superTypeRefs` accesses.

**Out-of-scope deferrals.**

- `JavaScopeResolver.findLocalClass` body retirement and
  `JavaClassOverAst.findInnerClassInSupertypes` retirement remain deferred to
  Step 4.5b. KDoc on `findLocalClass` is updated to reference Step 4.5b
  instead of the old "Stage 5 deferred" language.

### Step 4.5b — Stage-5 collapse (closes L2)

**Goal.** The AST-side `JavaScopeResolver.findLocalClass` body retires;
`JavaClassOverAst.findInnerClassInSupertypes` retires (or shrinks to a
same-file fast path). `JavaClassifierTypeOverAst.computeClassifier()`'s
**cross-file branch** (introduced in 4.5a as a minimal `JavaClassifier` with
only `classId` populated) is **upgraded** to wrap a full `JavaClass`-shaped
adapter derived from `firClass.javaClass` (for `FirJavaClass`) or a small
adapter on `FirRegularClass` (for Kotlin). The unification headline becomes
literally true.

**Per-file changes.**

| File | Changes |
|---|---|
| `compiler/java-direct/src/.../resolution/JavaScopeResolver.kt` | `findLocalClass(name): JavaClass?` body retires (becomes either deleted or a same-file fast path that reads only same-file declarations). The class shrinks to `findTypeParameter` + `findInheritedTypeParameter` + `getContainingClassIds` + the same-file fast path — exactly the post-Stage-5 shape `MERGED_REFACTORING_PLAN_2026_05_04.md` §3 bullet 4 promises. |
| `compiler/java-direct/src/.../model/JavaClassOverAst.kt` | `findInnerClassInSupertypes` retires entirely *or* shrinks to a same-file fast path for `JavaClassifierTypeOverAst.computeClassifier()`. Decision deferred to the implementation iteration based on parity-check results (see Validation gate). |
| `compiler/java-direct/src/.../model/JavaTypeOverAst.kt` | `computeClassifier()`'s cross-file branch (added in 4.5a) is **upgraded**: instead of returning a minimal `JavaClassifier` with only `classId` populated, it returns a full `JavaClass`-shaped adapter (see new file below) with outer-chain + `findInnerClass` answers driven by the FIR symbol provider. This unblocks `JavaTypeOverAst`'s multi-part navigation (`Outer.Inner` references, `j+k_complex.kt`-style hierarchies). The same-file fast path (already cheap) is preserved. |
| **New file** — `compiler/java-direct/src/.../resolution/FirBackedJavaClassAdapter.kt` (or sibling) | `JavaClass`-shaped adapter over `FirRegularClassSymbol`. **For `FirJavaClass`**: backed by the C-cache landed in 4.5a (`firClass.directSupertypeClassIds()` for supertype `ClassId`s; the underlying `JavaClass.supertypes` for full structural data is reached either via a parallel public accessor on the cache or, if 4.5a pivoted to fallback D, via `firClass.javaClass` directly). **For Kotlin / built-in / deserialized**: builds a structural view on demand using `firSession.symbolProvider` for outer-chain recovery (`classId.outerClassId` → `getClassLikeSymbolByClassId`). KDoc records the laziness contract (§7 invariants 1, 2, 3) and the cycle-bound (delegates to `JavaSupertypeLoopChecker` via the dispatcher). |
| `compiler/java-direct/src/.../resolution/JavaResolutionContext.kt` | If a `directSupertypeClassIds` consumer needs the full structural data (not just `ClassId`s) for L2's adapter, expose a parallel `directSupertypes(classId): List<JavaClassifierType>` helper that shares the dispatcher with `directSupertypeClassIds`. Backed by the same data sources (§6). |

**Validation gate.**

- Full `JavaUsingAst*` matrix unchanged: 2693 / 2693 passing.
- Trip-wire pair green: same as Step 4.5a.
- **Same-file fast-path parity check.** A new ad-hoc test (or an extension to
  `JavaUsingAstPhasedTestGenerated`) verifies that for same-file inherited
  inners the model still hits the local-file fast path (no
  `getClassLikeSymbolByClassId` call). This guards against regressing
  invariant 5 ("no eager parsing") and against accidentally widening the
  package index.

**Perf-counter expectation.**

- **Parse counter.** Unchanged — adapter does not parse new files;
  `getClassLikeSymbolByClassId` does not parse for cross-origin classes.
- **Symbol-creation counter.** May tick up by *one extra*
  `getClassLikeSymbolByClassId` call per outer-chain level for cross-file
  inherited inners; outer chains are typically 1–2 deep and the call is
  cached on the symbol provider side. Bounded; should be measurable as a
  small constant offset on the `testIntellij_platform_externalProcessAuthHelper`
  testbed used in the merged plan's perf gate
  ([`AGENT_INSTRUCTIONS.md` rule 3](../AGENT_INSTRUCTIONS.md)).
- **Net delta.** Within noise; the adapter replaces an AST-side syntactic walk
  with a symbol-provider-driven walk that is bounded by outer-chain depth.

**Out-of-scope deferrals.** None — Step 4.5b closes L2 and the unification
headline.

### Combined-step alternative (not recommended)

Combining Steps 4.5a and 4.5b into a single iteration is technically possible:
the dispatcher (§6) is a strict subset of L2's adapter, so writing the adapter
*first* and using it for L1 is internally consistent. The reason to keep them
separate is the merged plan's "each gate run isolates a single redesign's
effect" invariant: combining them maximises blast radius across two distinct
risk surfaces (FIR-core `directSupertypeClassIds()` cache + AST-side BFS
collapse + new `JavaSupertypeLoopChecker` + typed `LazySessionAccess` wrapper
on one hand, adapter design + AST-side classifier consumer rewrites on the
other). If the
implementation iteration encounters unexpected friction in 4.5a (e.g., the
test-fixture migration takes longer than expected), 4.5b can still proceed
independently against the post-4.5a baseline.

---

## 12. Open design questions (all resolved)

All four design questions are now resolved per the user's answers in the
issue thread; nothing in this section blocks the implementation iterations
from starting (modulo the `FirSession`-threading prerequisite, which is
itself out of scope for this doc — see §1).

### Q1 — `FirJavaClass.javaClass` exposure: variant C (cache) vs. variant D (visibility flip)

**Resolved: variant C.** A `directSupertypeClassIds(): List<ClassId>` cache is
added to `FirJavaClass` (Step 4.5a per-file table, §11). The cache is populated
lazily from `javaClass?.supertypes` at first read using the same lexical scope
`LeanJavaClassFinder` wields (containing-class chain + imports + package; no
FIR phases involved). The `private val javaClass: JavaClass?` field at line
45 stays `private` — the new accessor is the only surface the model needs.
Documented in
[`UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md` §3 "C — pre-resolved supertype-`ClassId` cache"](UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md#3-the-five-phase-light-alternatives).

**Rationale.** The user's preference is to keep `javaClass` encapsulated; a
dedicated cache exposes only the data the model needs (supertype `ClassId`s
for the BFS) and avoids exposing `JavaClass`'s full structural surface. This
is a strict subset of D's surface area; the FIR-core reviewer is reviewing a
new accessor on `FirJavaClass`, not a visibility relaxation.

**Fallback: variant D (visibility flip).** Retained as fallback if C ends up
being significantly heavier (e.g., the cache materialisation triggers
undesired allocations on the perf testbed) or causes perf issues. The pivot
is a localised change: replace the cache reads in §6's dispatcher with
`firClass.javaClass.supertypes` reads and flip `private val javaClass` →
`internal val javaClass` at line 45. The rest of the proposal is unaffected.

**Decision rule on follow-ups.** The Step 4.5b adapter
(`FirBackedJavaClassAdapter`) needs full structural `JavaClass` data for the
`FirJavaClass` arm, not just `ClassId`s. Under C this is provided either by
a parallel `directSupertypes(): List<JavaClassifierType>`-style accessor on
`FirJavaClass` (recommended; same population strategy as the cache) or by
falling back to D for the L2 adapter case while keeping C for L1's BFS. The
4.5a iteration picks the cleaner of the two and the 4.5b iteration consumes
the choice; both options are open.

### Q2 — Typed `LazySessionAccess` wrapper vs. policy-only mitigation for failure mode 1

**Resolved: typed wrapper.** `LazySessionAccess` is now a hard requirement of
Step 4.5a (§11 per-file table), not an optional follow-up. The wrapper is a
value class around `FirSession` constructed only at resolution-context
boundaries; the symbol provider is reachable only through methods on the
wrapper. Code that does not see the wrapper cannot reach the symbol provider.
The full mitigation has all three tiers (KDoc + agent-instructions rule +
typed wrapper) per §7's risk-delta table and §8 mode 1.

**Rationale.** The user's preference is the maintenance-friendly choice:
failure mode 1 is detectable at compile time rather than at code review.
Future contributors editing cache-population code (anything reachable from
`JavaClassCache` / `LeanJavaClassFinder.indexFile` / `JavaSupertypeGraph`-population)
cannot accidentally introduce a `firSession.symbolProvider` call because
they never see the wrapper in scope.

**Implementation note.** The wrapper lands as part of Step 4.5a alongside
`JavaSupertypeLoopChecker` (§11 per-file table). `JavaResolutionContext` and
any sibling resolution-time component holds a `LazySessionAccess` instead of
a raw `FirSession`. The cache-population code path takes only AST-shaped
inputs.

### Q3 — Test-fixture shape for stubbed `FirSession`

**Resolved: shared helper with minimal session impl.** The three call sites
(`JavaParsingMembersTest.kt:160`, `JavaParsingTypeResolutionTest.kt:147,315`)
become property reads:

```kotlin
val resolved: ClassId? = paramType.classifier?.classId
// Under Shape 1 the public `resolve(...)` method is gone; `classifier` is
// materialised by the model using the fixture's stubbed FirSession.
```

A shared `JavaParsingTestFixture` helper (or equivalent) builds a minimal
`FirSession` with a custom `FirSymbolProvider` whose `getClassLikeSymbolByClassId`
consults a fixed class set provided by the test. The minimal session is the
leanest impl that satisfies the model's needs (`symbolProvider`, the laziness
wrapper's plumbing, no other FIR machinery).

**Rationale.** The user prefers the compromise: a shared helper avoids
duplicating the fixture across both test classes (which would scale badly as
more parsing-level tests arrive) while keeping the FIR surface to the bare
minimum (no `FirSessionConfigurator`, no transitive `compiler/fir/raw-fir`
dependencies). The helper is the long-term-cleanest choice; if a test needs
more than the minimal surface it can extend the helper rather than re-inventing
one.

**Implementation note.** The helper lives under
`compiler/java-direct/test/.../fixtures/` (or wherever `JavaParsingMembersTest`
and `JavaParsingTypeResolutionTest` find their other shared infrastructure).
The symbol-provider stub is the only piece that needs per-test customisation;
the rest of the session is constructed once.

### Q4 — Cycle-diagnostic emission mechanism

**Resolved: direct `FirErrorTypeRef` synthesis** mirroring `breakLoops`'s
mechanism
([`FirSupertypesResolution.kt:825,844`](../../fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirSupertypesResolution.kt)).
When `loopChecker.guarded(classId, ...)` detects re-entry, the recorded edge
`(parentClassId, supertypeClassId)` is consumed by the BFS finalisation; for
each edge, a `FirErrorTypeRef` carrying `ConeSimpleDiagnostic(LoopInSupertype)`
is substituted into `FirJavaClass.computeSuperTypeRefsByJavaClass`'s output
(or into the `directSupertypeClassIds()` cache landed under §12 Q1 / variant C,
depending on which location owns the supertype list at diagnostic-emission
time). The existing `coneDiagnosticToFirDiagnostic.kt:1048` mapping turns
that into `CYCLIC_INHERITANCE_HIERARCHY`.

**Rationale.** The user's preference is to mirror the Kotlin path exactly:
no new diagnostic class, same downstream presentation, same error-recovery
shape (the loop edge is replaced by an error type, downstream consumers
already handle `FirErrorTypeRef` gracefully). The cleaner-separation
alternative (a separate FIR-side checker consuming the edges after the BFS
returns, more literally mirroring K1's `SupertypeLoopChecker`) trades the
small wiring win for more code and a decoupled emission site that requires
its own test-fixture coverage.

**Implementation note.** The wiring point — exposing the checker's recorded
edges to `FirJavaClass.computeSuperTypeRefsByJavaClass` (or to the variant-C
`directSupertypeClassIds()` cache, whichever owns the supertype list at
emission time) — is a small new plumbing surface between
`JavaResolutionContext` and `FirJavaFacade`. The natural shape is a
`FirSession`-scoped component (`JavaSupertypeCycleEdges`-style holder)
populated by the checker and read by the cache-population callback in
`FirJavaFacade.createFirJavaClass`. Concrete shape is decided in the Step
4.5a iteration; the contract (recorded edges → `LoopInSupertype` →
`CYCLIC_INHERITANCE_HIERARCHY`) is fixed.

**Fallback (not chosen).** A separate FIR-side checker that consumes the
recorded edges after the BFS returns is documented for completeness; it can
be revisited only if the wiring above turns out to be infeasible (e.g., if
the C-cache cannot be reached from a diagnostic-emission point without
breaking the laziness invariants in §7). The fallback's pros (cleaner
separation of concerns) and cons (decoupled emission timing, more code) are
preserved here as a forward reference for the implementation iteration.

---

## 13. Merged-plan amendment (applied 2026-05-06)

The amendment described in earlier revisions of this doc as "sketched, not
applied" has now been **applied to
[`MERGED_REFACTORING_PLAN_2026_05_04.md`](MERGED_REFACTORING_PLAN_2026_05_04.md)
and
[`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md)**
in the same docs-only iteration that resolves §12 Q4 and refreshes this
section. The merged-plan §5 step list is now the authoritative execution
order (including Steps 4.5a and 4.5b); this section is preserved as a
high-level recap so the proposal stands on its own.

### What landed in the merged plan

- **§1 status snapshot.** The reference to this doc was reworded from "sketches
  Step 4.5a / 4.5b inserts — not yet applied" to "Step 4.5a / 4.5b inserts and
  the Step 4 re-classification are now applied to §5".
- **§3 expected-results bullets.** Bullet 3 ("classifier resolution goes
  through one origin-agnostic FIR path") is now annotated as becoming
  literally true post-Step-4.5b. Bullet 4 ("five laziness invariants
  upheld; three failure modes guarded") is annotated with the post-Step-4.5a
  enforcement shift (failure mode 1 from *structural* to *policy + typed
  `LazySessionAccess` wrapper*) and with the new `JavaSupertypeLoopChecker`'s
  cycle-bound and `CYCLIC_INHERITANCE_HIERARCHY` emission.
- **§4 source-document table.** The row for this doc is updated to enumerate
  the proposal's actual deliverables (deletion of `resolve(...)`, per-origin
  routing, `JavaSupertypeLoopChecker`, typed `LazySessionAccess`, the
  variant-C cache) and is marked **applied 2026-05-06**.
- **§5 execution order.**
  - **Step 4** is re-classified as *"Unification Stages 4 + partial 5 (KDoc only)"*
    so the per-step accounting reflects what actually landed (per the
    `2026-05-05 (Step 4)` entry in `ITERATION_RESULTS.md`).
  - **Step 4.5a — `FirSession` injection + `resolve(...)` deletion + drop
    Phase 1 (closes L1)** is inserted with full prerequisites / validation gate
    /  references, between the existing Step 5 and Step 6.
  - **Step 4.5b — Stage-5 collapse (closes L2)** is inserted after Step 4.5a.
  - **Step 6 prerequisites** are updated to include "Steps 4.5a + 4.5b green";
    the rationale ("indirect-caller audit becomes a propagation, not an
    invention, because the model's classifier path now goes through
    `firSession.symbolProvider`") is recorded inline.

### What landed in the unification-and-laziness doc

- **§"Three failure modes" mode 1.** A new "Mitigation enforcement update —
  post-Step-4.5a" sub-paragraph documents the three-tiered post-injection
  mitigation: KDoc, agent-instructions rule, and the **required** typed
  `LazySessionAccess` wrapper. Pre-injection structural prevention is
  described and contrasted with the post-injection policy-plus-wrapper
  enforcement.

### What is still sketched, not applied (and why)

- **Source code changes** described in §§3, 5, 6, 6.1, 11. These are the
  Step 4.5a and Step 4.5b implementation iterations themselves; they are
  *deliberately* not landed in this docs-only iteration. The merged plan
  now schedules them as concrete steps; this proposal owns their
  per-file contracts.
- **The `AGENT_INSTRUCTIONS.md` laziness-rule bullet** described in §7
  mitigation tier 2 lands alongside Step 4.5a (per the §14 cross-reference
  to that file). It is a small additive bullet, not a rewrite, and is
  therefore better landed adjacent to the source change it supports.

---

## 14. Cross-references

- [`MERGED_REFACTORING_PLAN_2026_05_04.md`](MERGED_REFACTORING_PLAN_2026_05_04.md) —
  the *when / in what order*; **amended 2026-05-06** to land Step 4.5a + Step 4.5b
  inserts and re-classify the existing Step 4 entry as *"Stages 4 + partial 5
  (KDoc only)"*. See §13 above for the per-section recap.
- [`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md) —
  the *what / why* of unification; owns the five laziness invariants and three
  failure modes that this proposal re-audits in §§7–8. **Amended 2026-05-06**
  to record the post-Step-4.5a enforcement shift on failure mode 1 (structural
  → policy + typed `LazySessionAccess` wrapper).
- [`UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md`](UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md) —
  **superseded** by this proposal as the recommended track. Alternative B
  (its previous recommendation's L2 closer) is subsumed by Step 4.5b's
  `FirBackedJavaClassAdapter`; **alternative C** (pre-resolved supertype-`ClassId`
  cache on `FirJavaClass`) is **adopted** as the L1 closer (per §12 Q1) and
  is the source for the new `directSupertypeClassIds()` accessor introduced
  in Step 4.5a; alternative D (the visibility flip on `FirJavaClass.javaClass`)
  is retained as the §12 Q1 fallback; alternatives A and F remain documented
  as fallback if `FirSession` injection is rejected; alternative E is
  documented for completeness.
- [`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md) —
  the *what / why* of PSI removal; owns the indirect-caller catalogue (§1.5)
  that Step 6 re-routes through `session.symbolProvider`. This proposal
  *strengthens* Step 6 (§9 above).
- [`CLASSIFIER_RESOLUTION_TRACE_2026_05_04.md`](CLASSIFIER_RESOLUTION_TRACE_2026_05_04.md) —
  end-to-end classifier-resolution trace; useful for predicting which call
  sites Steps 4.5a / 4.5b affect.
- [`ITERATION_RESULTS.md`](../ITERATION_RESULTS.md) — per-iteration log; the
  Step-3 entry's "Stage 2b post-mortem" sub-section records the trip-wire
  reproductions that motivated this design space.
- [`AGENT_INSTRUCTIONS.md`](../AGENT_INSTRUCTIONS.md) — non-negotiable rules
  for working in the module (no command chaining, performance-measurement
  harness, log-piping conventions); applies to any 4.5a / 4.5b implementation
  iteration. The §7 mitigation tier 2 ("agent-instructions rule") would land
  alongside Step 4.5a as a small additive bullet in this file's laziness
  section.

---

## 15. Implementation prerequisites and remaining gaps

This section answers the question *"is anything still blocking the
implementation?"* explicitly. As of 2026-05-06 — after Q4 is resolved and
the merged-plan amendment is applied — the proposal is **ready for the
Step 4.5a iteration to start**, modulo one **architectural** prerequisite
and a small set of **iteration-internal** decisions that the implementer
finalises during the first iteration.

### The single outstanding architectural prerequisite

- **`FirSession` is reachable inside the Java Model.** This is the only
  prerequisite that has to land in a separate iteration *before* Step 4.5a.
  Per the user's instruction (and §1's scope rules), the wiring iteration
  is a distinct piece of work — likely late-init on `JavaClassFinderOverAst`
  first, followed by a restructured entry point. Step 4.5a's per-file
  contract assumes this is in place; it does not specify the wiring shape
  beyond "the model can call `firSession.symbolProvider.getClassLikeSymbolByClassId(...)`".

  Until this lands, Step 4.5a cannot meaningfully execute (the
  callback-deletion changes would have nowhere to source resolution data
  from). Any iteration that tries to land Step 4.5a without this wiring in
  place should be rejected at planning, not at code review.

### Decisions the Step 4.5a iteration finalises (none of which block start)

The following are *contracts already fixed* in this proposal; the
implementer makes the concrete shape choice during the iteration. None of
them is a design decision in disguise — they are mechanical realisations
of decided contracts.

- **Loop-checker diagnostic-edge wiring shape (§6.1, §12 Q4).** The
  contract is fixed: recorded edges → `LoopInSupertype` → `CYCLIC_INHERITANCE_HIERARCHY`
  via direct `FirErrorTypeRef` synthesis mirroring `breakLoops`. The
  recommended concrete plumbing is a `FirSession`-scoped
  `JavaSupertypeCycleEdges`-style component populated by `JavaSupertypeLoopChecker`
  and read by `FirJavaClass.computeSuperTypeRefsByJavaClass` (or the
  variant-C `directSupertypeClassIds()` cache). The iteration picks the
  exact owner of the supertype list at emission time; the user-visible
  diagnostic is the same either way.
- **Test-fixture helper shape (§12 Q3).** The contract is fixed: a shared
  helper providing a minimal `FirSession` with a stubbed
  `FirSymbolProvider`, reused by `JavaParsingMembersTest.kt` and
  `JavaParsingTypeResolutionTest.kt`. The iteration picks the file path
  (under `compiler/java-direct/test/.../fixtures/` or wherever the existing
  shared infrastructure lives) and the helper's API surface (a
  `JavaParsingTestFixture.buildSession(classes: Map<ClassId, FirRegularClassSymbol>)`-style
  factory is the canonical shape). No design ambiguity.
- **`FirJavaClass.directSupertypeClassIds()` cache populator (§12 Q1,
  Step 4.5a per-file table).** The contract is fixed: variant **C** (a
  lazy cache populated from `javaClass?.supertypes` using the same lexical
  scope as `LeanJavaClassFinder`'s name resolver). The iteration confirms
  the population strategy does not regress allocations on the perf testbed;
  if it does, the documented fallback is variant **D** (one-line
  visibility flip on `FirJavaClass.javaClass`). Both options are
  pre-approved; no further design discussion required to pivot.
- **`LazySessionAccess` wrapper API surface (§7, §12 Q2).** The contract
  is fixed: a typed wrapper around `FirSession` that exposes only the
  symbol-provider methods used by resolution-time code; cache-population
  components hold AST-shaped inputs only. The iteration picks the exact
  method names (likely `classLikeSymbol(classId)` and
  `lazyResolveSupertypes(symbol)` per the §11 sketch); the contract
  ("never reach `firSession.symbolProvider` from cache-population code")
  is non-negotiable.
- **Same-file fast-path retention in Step 4.5b (§6 step 1, §11 Step 4.5b).**
  The contract is fixed: the same-file fast path stays cheap and
  AST-only. The iteration decides whether
  `JavaClassOverAst.findInnerClassInSupertypes` retires entirely or
  shrinks to a thin same-file fast path; a parity check against the
  pre-iteration baseline is the deciding signal.

### Non-blocking concerns (separate from the implementation iterations)

- **Perf-counter harness reachability.** The
  `phase-c-instrumentation-v5-v6-measurements` stash referenced in earlier
  iterations is incompatible with the current source tree (per the Step-5
  iteration log entry). Re-running the parse-counter harness on the
  post-Step-4.5a baseline is *retrospective*: the structural argument
  records that 4.5a does not parse anything new, so the parse counter
  cannot be affected. If the harness is re-cut on the post-Step-4.5a tree,
  the result will retroactively confirm the structural argument. This does
  **not** block the iteration; it is a follow-up that can land any time
  before Step 6.
- **Step 6 (PSI Phase 2) preparation.** Step 6's prerequisites now include
  Steps 4.5a + 4.5b green (per the merged-plan amendment). The
  indirect-caller audit (PSI doc §1.5) becomes a propagation, not an
  invention, post-Step-4.5b. This is a strengthening of Step 6, not a new
  prerequisite, and it does not feed back into the Step 4.5a / 4.5b
  iteration design.
- **`AGENT_INSTRUCTIONS.md` laziness-rule bullet (§7 mitigation tier 2).**
  Lands as a small additive bullet alongside Step 4.5a's source change
  per §13 "What is still sketched, not applied". Not a separate blocking
  iteration.

### Explicitly: nothing else blocks Step 4.5a

A reviewer of this doc looking for a "go / no-go" signal can use the
following checklist:

- ✅ All four design questions in §12 are resolved.
- ✅ The merged plan now schedules Step 4.5a and Step 4.5b explicitly with
  prerequisites, validation gates, and references.
- ✅ The unification-and-laziness doc records the post-injection
  enforcement shift on failure mode 1.
- ✅ The cycle-handling concern raised by the user (recursive supertype
  walks) is addressed by `JavaSupertypeLoopChecker` (§6.1) with a
  decided diagnostic-emission mechanism (§12 Q4).
- ✅ The shape of the test-fixture migration, the `directSupertypeClassIds()`
  cache, the typed `LazySessionAccess` wrapper, and the loop-checker's
  diagnostic-edge wiring are all decided contracts.
- ⚠ The single outstanding architectural prerequisite is **`FirSession`
  threading inside the Java Model**, which is intentionally out of scope
  for this proposal and is the work of a separate iteration that must
  land before Step 4.5a.
- ✅ Nothing else in this proposal, the merged plan, or the
  unification-and-laziness doc is a blocker.

