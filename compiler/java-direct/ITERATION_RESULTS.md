# Java-Direct: Iteration Results Log

**Current status**: 1178/1178 box + 1513/1513 phased (2793/2793, 100%). No known won't-fix.

**Last archived**: `implDocs/archive/ITERATION_RESULTS_2026_06_01.md` (entries through 2026-06-01).

---

## How to write entries

This log is read into the agent's context every session, so **entries must stay short**.

- **Newest entry on top.** One entry per landed change or per investigated regression.
- **Cap each entry at ~15 lines / ~150 words.** If the rationale, a trace, or a
  measurement table is longer, put it in a dedicated `implDocs/<TOPIC>.md` and link to it
  from the entry — do not inline it here.
- **Use the fixed fields below.** No free-form multi-paragraph narration; if a field needs
  more than ~2 lines, link out instead.
- **No pasted logs, stacktraces, or diffs.** Quote the single line that matters; link the rest.
- **Archive when this file passes ~600 lines** (see `AGENT_INSTRUCTIONS.md` →
  "Docs Maintenance"): `git mv` it to
  `implDocs/archive/ITERATION_RESULTS_<last-entry-date>.md`, add an archive banner, and
  reset this file to the template below.

### Entry template

```
### YYYY-MM-DD — <one-line title>
- **Change**: what changed and why (1–3 lines).
- **Files**: key files touched (+N/−M LoC if useful).
- **Tests**: suites run + counts (e.g. box 1178/1178, phased 1513/1513).
- **Result**: green / regression fixed / won't-fix — link to a detail doc if there is one.
```

---

<!-- Add new entries below, newest first. -->

### 2026-06-10 — Relocate inherited-inner outer-arg recovery from shared FIR into the model
- **Change**: Give `FirBackedJavaClassAdapter` a real on-air-resolved `supertypes` chain (mirroring
  `FirJavaElementFinder.resolveSupertypesOnAir`) and move the implicit-outer-class-type-argument
  recovery for bare inherited inner-class refs into `JavaClassifierTypeOverAst.computeTypeArguments`;
  delete the java-direct-specific recovery (`outerTypeArgs` branch + 3 helpers) and the
  `containingClassSymbol` side-channel from shared FIR. Design: `implDocs/MODEL_SIDE_OUTER_ARG_RECOVERY_2026_06_10.md`.
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
  `implDocs/archive/ITERATION_RESULTS_2026_05_11.md`) for the in-flight guard and the hypothetical
  malformed-cyclic-Java pattern for the supertype guard.
- **Files**: `JavaModelSessionAccess.kt` (+guard), `JavaTypeResolver.kt`, `JavaFileContext.kt`
  (−`cycleChecker`), `JavaClassFinderOverAstImpl.kt` (+register), `JavaCycleBreakerTest.kt`;
  deleted `JavaSupertypeCycleChecker.kt`.
- **Tests**: `:compiler:java-direct:test` 2816/2816 (455 files, 0 failures); `JavaCycleBreakerTest`
  4/4 (each breaker proven load-bearing — `StackOverflowError` when the guard component is absent).
- **Result**: green; valid-code paths unaffected, both breakers stay out of the way.
