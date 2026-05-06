# Unification Closure — Phase-light Alternatives — 2026-05-05

> **Status / scope.** Companion to
> [`MERGED_REFACTORING_PLAN_2026_05_04.md`](MERGED_REFACTORING_PLAN_2026_05_04.md)
> and [`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md).
> This document contrasts the candidate approaches for closing the two unification
> leftovers that survived Steps 2–5 (Stage 2b — drop `JavaInheritedMemberResolver`'s
> Phase 1; full Stage 5 — retire `JavaScopeResolver.findLocalClass` and
> `JavaClassOverAst.findInnerClassInSupertypes`), with an explicit compatibility check
> against Step 6 (PSI Phase 2). It does **not** modify production source files and does
> **not** by itself amend the merged plan — it captures the design space so a follow-up
> amendment can pick a track.
>
> See also: [`AGENT_INSTRUCTIONS.md`](../AGENT_INSTRUCTIONS.md),
> [`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md),
> [`CLASSIFIER_RESOLUTION_TRACE_2026_05_04.md`](CLASSIFIER_RESOLUTION_TRACE_2026_05_04.md),
> [`ITERATION_RESULTS.md`](../ITERATION_RESULTS.md).

---

## 1. What is left to close

Steps 2–5 of the merged plan landed Unification Stages 1, 2a (partial), 3, 4 and a
verification-only sweep. Two pieces of the resolver-unification track did **not** land:

- **L1 — Stage 2b.** Drop Phase 1 of `JavaInheritedMemberResolver.resolveInheritedInnerClassToClassId` (today a two-phase BFS: Phase 1 walks the AST source index; Phase 2 walks the FIR `getResolvedSupertypeClassIds` callback).
  The unification doc's invariant is that supertype walks should be origin-agnostic — i.e., Phase 1 should disappear in favour of Phase 2 alone. It can't yet, because of the timing bug in §2.
- **L2 — Full Stage 5.** Retire `JavaScopeResolver.findLocalClass` (the AST-side fast path that still fans out over inner / sibling / inherited / outer-chain / same-file lookups) and `JavaClassOverAst.findInnerClassInSupertypes` (the AST-side substitution-aware inherited-inner walker used by `JavaTypeOverAst.computeClassifier`).
  The unification doc's headline outcome — "the AST-side resolver retains *only* type parameters + `containingClassIds` + the same-file fast path" ([`MERGED_REFACTORING_PLAN_2026_05_04.md` §3](MERGED_REFACTORING_PLAN_2026_05_04.md#3-expected-results) third bullet) — is not yet in effect. Step 4 collapsed `findLocalClass` out of the **`ClassId`-resolution path** but left it in place as a **structural-`JavaClass` fast path** for cross-file inherited inners (the `j+k_complex.kt` trip-wire). `getClassLikeSymbol` on its own does not yield a `JavaClass` with a full outer-chain.

L1 and L2 are entangled — both need a way to read inherited-supertype data
*without* depending on the FIR phase clock being honest in compiler mode (see §2). But
they also fail in different ways: L1 is a *timing* bug on a `ClassId`-only read, L2 is a
*structural* gap on a `JavaClass`-shaped read. None of the alternatives below close
*both* on its own; the pairings in §4 spell out which combinations do.

---

## 2. The timing bug, cleanly stated

Today `JavaInheritedMemberResolver`'s Phase 2 calls
`JavaTypeConversion.getResolvedSupertypeClassIds(classId, session)`
([`JavaTypeConversion.kt`](../../fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt)
line 451). That callback does:

1. resolve `classId` to a `FirRegularClassSymbol` via `session.symbolProvider`,
2. call `classSymbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)`,
3. read `classSymbol.fir.superTypeRefs` and extract a `ClassId` from each.

In **LL-FIR**, `lazyResolveToPhase(SUPER_TYPES)` is honest: if the symbol is below
`SUPER_TYPES` and not on the active resolution stack, the supertype transformer is
invoked synchronously and `superTypeRefs` is fully populated by the time the call
returns; if the symbol *is* on the active stack (cycle case), the call no-ops and the
read returns whatever has accumulated so far — that's the cycle bound, and it's
correct. **In compiler mode**, `FirLazyDeclarationResolver` is a no-op stub: classes
are assumed to already be at their final phase by the time anyone queries them.
`lazyResolveToPhase(SUPER_TYPES)` therefore no-ops *unconditionally*, regardless of the
symbol's actual phase. That is harmless when the symbol is at or above `SUPER_TYPES`
(eager driver finished it before the BFS got there) and harmless on the active stack
(cycle bound, same as LL-FIR) — but it is **wrong** when the symbol is below
`SUPER_TYPES` and **off** the active stack:

- `superTypeRefs` is observable (it's a `lazy {}` delegate on `FirJavaClass`, see
  [`FirJavaClass.kt:115`](../../fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/declarations/FirJavaClass.kt)),
  so the read returns a value — but that value may be **partial** (the lazy delegate
  fired against not-yet-final `nonEnhancedSuperTypes`, or it hasn't fired at all and we
  see an empty list).

The two trip-wires `Tests.J_k.CollectionOverrides.testMapMethodsImplementedInJava` and
`Tests.Generics.InnerClasses.testJ_k_complex` regress on exactly this case: while
resolving `Derived → Base → Map<…>` the BFS asks `getResolvedSupertypeClassIds(Base)`,
`Base.superTypeRefs` is empty *at that moment* in compiler mode, the BFS sees no
`Map`, and `Map.Entry` is reported unresolved. (See
[`ITERATION_RESULTS.md`](../ITERATION_RESULTS.md) "Merged plan Step 3" entry,
"Stage 2b post-mortem" sub-section.)

> **In one sentence.** `lazyResolveToPhase(SUPER_TYPES)` is a no-op in compiler mode
> regardless of the symbol's actual phase, so the subsequent read of
> `classSymbol.fir.superTypeRefs` may observe a partially-populated lazy list whenever
> the symbol is below `SUPER_TYPES` and off the active resolution stack.

This is not a cycle bug (the cycle bound is sound) and it is not a phase-ordering bug
inside the eager driver (the eager driver is, by design, allowed to schedule classes
in any order so long as the FIR public API is queried only post-completion). It is a
*read-side* bug: `JavaInheritedMemberResolver` reads a phase-gated value through an
adapter that does not actually gate it in compiler mode.

The five alternatives below are five different ways of stopping the resolver from
reading a phase-gated value at all.

---

## 3. The five phase-light alternatives

The detailed derivation of each alternative — what it is, what new code it requires,
its cost / risk profile, and what it does *not* fix on its own — lives in the
preceding planning round; this section names them and records the one-line essence so
this doc and the comparison tables in §§4–5 read self-contained.

| Tag | Essence | Touches FIR core? | Closes |
|---|---|---|---|
| **A** | Phase-aware adapter that *forces* `SUPER_TYPES` from the outermost lazy entry, including in compiler mode (via re-entry detection or a thread-local active-supertype set inside FIR). | **Yes — semantics** | L1 |
| **B** | Origin-agnostic AST-side core: a `JavaClass`-shaped adapter over `FirRegularClassSymbol` that materialises `outerClass` / `findInnerClass` / `typeParameters` from FIR; `findLocalClass` and `findInnerClassInSupertypes` retire. | No | L2 (depends on something closing L1 for cross-origin inherited-inner reads to be correct) |
| **C** | Pre-resolved supertype-`ClassId` cache on `FirJavaClass` (or a `FirSessionComponent` keyed by `ClassId`), computed once from `javaClass.supertypes` using the same lexical scope `LeanJavaClassFinder` already wields. The BFS reads the cache; `superTypeRefs` is no longer touched for that query. | No (one new field / component, no phase logic) | L1 |
| **D** | Un-`private` `FirJavaClass.javaClass` (visibility-only change) and let `JavaTypeConversion.getResolvedSupertypeClassIds` branch on `is FirJavaClass` to read `javaClass.supertypes` directly. The smallest possible diff. | **Yes — visibility only** (one modifier flip) | L1 |
| **E** | Origin-agnostic AST-side BFS: widen `LeanJavaClassFinder.collectInheritedInnerClasses` to walk both source and binary `JavaClass` instances; Phase 2 (the FIR-callback walk) retires; Phase 1 becomes the canonical origin-agnostic walk. | No | L1 (and structurally preps L2 — but does not close it on its own) |
| **F** | Tighten the FIR-core invariant that `lazyResolveToPhase(SUPER_TYPES)` is eager-correct in compiler mode (one-line precondition / driver fix). Requires confirmation from FIR maintainers; the `java-direct` team cannot land it unilaterally. | **Yes — semantics** | L1 |

A is included for completeness (and contrast with the phase-light alternatives), since
the merged plan's original Stage 3 closing paragraph foresaw a maintainer-confirmation
piece around it. The user's design preference, expressed in the preceding round, is to
avoid touching FIR resolution-phase semantics — i.e., avoid A and F. The remaining
candidates are B (for L2) plus C / D / E (for L1).

---

## 4. Compatibility with Step 6 (PSI Phase 2)

Step 6 of the merged plan ([§5 Step 6](MERGED_REFACTORING_PLAN_2026_05_04.md#5-merged-execution-order)
and [PSI doc §2.4](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md#2.4) ff.) delivers the
following invariants that any L1 / L2 closer has to live with:

- **`JavaSymbolProvider` becomes source-only.** Binary Java lookups move to
  `JvmClassFileBasedSymbolProvider`, which is a *symbol provider* (returns `FirRegularClassSymbol`s),
  not a `JavaClassFinder` (does not return `JavaClass` instances).
- **`CombinedJavaClassFinder` and `BinaryJavaClassFinder` are deleted.** The only
  `JavaClassFinder` left is the source-side one (AST-backed by default,
  PSI-backed during the 1–2-release transition behind a flag).
- **`FirJavaFacade.classFinder` becomes the source-only finder.** Callers that today
  rely on `combinedJavaClassFinder` to fall through into binary classes lose that
  fall-through.
- **`session.symbolProvider` becomes the canonical origin-agnostic classifier entry.**
  The unification doc and the PSI doc both target this convergence; Step 6
  *operationalises* it.

The compatibility verdict for each alternative falls out of these four invariants:

### B — origin-agnostic AST-side core via `FirRegularClassSymbol`-backed adapter

**Step 6 compatibility: ✅ strengthened.**

B's adapter materialises `JavaClass`-shaped data from a `FirRegularClassSymbol`, with
the outer-chain recovered by walking `classId.outerClassId` through
`session.symbolProvider.getClassLikeSymbolByClassId`. That is precisely the API
[Step 6](MERGED_REFACTORING_PLAN_2026_05_04.md#5-merged-execution-order) makes
canonical. B does not depend on any flavour of `JavaClassFinder` — it goes through the
symbol provider — so the deletion of `CombinedJavaClassFinder` /
`BinaryJavaClassFinder` does not affect B at all. The adapter works uniformly across
source (`JavaClassOverAst`-backed `FirJavaClass`) and binary
(`BinaryJavaClass`-backed `FirJavaClass`) instances, both before and after Step 6.

If B reads supertypes through `firClass.javaClass.supertypes` (D's path) rather than
`firClass.superTypeRefs`, the adapter is **also** L1-correct in compiler mode. That
composition is the recommended track; see §5.

### C — pre-resolved supertype-`ClassId` cache on `FirJavaClass`

**Step 6 compatibility: ✅ unaffected.**

C's cache lives on `FirJavaClass` (or a `FirSessionComponent` keyed by `ClassId`) and
is populated from `javaClass.supertypes` at construction time. Its inputs are:

- `javaClass: JavaClass` — survives Step 6 unchanged. After Step 6, source classes are
  still backed by `JavaClassOverAst`, binary classes are still backed by
  `BinaryJavaClass` — only the *route from a `ClassId` to a `JavaClass` outside of
  FIR* changes (it's no longer a `JavaClassFinder`; it's the symbol provider's route
  to `FirJavaClass`, which already retains the `JavaClass`).
- the lexical scope (containing-class chain + imports + package) — purely intra-class
  data, not affected by Step 6.
- the name resolver (today inside `LeanJavaClassFinder`) — for source classes,
  unchanged; for binary classes, names in `BinaryJavaClass.supertypes` are already
  FQN-shaped, so the resolver is trivial.

The cache never asks any `JavaClassFinder` to traverse supertypes; it only resolves
the *names* of *direct* supertypes. Step 6 does not affect that operation.

### D — expose `FirJavaClass.javaClass` and read `javaClass.supertypes` from `JavaTypeConversion`

**Step 6 compatibility: ✅ unaffected.**

D is the smallest-blast-radius variant of C: instead of a cache, expose the
already-existing `private val javaClass: JavaClass?` (single visibility flip in
`compiler/fir/fir-jvm/.../declarations/FirJavaClass.kt:45`) and let the call site read
`firClass.javaClass.supertypes` directly. Same reasoning as C: the data sits on the
FIR symbol regardless of origin; the symbol-provider route to `FirJavaClass` is the
canonical post-Step-6 entry; no `JavaClassFinder` is involved.

The name resolver (`JavaClassifierType → ClassId`) does need to live somewhere
post-Step-6. Today it's inside `LeanJavaClassFinder`; if the source-only finder
absorbs `LeanJavaClassFinder`'s helpers (likely under Step 7), the resolver moves with
them — but the **call** in `JavaTypeConversion.kt` is just "give me a resolver" and
that's straightforward to thread.

### E — origin-agnostic AST-side BFS in `LeanJavaClassFinder.collectInheritedInnerClasses`

**Step 6 compatibility: ❌ structurally incompatible.**

This is the case the user's intuition was right about. Today
`LeanJavaClassFinder.collectInheritedInnerClasses` is implemented in
`JavaSupertypeGraph.collectInheritedInnerClasses`
([`JavaSupertypeGraph.kt:98`](../src/org/jetbrains/kotlin/java/direct/util/JavaSupertypeGraph.kt))
and is wired through four strictly **source-only** primitives:

- `classCacheLookup: (ClassId) -> JavaClass?` — backed by `JavaClassFinderOverAstImpl`'s
  source-class cache.
- `filesForClassLookup: (ClassId) -> List<VirtualFile>` — source-file index only.
- `sameClassInSameFilePackage: (FqName, String) -> Boolean` — source-package index only.
- `sourceFileReader: JavaSourceFileReader` — source-file slow-path reader only.

To make the BFS origin-agnostic in the sense E proposes, those primitives need a
binary-side counterpart: ideally the BFS asks `JavaClassFinder.findClass(classId)` and
the finder returns either a source `JavaClassOverAst` or a binary `BinaryJavaClass`,
walking on. **That is exactly what `CombinedJavaClassFinder` /
`BinaryJavaClassFinder` provide today, and exactly what Step 6 deletes.**

After Step 6, the only route from a `ClassId` to a `BinaryJavaClass` is:
`session.symbolProvider.getClassLikeSymbolByClassId(classId) → FirRegularClassSymbol →
(if FirJavaClass) firClass.javaClass`. That route already passes through the FIR
symbol provider — i.e., E would have to *reach across* into FIR, get a symbol, project
it back to a `JavaClass`, and continue the BFS. At which point E has implemented D's
mechanism in its inner loop and is no longer "an AST-side BFS" in any meaningful
sense; it has become "a `FirRegularClassSymbol`-driven BFS" with a `JavaClass` view
borrowed from the FIR side.

Two ways to reconcile, neither attractive:

1. **Keep a binary-side `JavaClassFinder` alive for the BFS only.** Contradicts
   Step 6's deletion goal; reintroduces `BinaryJavaClassFinder` under a different
   name; defeats the IntelliJ-platform-dependency-removal motivation (PSI doc §2.5).
2. **Re-build E on top of the symbol provider.** This is no longer E; it converges
   onto D + a thin walker, which is the recommended track in §5.

The merged plan does *not* allocate a step for "preserve a binary `JavaClassFinder`",
and the PSI doc explicitly enumerates the indirect callers that must lose the binary
fall-through ([§1.5](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md#1.5)). E's contract is
in direct tension with both. **E should not be selected; if its structural prep for
L2 is desired, that prep is a strict subset of B.**

### F — FIR-side invariant tightening

**Step 6 compatibility: ✅ unaffected.**

F is purely a FIR-core change: it does not touch any `JavaClassFinder` and does not
affect the symbol-provider topology. Whether it lands or not is orthogonal to Step 6.
Note that F closes L1 only; it leaves L2 entirely open, and L2's closer (B) does not
benefit from F (B does not depend on `lazyResolveToPhase` honesty — it doesn't read
`superTypeRefs` for the BFS path at all).

### A — phase-aware adapter

**Step 6 compatibility: ✅ unaffected.**

Same as F: A is a FIR-core change that does not interact with the `JavaClassFinder`
deletion. Listed here only for completeness; the user has explicitly preferred to
avoid A.

### Compatibility summary

| Approach | Closes | Touches FIR semantics? | Step-6-compatible? |
|---|---|---|---|
| A | L1 | yes | yes |
| **B** | **L2** | **no** | **yes — strengthened by Step 6** |
| **C** | **L1** | **no** | **yes** |
| **D** | **L1** | **no (visibility only)** | **yes** |
| **E** | L1 (and structural prep for L2) | no | **no — collides with `BinaryJavaClassFinder` deletion** |
| F | L1 | yes | yes |

Bold rows are the phase-light, Step-6-compatible candidates.

---

## 5. Recommended track and its rationale

**D + B — D first, then B — sequenced as Step 4.5a + Step 4.5b in the merged plan.**

This pairing is the only one that satisfies all four design preferences expressed in
the preceding round:

1. **No FIR resolution-phase semantics are touched** — D is a single visibility
   modifier flip in FIR core; B lives entirely in `java-direct`.
2. **Closes both L1 and L2.** D closes L1 (the BFS reads `javaClass.supertypes`
   directly, never `superTypeRefs`, so the timing bug §2 cannot manifest). B closes
   L2 (the AST-side `findLocalClass` and `findInnerClassInSupertypes` retire in
   favour of a `FirRegularClassSymbol`-backed `JavaClass` adapter).
3. **Step-6-compatible — strengthened by Step 6.** D's data path is
   `FirJavaClass.javaClass`; B's outer-chain walk is
   `session.symbolProvider.getClassLikeSymbolByClassId`. Both are the canonical
   post-Step-6 routes.
4. **One-direction blast radius per step.** Step 4.5a (D + drop Phase 1) touches FIR
   core in exactly one syntactic place (a visibility modifier on
   `FirJavaClass.javaClass`), with the rest in `java-direct`. Step 4.5b (B) lives
   entirely in `java-direct`. Each step has its own perf gate and trip-wire suite.

If D is rejected in FIR-core review (e.g., the maintainers prefer not to expose
`javaClass`), **C is the drop-in fallback**: a `directSupertypeClassIds` cache on
`FirJavaClass` (or a session component) gives B the same data path without the
visibility flip. The recommendation reverts to C + B with the same sequencing and the
same Step-6 compatibility profile.

**E should not be selected.** Beyond the Step-6 incompatibility analysed in §4, E
adds the most code of any phase-light alternative for the smallest gain — it closes L1
but does not close L2, and its structural prep for L2 (an origin-agnostic
`JavaClassFinder` arm) is *deleted* by Step 6 anyway. If a future cleanup wants the
structural shape E proposes, the natural home is *inside* a post-Step-7 source-only
`LeanJavaClassFinder` that has already lost its binary half — i.e., E becomes vacuous
at that point, since "origin-agnostic" reduces to "source-only".

**A and F are listed for completeness.** Either would close L1 with semantics-level
FIR changes; the user has preferred to avoid both. F in particular is the cheapest
*if* the FIR maintainers accept its premise — i.e., if they consider "compiler-mode
`FirJavaClass` instances are at `SUPER_TYPES` before being handed out by the symbol
provider" to be an enforceable invariant. That is a maintainer-side question, not a
`java-direct`-side decision.

### Suggested merged-plan amendment

If the recommendation is accepted, the cleanest way to reconcile the merged plan with
the actual unification track is:

- Insert **Step 4.5a — D + drop Phase 1 (closes L1)** between current Step 5
  (verification-only sweep) and current Step 6 (PSI Phase 2). Validation gate:
  `JavaUsingAst*` matrix unchanged; trip-wire pair (`testJ_k_complex` +
  `testMapMethodsImplementedInJava`) green; perf-counter re-run on the Step-3 testbed
  is structurally a no-op (D removes a `lazyResolveToPhase` no-op + a lazy
  `superTypeRefs` enhancement read; no new parsing surface).
- Insert **Step 4.5b — B (closes L2)** after 4.5a. Validation gate: same matrix +
  trip-wire suite + same-file fast-path parity check + perf-counter re-run.
- **Re-classify the existing Step 4 entry** as "Stages 4 + partial 5 (KDoc)" so the
  per-step accounting reflects what actually landed.

This is a docs-only edit. It does not disturb Steps 1–5 (already landed and logged) or
Steps 6–7 (their prerequisites do not depend on the unification track's residue, and
in fact the §6 third-bullet "audit becomes a propagation, not an invention" argument
gets *stronger* after 4.5a + 4.5b: the four indirect callers in PSI doc §1.5 then
plug into a fully-unified resolver, not a half-unified one).

---

## 6. Cross-references

- [`MERGED_REFACTORING_PLAN_2026_05_04.md`](MERGED_REFACTORING_PLAN_2026_05_04.md) —
  the *when / in what order*; this doc proposes 4.5a and 4.5b inserts there.
- [`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md) —
  the *what / why* of unification; owns the five laziness invariants and three failure
  modes that any L1 closer must continue to uphold.
- [`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md) —
  the *what / why* of PSI removal; owns the indirect-caller catalogue (§1.5) that
  Step 6 re-routes through `session.symbolProvider`.
- [`CLASSIFIER_RESOLUTION_TRACE_2026_05_04.md`](CLASSIFIER_RESOLUTION_TRACE_2026_05_04.md) —
  end-to-end classifier-resolution trace; useful for predicting which call sites the
  L1 / L2 closers affect.
- [`ITERATION_RESULTS.md`](../ITERATION_RESULTS.md) — per-iteration log; the Step-3
  entry's "Stage 2b post-mortem" sub-section records the trip-wire reproductions that
  motivated this design space.
- [`AGENT_INSTRUCTIONS.md`](../AGENT_INSTRUCTIONS.md) — non-negotiable rules for
  working in the module (no command chaining, performance-measurement harness,
  log-piping conventions); applies to any 4.5a / 4.5b implementation iteration.
