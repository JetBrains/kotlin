# Merged Refactoring Plan: PSI Removal × Resolver Unification — 2026-05-04

> **Status snapshot.** Baseline is `HEAD` (commit `b300d9ac8536` plus the 2026-05-04
> follow-ups), with PSI Phase 1 already landed and turned ON by default
> (`USE_BINARY_FINDER = true` in
> `compiler/java-direct/src/.../JavaDirectPluginRegistrar.kt`); 2692/2692 (100%)
> `JavaUsingAst*` tests are green. See [`ITERATION_RESULTS.md`](../ITERATION_RESULTS.md)
> 2026-04-30 / 2026-05-04 entries for the landed work and follow-ups (six original
> failures fixed; `<javaSourceRoots packagePrefix="…">` plumbed through `JavaPackageIndexer`).
> This document sequences the two remaining design tracks; it does not change either
> track's architectural decisions and modifies no production source files.
>
> See also: [`AGENT_INSTRUCTIONS.md`](../AGENT_INSTRUCTIONS.md),
> [`implDocs/ARCHITECTURE.md`](ARCHITECTURE.md),
> [`implDocs/RESOLUTION_PIPELINE.md`](RESOLUTION_PIPELINE.md),
> [`implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md),
> [`implDocs/RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md),
> [`implDocs/UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md`](UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md)
> (superseded by the next entry; kept for fallback alternatives A / C / F),
> [`implDocs/FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md`](FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md)
> (chosen redesign track for closing the unification residue; Step 4.5a / 4.5b inserts and the Step 4 re-classification are now applied to §5 below),
> [`implDocs/CLASSIFIER_RESOLUTION_TRACE_2026_05_04.md`](CLASSIFIER_RESOLUTION_TRACE_2026_05_04.md).

---

## 1. Overview

Two refactorings on the `java-direct` story have been designed and reviewed independently,
and now need a shared execution timeline. The **PSI-replacement** track
([`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md))
removes the IntelliJ-platform dependency for binary Java lookups, in three phases (Phase 1
already landed). The **resolver-unification** track
([`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md))
collapses the AST-vs-FIR split for classifier resolution into a single FIR-native path,
while preserving the laziness contract — five invariants, three failure modes — that
predominantly-Java compilation depends on. Together, they leave `java-direct` with one
symbol provider per data source, one origin-agnostic resolver, and an AST-or-PSI choice
that survives only as a *source-side* plugin during a 1–2-release transition window
before PSI is removed entirely.

This document is the **single source of truth for execution order** going forward. It
states the merged sequence, identifies coupling points between the two threads, and
explains the rationale concisely. Full design content stays in the two source documents;
this doc references them rather than duplicating.

## 2. Motivation

The two plans were designed independently. A cross-check pass concluded they are
**compatible and largely reinforcing** — both converge on `session.symbolProvider` as the
origin-agnostic classifier entry, both implement *one symbol provider per data source*,
both retire origin-based special-casing for the same architectural reason, and both
preserve laziness via per-provider data ownership. Apparent tensions
(`knownClassNamesInPackage` union shape, the same-file fast-path performance argument,
the `Java.Source` origin check) all resolve in favour of compatibility on inspection: they
touch the same surface for different reasons, and the reasons compose. The full cross-check
analysis lives in the planning-round chat history; this doc does not re-derive it.

A separate ordering review concluded that the unification track is mostly **local**
(`java-direct` plus one shared FIR file, `JavaTypeConversion.kt`), while PSI Phase 2/3
reshape the JVM-FIR symbol-provider topology more **broadly**. That asymmetry is the basis
for the merged ordering chosen here: unification first on a clean Phase-1 baseline, with
the performance gate (parse counter / symbol-creation counter on
`IntelliJFullPipelineTestsGenerated.testIntellij_platform_externalProcessAuthHelper`) run
*before* the more invasive PSI Phase 2 begins.

**Non-goals.** This document does not change the architectural decisions of either source
plan; it only sequences them. It does not touch LL-FIR or the non-`java-direct`
compilation path (LL-FIR's parallel `LLFirJavaSymbolProvider` /
`LLCombinedJavaSymbolProvider` is already out of scope of both source docs and stays out
of scope here). It introduces no new design content beyond the rationale for the chosen
order.

## 3. Expected Results

End-state after all steps land:

- `JavaClassFinderImpl` (PSI) is removed from JVM-FIR / `java-direct`. The
  IntelliJ-platform dependency that motivated the work is gone from this surface.
  Owns:
  [`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` §2.5](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md#2.5).
- `CombinedJavaClassFinder` and `BinaryJavaClassFinder` are deleted. The only
  `JavaClassFinder` left is the source-side one — AST-backed by default, PSI-backed during
  the 1–2-release transition behind a flag.
  Owns:
  [`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` §2.4](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md#2.4),
  [§2.5](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md#2.5).
- Classifier resolution goes through one origin-agnostic FIR path; the AST-side resolver
  shrinks to its irreducible core (type parameters, `containingClassIds`, and the
  same-file fast path). Becomes **literally** true post-Step-4.5b, when
  `JavaScopeResolver.findLocalClass` and `JavaClassOverAst.findInnerClassInSupertypes`
  retire and `JavaClassifierType.classifier?.classId` answers every cross-file reference
  via `firSession.symbolProvider`.
  Owns:
  [`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md` §Stage 5](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md),
  [`FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` §10](FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md).
- The five laziness invariants are upheld and the three failure modes remain guarded
  against. Post-Step-4.5a, failure mode 1's mitigation moves from *structural*
  (the model has no `FirSession`) to *policy + typed wrapper* — invariants 1, 2, 3
  are enforced by KDoc + an `AGENT_INSTRUCTIONS.md` rule + a typed `LazySessionAccess`
  wrapper that makes the laziness contract checkable rather than reviewable. The new
  `JavaSupertypeLoopChecker` (model-side analogue of K1's `SupertypeLoopChecker`)
  bounds inheritance cycles on every supertype-walking entry point and emits
  `CYCLIC_INHERITANCE_HIERARCHY` for Java-only cycles that today silently truncate.
  Owns:
  [`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md` §Five laziness invariants](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md),
  [§Three failure modes](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md),
  [`FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` §§6.1, 7, 8](FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md).
- Performance is not regressed on the agreed CI testbed
  (`IntelliJFullPipelineTestsGenerated.testIntellij_platform_externalProcessAuthHelper`):
  parse-counter / symbol-creation-counter values after Step 4 (end of unification) and
  after Step 6 (end of PSI Phase 2) are within noise of the Phase-1 baseline, with each
  measurement attributable to a single redesign.
  Owns:
  [`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md` §Verification under realistic loads](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md).

## 4. Source documents and their continuing roles

This doc is the *when* and *in what order*. The two source documents continue to be the
*what* and *why*; `ITERATION_RESULTS.md` continues to be the per-iteration *what landed
and when*.

| Document | Role | Owns |
|---|---|---|
| [`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md) | What/why of PSI removal. | Three-phase design, indirect-caller catalogue (§1.5), risks per phase (§2.7). |
| [`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md) | What/why of resolver unification. | Five laziness invariants, three failure modes, five-stage migration. |
| [`UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md`](UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md) | **Superseded** — design space for the L1 / L2 leftovers (alternatives A–F + Step-6 compatibility verdicts). | Kept for fallback closers A / C / F if `FirSession` injection is rejected; the §2 timing-bug analysis remains canonical. |
| [`FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md`](FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md) | Redesign track for closing the unification residue (L1 + L2); supersedes the alternatives doc. | Deletion of `JavaClassifierType.resolve(...)` and `JavaAnnotation.resolveAnnotation(...)`; per-origin supertype routing; `JavaSupertypeLoopChecker` (cycle bound + diagnostic emission); typed `LazySessionAccess`; `directSupertypeClassIds()` cache on `FirJavaClass` (variant C); per-file plan for Step 4.5a / 4.5b (§5 inserts and Step 4 re-classification **applied 2026-05-06**). |
| [`ITERATION_RESULTS.md`](../ITERATION_RESULTS.md) | Per-iteration log. | Dated entries for each landed step (overview / changes / test results / files / key learnings). |
| `MERGED_REFACTORING_PLAN_2026_05_04.md` (this doc) | When / in what order. | Step ordering, per-step prerequisites and validation gates, coupling points. |

This doc **does not duplicate iteration entries**. `ITERATION_RESULTS.md` continues to log
each landed step in the order set here; if a step lands or is deferred, it is recorded
there and not edited into this plan.

## 5. Merged execution order

Each step uses the same template:

```
#### Step <N> — <short title>
- Origin: <PSI Phase X | Unification Stage X>
- Goal (one line):
- Prerequisites:
- Validation gate:
- References: <link to source-doc section>
```

#### Step 1 — PSI Phase 1: `BinaryJavaClassFinder` ✅ already landed

- **Origin**: PSI Phase 1.
- **Goal**: Replace the PSI binary half of `CombinedJavaClassFinder` with an index-based,
  PSI-free `BinaryJavaClassFinder` backed by `JvmDependenciesIndex` / `KotlinClassFinder`
  + ASM `BinaryJavaClass`.
- **Prerequisites**: none.
- **Validation gate**: `JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated`
  full matrix green; representative `IntelliJFullPipelineTestsGenerated` subset green.
- **Status**: landed and turned ON by default
  (`USE_BINARY_FINDER = true` in
  `compiler/java-direct/src/.../JavaDirectPluginRegistrar.kt`); 2692/2692 (100%) green.
  See [`ITERATION_RESULTS.md`](../ITERATION_RESULTS.md):
  - 2026-04-30 — Phase 1 landed behind a default-OFF flag (initial 2686/2692 with flag
    ON, six follow-up failures recorded).
  - 2026-05-04 — six follow-up failures fixed (ancestor `findPackage` recognition;
    `;`-tolerant `PACKAGE_REGEX` matching PSI's error-tolerant Java parser); the flag
    was flipped to default-ON.
  - 2026-05-04 (later) — `<javaSourceRoots packagePrefix="…">` plumbed through
    `JavaPackageIndexer` so `IntelliJFullPipelineTestsGenerated` representative tests
    pass.
- **References**:
  [`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` §2.2](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md#2.2),
  [§2.6 Phase 1](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md#2.6).

#### Step 2 — Unification Stages 1–2 (mechanical, risk-free)

- **Origin**: Unification Stages 1 and 2.
- **Goal**: Thread `getClassLikeSymbol` through `JavaResolutionContext` (Stage 1); narrow
  the AST-side resolver (`JavaScopeResolver.findLocalClass`) to type parameters +
  `containingClassIds` + the same-file fast path; drop `JavaInheritedMemberResolver`'s
  Phase 1 (the AST walk over Java-source supertypes) in favour of the existing Phase 2
  (FIR-callback walk) (Stage 2).
- **Prerequisites**: Step 1 green on default-ON flag (already true).
- **Validation gate**: full `JavaUsingAst*` matrix unchanged (2692/2692).
- **References**:
  [`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md` §Stage 1](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md),
  [§Stage 2](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md).

#### Step 3 — Unification Stage 3 (load-bearing; perf gate on clean Phase-1 baseline)

- **Origin**: Unification Stage 3.
- **Goal**: Replace the `FirDeclarationOrigin.Java.Source` filter in
  `getResolvedSupertypeClassIds` (`JavaTypeConversion.kt`) with
  `lazyResolveToPhase(SUPER_TYPES)`. This is the substantive correctness-and-laziness
  piece of the unification: origin stops being the cycle-bound, the lazy-phase model
  bounds it.
- **Prerequisites**: Step 2 green.
- **Validation gate**: `JavaUsingAst*` matrix unchanged + parse-counter /
  symbol-creation-counter check on
  `IntelliJFullPipelineTestsGenerated.testIntellij_platform_externalProcessAuthHelper`.
  The performance gate runs on the **clean Phase-1 baseline** (no PSI Phase 2 changes
  yet), so any regression observed is attributable solely to Stage 3. The five
  laziness invariants and three failure modes are the explicit checklist for any
  regression triage.
- **References**:
  [`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md` §Stage 3](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md),
  [§Five laziness invariants](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md),
  [§Three failure modes](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md).

#### Step 4 — Unification Stages 4 + partial 5 (KDoc only)

- **Origin**: Unification Stages 4 and partial Stage 5 (KDoc-only — full Stage 5
  body retirement is now Step 4.5b below).
- **Goal**: Collapse `findLocalClass` out of the `ClassId`-resolution path (Stage 4);
  the origin-agnostic outcome (Stage 5) is delivered as KDoc on
  `JavaScopeResolver.findLocalClass` documenting its post-Stage-4 role as the
  AST-side fast path for `JavaTypeOverAst.computeClassifier` /
  `JavaClassCache` / `ConstantEvaluator`. The full body retirement is deferred
  to Step 4.5b once the model has direct access to FIR-derived classifier data
  (post-Step-4.5a `FirSession` injection).
- **Prerequisites**: Step 3 green; Stage 3's parse-counter baseline recorded.
- **Validation gate**: full `JavaUsingAst*` matrix unchanged + re-run parse counter on
  the Stage-3 testbed (must be ≤ Step 3's value, within noise).
- **References**:
  [`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md` §Stage 4](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md),
  [§Stage 5](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md),
  [`FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` §13](FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md)
  (Step 4 re-classification rationale).

#### Step 5 — Performance & test-data sweep (verification only)

- **Origin**: cross-cut. No code change.
- **Goal**: Confirm the Phase-1 baseline still holds after the unification track:
  parse-counter / symbol-creation-counter values are identical or smaller than at the end
  of Step 1; `JavaUsingAst*` 2692/2692 is unchanged. Captures the pre-Phase-2 reference
  point so any later regression is attributable to Phase 2 alone.
- **Prerequisites**: Step 4 green.
- **Validation gate**: pure verification — no source change. Numbers recorded in
  [`ITERATION_RESULTS.md`](../ITERATION_RESULTS.md) at the same level of detail as the
  existing Phase-1 entries.
- **References**:
  [`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md` §Verification under realistic loads](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md);
  [`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` §2.7 Phase 1](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md#2.7).

#### Step 4.5a — `FirSession` injection + `resolve(...)` deletion + drop Phase 1 (closes L1)

- **Origin**: `FirSession`-injection redesign track (closes L1 — drop Phase 1
  of `JavaInheritedMemberResolver`'s inherited-inner BFS).
- **Goal**: Land the load-bearing **deletion** of `JavaClassifierType.resolve(...)`
  and `JavaAnnotation.resolveAnnotation(...)` from `core/compiler.common.jvm`'s
  public interfaces; restore `JavaTypeConversion.resolveTypeName` to its
  pre-`java-direct` body (`classifier?.classId ?: findClassIdByFqNameString(...) ?: ClassId.topLevel(...)`);
  collapse `JavaInheritedMemberResolver`'s two-phase BFS into a single per-class-dispatched
  origin-agnostic loop using a new model-internal `directSupertypeClassIds(classId)`
  dispatcher; introduce the new `directSupertypeClassIds()` cache on `FirJavaClass`
  (variant C of the alternatives doc); introduce the typed `LazySessionAccess`
  wrapper as the failure-mode-1 mitigation; introduce `JavaSupertypeLoopChecker`
  (model-side analogue of K1's `SupertypeLoopChecker`) for inheritance-cycle
  bounding + `CYCLIC_INHERITANCE_HIERARCHY` emission for Java-only cycles; delete
  the dead `JavaResolvedClassOrigin` / `JavaResolvedClassLikeSymbol` plumbing
  added in Step 2's Stage 1; migrate the three test-fixture call sites in
  `JavaParsingMembersTest.kt` and `JavaParsingTypeResolutionTest.kt` to property
  reads (`paramType.classifier?.classId`) backed by a shared minimal-`FirSession`
  helper.
- **Prerequisites**: Step 5 green; **`FirSession` is reachable inside the Java
  Model** (the wiring iteration — late-init on `JavaClassFinderOverAst` first,
  restructured entry point later — is itself a separate piece of work, not
  scoped to this step).
- **Validation gate**: full `JavaUsingAst*` matrix unchanged; trip-wire pair
  green (`Tests.Generics.InnerClasses.testJ_k_complex` and
  `Tests.J_k.CollectionOverrides.testMapMethodsImplementedInJava`); cross-origin
  re-entry trip-wire green (`KJKComplexHierarchyNestedLoop.kt`); new diagnostic
  test cases for direct (`A extends A`) and indirect (`A extends B; B extends A`)
  Java-only inheritance cycles produce `CYCLIC_INHERITANCE_HIERARCHY` and do
  not deadlock; `git diff` review per `AGENT_INSTRUCTIONS.md` rule 4. Perf:
  parse counter unchanged (no new parses); symbol-creation counter unchanged
  call distribution; the BFS path is structurally cheaper (no
  `lazyResolveToPhase(SUPER_TYPES)` no-op + `superTypeRefs` enhancement read on
  the Java arms).
- **References**:
  [`FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` §11 "Step 4.5a"](FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md);
  [`UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md` §3 "C — pre-resolved supertype-`ClassId` cache"](UNIFICATION_CLOSURE_ALTERNATIVES_2026_05_05.md)
  (variant C and fallback variant D).

#### Step 4.5b — Stage-5 collapse (closes L2)

- **Origin**: `FirSession`-injection redesign track (closes L2 — retire
  `JavaScopeResolver.findLocalClass` body and
  `JavaClassOverAst.findInnerClassInSupertypes`).
- **Goal**: Retire the AST-side `JavaScopeResolver.findLocalClass` body;
  retire `JavaClassOverAst.findInnerClassInSupertypes` (or shrink it to a
  same-file fast path); upgrade `JavaClassifierTypeOverAst.computeClassifier()`'s
  cross-file branch (introduced minimally in 4.5a as a `JavaClassifier` with only
  `classId` populated) to wrap a full `JavaClass`-shaped adapter
  (`FirBackedJavaClassAdapter`) backed by the C-cache landed in 4.5a (or by
  the fallback-variant-D `firClass.javaClass` direct read, whichever 4.5a
  picked). The unification headline (§3 bullet 3) becomes literally true.
- **Prerequisites**: Step 4.5a green.
- **Validation gate**: full `JavaUsingAst*` matrix unchanged; trip-wire pair
  green (`testJ_k_complex`, `testMapMethodsImplementedInJava`); same-file
  fast-path parity check (the AST-only fast path for same-file references
  must still bypass the symbol provider); `KJKComplexHierarchyNestedLoop.kt`
  green; perf-counter re-run (must be within noise of the post-Step-4.5a
  baseline).
- **References**:
  [`FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` §11 "Step 4.5b"](FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md).

#### Step 6 — PSI Phase 2: structural refactoring of the symbol-provider topology

- **Origin**: PSI Phase 2.
- **Goal**: `JavaSymbolProvider` becomes source-only; `JvmClassFileBasedSymbolProvider`
  takes ownership of binary Java lookups (callers (E)–(I) catalogued in PSI doc §1.4); the
  four indirect callers in PSI doc §1.5 (`FirJvmConflictsChecker`,
  `FirDirectJavaActualDeclarationExtractor`, Lombok `AbstractBuilderGenerator`, and the
  LL-FIR Analysis API component — out of scope for `java-direct` itself but listed for
  completeness) are re-routed through `session.symbolProvider` with optional Java-origin
  filtering. `CombinedJavaClassFinder` *and* the Phase-1 `BinaryJavaClassFinder` are
  deleted; `FirJavaFacade.classFinder` becomes the source-only finder.
- **Prerequisites**: Steps 4.5a + 4.5b green (the model's classifier path now
  goes through `firSession.symbolProvider` for cross-origin queries —
  *exactly* the post-Step-6 canonical entry, so the indirect-caller audit
  becomes a propagation, not an invention); Stage 3's lazy-phase routing
  already in place (Step 3), so the indirect-caller audit reuses the
  `session.symbolProvider` + optional origin-filter pattern unification just
  made canonical.
- **Validation gate**: full `JavaUsingAst*` matrix + re-run parse counter and
  symbol-creation counter on the same testbed. Any new regression is attributable to
  Phase 2 alone, since Steps 1–5 set the prior baseline.
- **References**:
  [`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` §2.4](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md#2.4)
  (all subsections),
  [§2.6 Phase 2](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md#2.6),
  [§2.7 Phase 2](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md#2.7).

#### Step 7 — PSI Phase 3: source-only AST/PSI switch + 1–2-release transition

- **Origin**: PSI Phase 3.
- **Goal**: Repurpose `JavaClassFinderFactory` as a *source-finder* factory that picks
  `JavaClassFinderOverAstImpl` (java-direct, default) or `JavaClassFinderImpl` scoped to
  source files (legacy PSI source path) behind a flag. The PSI source side stays available
  for 1–2 releases as a parity safety net. At the end of the window — once the AST source
  path has been validated against PSI source parity in production — delete
  `JavaClassFinderImpl` from the `java-direct` path; the residual IntelliJ-platform
  dependency from this surface is gone.
- **Prerequisites**: Step 6 green; release window scheduled.
- **Validation gate**: AST/PSI source-path parity tests (full
  `JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated` matrix run against
  both source-finder choices); once both legs are green and the transition window has
  elapsed, the PSI leg is removed.
- **References**:
  [`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` §2.5](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md#2.5),
  [§2.6 Phase 3](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md#2.6),
  [§2.7 Phase 3](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md#2.7).

## 6. Coupling points and shared work

The two threads touch each other in four well-defined places. Each is owned in detail by
one of the source documents; this section flags the linkage.

- **Indirect-caller audit shared between Step 3 and Step 6.** Step 3 establishes the
  `session.symbolProvider` + optional origin-filter pattern inside `JavaTypeConversion.kt`
  (a single FIR file). Step 6 then *propagates* that pattern to the four external callers
  catalogued in PSI doc §1.5 (`FirJvmConflictsChecker`,
  `FirDirectJavaActualDeclarationExtractor`, Lombok's `AbstractBuilderGenerator`, the
  LL-FIR Analysis API component). The audit becomes a propagation, not an invention —
  which is the second-largest reason to do unification first. See PSI doc
  [§2.4.4](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md#2.4) and unification doc
  [§Stage 3](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md).
- **Doc-wording follow-ups when Step 6 lands.** The unification doc's chain quote
  `tryResolve(ClassId) → JvmSymbolProvider → CombinedJavaClassFinder → BinaryJavaClassFinder (miss) → JavaClassFinderOverAstImpl`
  becomes stale post-Phase-2 (those classes are deleted). The doc's *argument* survives —
  the chain just shortens to
  `tryResolve(ClassId) → session.symbolProvider → JvmClassFileBasedSymbolProvider | JavaSymbolProvider → JavaClassFinderOverAstImpl (source side only)`.
  Refresh the chain quote in
  [`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md)
  (around its "Why the code looks the way it does today" reason 2 and the closing
  "Net assessment") in the same iteration that lands Step 6, so the design narrative
  matches reality.
- **Parse-counter / symbol-creation-counter CI guardrail — run twice.** Once after Step 3
  (clean Phase-1 baseline), once after Step 6 (post-Phase-2 baseline). Each run isolates a
  single redesign's effect. Without the split, a regression observed only at the end of
  the combined work would have to be bisected by hand between the two threads.
- **Phase-1 follow-up failures.** As of 2026-05-04 the original six `assertEqualsToFile`
  divergences (PSI doc [§2.6 Phase 1](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md#2.6)) are
  resolved on the source half (ancestor `findPackage` + `;`-tolerant `PACKAGE_REGEX`; see
  [`ITERATION_RESULTS.md`](../ITERATION_RESULTS.md) 2026-05-04 entry "Phase 1 follow-up:
  fix the six failures triggered by enabling `BinaryJavaClassFinder`"). Step 5's sweep
  re-confirms they have not regressed. Step 6 *dissolves* the abstraction itself
  (`BinaryJavaClassFinder` goes away), so the failure class becomes vacuous from that
  point on.

## 7. Rationale for this ordering

Three observations drive the order. They are summaries of a longer planning-round
analysis; the long form lives in the chat history of the prior rounds and is not repeated
here.

- **Smaller blast radius first.** Unification is mostly local — Stages 1–2 and 4–5 live
  inside the `java-direct` module; only Stage 3 changes a single shared FIR file
  (`JavaTypeConversion.kt`), and only one function within it. PSI Phase 2 reshapes the
  JVM-FIR symbol-provider topology and re-routes four external callers; that is a much
  larger surface. Landing the local refactoring first leaves a smaller failure space if
  something goes wrong, and a stable platform on which the wider refactoring stands.
- **Clean baseline for the performance gate.** Stage 3's perf gate (parse counter /
  symbol-creation counter on
  `IntelliJFullPipelineTestsGenerated.testIntellij_platform_externalProcessAuthHelper`)
  is the load-bearing signal that the lazy-phase routing has not introduced eagerness
  leaks (one of the unification doc's three failure modes). Running it on the Phase-1
  baseline, before the symbol-provider topology shifts, ensures any regression seen there
  is attributable to Stage 3 alone, not to a superposition of two redesigns. Phase 2's
  re-run of the same gate then isolates Phase 2's effect cleanly. Reversing the order
  would mean the second gate run covers two superimposed changes and the regression
  source would have to be bisected by hand.
- **Audit-work re-use.** PSI Phase 2's hardest piece is the audit of indirect callers
  that today rely on `session.javaSymbolProvider`'s binary fall-through. By the time
  Phase 2 begins, Stage 3 has already established the unified `session.symbolProvider` +
  optional origin-filter pattern *inside* `JavaTypeConversion.kt`. The Phase 2 audit then
  *extends* the same pattern to four external callers, instead of inventing it from
  scratch.

The trade-off is explicit: under this ordering, the IntelliJ-platform-dependency removal
— the strategic motivation behind PSI removal — lands later than it would under "Phase 2
first". Unless that goal has a hard deadline (it does not, at time of writing), the
safer-to-debug ordering wins.

## 8. Cross-references

- [`AGENT_INSTRUCTIONS.md`](../AGENT_INSTRUCTIONS.md) — non-negotiable rules for working
  in this module (no command chaining, performance-measurement harness, log-piping
  conventions).
- [`implDocs/ARCHITECTURE.md`](ARCHITECTURE.md) — key architecture decisions; type /
  annotation resolution callback pattern.
- [`implDocs/RESOLUTION_PIPELINE.md`](RESOLUTION_PIPELINE.md) — end-to-end resolution
  pipeline reference.
- [`implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md)
  — the *what* and *why* of PSI removal. Owns the three-phase design and risks.
- [`implDocs/RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`](RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md)
  — the *what* and *why* of resolver unification. Owns the laziness invariants and
  failure modes.
- [`implDocs/CLASSIFIER_RESOLUTION_TRACE_2026_05_04.md`](CLASSIFIER_RESOLUTION_TRACE_2026_05_04.md)
  — sister document to the unification plan; traces a single classifier resolution
  end-to-end through the *current* architecture.
- [`ITERATION_RESULTS.md`](../ITERATION_RESULTS.md) — per-iteration log; each landed step
  in this plan adds a dated entry there.
