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
