# Java-Direct: Iteration Results Log

**Current status**: 1178/1178 box + 1513/1513 phased (2793/2793, 100%).

**Last Updated**: 2026-05-25 (Same-day later: shared CLI diagnostic
`testJavaSrcWrongPackage` `.out` update — under unconditional
`java-direct`, `A.java`-declaring-`foo`-but-placed-at-the-root is
not indexed as `<root>.A` (matches `javac`; PSI was indexing
physical paths via `JvmDependenciesIndex` and then reading
`PsiClass.qualifiedName` from content, producing a self-inconsistent
`return type mismatch '<root>.A.Nested' vs 'foo.A.Nested!'` chain),
so the new diagnostic is two `unresolved reference 'A'` errors —
pure test-expectation update, no production change; full root-cause
in `implDocs/JAVA_SRC_WRONG_PACKAGE_2026_05_25.md`.
Earlier same day: fresh `fir-jvm`-vs-`ff12cbb3` diff audit
+ §3.4 `JavaTypeParameterWithFirSymbol` interface deletion + §3.14
`javaAnnotationsMapping.kt` graceful-fallback dead-branch cleanup;
`fir-jvm` diff vs `ff12cbb3` shrinks from `+397 / −53` to roughly
`+371 / −53`. Same-day predecessor: Category γ TYPE_USE filter +
`JavaModelExtensions.kt`'s remaining two callback interfaces all
relocated to java-direct. File `JavaModelExtensions.kt` is gone;
FIR-jvm carries no java-direct-specific protocol interface anymore.
≈−367 LoC on FIR-jvm, ≈−30 LoC net codebase).

> **Caveat on historical numbers.** Before 2026-04-28, the `JavaUsingAst*` test
> generators did **not** actually route `// FILE: *.java` blocks through
> `java-direct`'s AST — they fell through to PSI's `JavaClassFinderImpl`. Any
> "1168/1168 box" / "1454/1456 phased" / "feature complete" status claim dated
> before 2026-04-28 was measured against the PSI loader, not `java-direct`. The
> 2026-04-28 framework fix grew the suite to 2793 tests and surfaced fresh
> regression categories, all resolved by 2026-05-11.

## Recent history (one-liners)

- **2026-05-25** — Shared CLI diagnostic test
  `org.jetbrains.kotlin.cli.CliTestGenerated.DiagnosticTests.testJavaSrcWrongPackage`
  unmuted under unconditional `java-direct`. The fixture places
  `A.java` declaring `package foo;` physically at the source root
  (not under `foo/`) and a Kotlin file referencing bare `A`.
  Pre-existing PSI loader path produced the diagnostic pair
  `return type mismatch: expected '<root>.A.Nested', actual
  'foo.A.Nested!'` (col 24) + `cannot access class 'foo.A.Nested'.
  Check your module classpath for missing or conflicting dependencies`
  (col 28) — an artefact of PSI's two-layer split, where
  `KotlinCliJavaFileManagerImpl.findVirtualFileForTopLevelClass`
  indexes `.java` files by *physical path* via `JvmDependenciesIndex`
  (so `<root>.A` is discoverable), and then `PsiClass.qualifiedName`
  reads the *declared* `package` statement and reports `foo.A` — K2
  cannot reconcile the requested `ClassId` with the returned class's
  self-reported FQN and emits the mismatch + cannot-access pair.
  `java-direct` deliberately does not replicate that split: per the
  `JavaPackageIndexer.kt:174` invariant — *"Files with mismatched
  package/directory are skipped, matching javac behavior"* — the
  per-package `tryBuildFileEntry(file, packageFqName)` walk drops
  files whose declared package disagrees with the directory it is
  scanning. The dir-roots-only hoist at `JavaPackageIndexer.kt:98–110`
  *does* register top-level `.java` files of a directory root under
  their **declared** package (making `foo.A` discoverable for the
  test-infrastructure case), but it does **not** register them under
  `<root>`, so the `.kt`'s bare `A` falls through and produces two
  `unresolved reference 'A'` errors (cols 13 and 24). The new
  diagnostic is cleaner (no spurious "classpath" red herring) and
  matches `javac`'s own behaviour for the same layout. Fix is a pure
  test-expectation update: `compiler/testData/cli/jvm/diagnosticTests/
  javaSrcWrongPackage.out` rewritten to the two `unresolved reference
  'A'` lines; no production code change. Rule §6 exception applies
  because (a) the fixture is a shared CLI diagnostic test, not
  `java-direct`'s own corpus, (b) the new behaviour is the documented
  design of `JavaPackageIndexer`, and (c) no test semantics are
  weakened — the program still fails to compile, only the wording /
  location is updated. Verification:
  `./gradlew :compiler:tests-integration:test --tests
  "org.jetbrains.kotlin.cli.CliTestGenerated\$DiagnosticTests.testJavaSrcWrongPackage"`
  → `BUILD SUCCESSFUL` (was: `1 test completed, 1 failed`); manual
  compiler invocation on the fixture produced the matching two
  `unresolved reference 'A'` lines modulo the framework's
  `COMPILATION_ERROR` trailer. Full writeup with the PSI/`java-direct`
  semantic divergence diagram and an open backlog note on whether the
  fixture should be reshaped to make its intent explicit (or replaced
  by a fixture that triggers a genuine cross-language FQN mismatch
  through a path surviving `javac`'s rules) lives in
  `compiler/java-direct/implDocs/JAVA_SRC_WRONG_PACKAGE_2026_05_25.md`.

- **2026-05-25** — Fresh `fir-jvm`-vs-`ff12cbb3` diff audit + minimisation
  wave landed. Earlier in the day a ground-up audit of the `+397 / −53`
  `fir-jvm` diff (vs base `ff12cbb3d915`) was written up as
  `compiler/java-direct/implDocs/FIR_JVM_DIFF_ANALYSIS_2026_05_25.md`
  (776 lines), enumerating the 11 distinct logical change clusters
  (`F1`/`C1`/`S1`/`S2`/`H1…H5`/`J1…J5`/`A1`) and grading each by
  liveness, rule-§7 status, and rollback feasibility. After the user
  confirmed the committed branch had been validated against broader
  corpora (`KotlinFullPipelineTestsGenerated`,
  `IntelliJFullPipelineTestsGenerated`), the realistic §4 minimisation
  budget was applied: **§3.4** — `JavaTypeParameterWithFirSymbol`
  interface deleted from
  `compiler/fir/fir-jvm/src/.../MutableJavaTypeParameterStack.kt`
  (−19 LoC on `fir-jvm`); supertype + import dropped from
  `FirBackedJavaTypeParameter` in
  `compiler/java-direct/src/.../resolution/FirBackedJavaClassAdapter.kt`
  (kept `firTypeParameterSymbol` as a plain `internal val` for the
  adapter's own identity / equality / debug `toString` — the field is
  what makes the cross-file outer-type-parameter wrapper stable; the
  qualified-form raw-detection walk in
  `JavaClassifierTypeOverAst.computeIsRaw` reads `outer.typeParameters`
  for counts only, so the wrapper does not need a separate FIR-side
  shortcut interface). Stale KDoc in `FirBackedJavaClassAdapter.kt`
  and `JavaTypeOverAst.kt` was rewritten to no longer cite the
  retired interface. **§3.14** — `javaAnnotationsMapping.kt` mechanical
  cleanup: unused `org.jetbrains.kotlin.fir.resolve.providers.symbolProvider`
  import removed; the structurally-dead inner `if (fallbackClassId != null)`
  recomputation in the `JavaEnumValueAnnotationArgument →` arm
  collapsed (the outer `enumClassId ?: expectedArrayElementTypeIfArray?…`
  already absorbs the same operand), keeping only the graceful
  `buildErrorExpression` fallback — total −7 LoC on `fir-jvm`.
  **§3.12-D1 / D2** — verified already at HEAD's minimal shape (the
  `null →` arm is the 22-line trivial path serving the live
  `JTC_NULL_PROJ_BUILD` (5 hits) and `JTC_NULL_PROJ_LOWER` (155 hits)
  paths; the raw-detection `else` clause on the `JavaClassifierType ->`
  block is already gone — "pre-landed", no further reduction possible
  without breaking those live paths). **§3.2** (relocate
  `directSupertypeClassIds` cache into a `FirSessionComponent`) and
  **§3.3 option 2** (java-direct-private
  `JavaClassifierTypeWithContainingClassIds` subinterface) were
  intentionally **not** pursued — both are flagged in §4 of the
  analysis doc as net codebase washes worth doing only if the project
  explicitly wants to tighten the FIR-jvm / java-direct boundary.
  Verification: `./gradlew :compiler:fir:fir-jvm:compileKotlin
  :compiler:java-direct:compileKotlin` exit 0; repo-wide
  `search_contents_by_grep` for `JavaTypeParameterWithFirSymbol`
  returns no remaining `.kt` / `.java` references (only documentation
  mentions in the analysis and prior JTC docs); `git diff --stat`
  shows the four-file change set with net `29 insertions(+),
  61 deletions(-)`. Net realised saving on `fir-jvm`: **≈ −26 LoC**
  (`MutableJavaTypeParameterStack.kt` −19, `javaAnnotationsMapping.kt`
  −7) on top of the already-landed D1/D2 reductions that the
  pre-existing 2026-05-24 D1+D2+D3 entry counted as still-pending.
  The `fir-jvm` diff vs `ff12cbb3` therefore shrinks from `+397 / −53`
  to approximately `+371 / −53`. Java-direct module side: −5 LoC
  + 12-line KDoc refresh in `FirBackedJavaClassAdapter.kt`,
  comment-only refresh in `JavaTypeOverAst.kt` (net 0). Tests were
  not re-run in this session beyond compile-only verification — the
  user's explicit broader-corpus safety statement was the gating for
  landing §3.4 without rerunning the 2793-test `JavaUsingAst*` suite.
  The analysis doc was extended with §8 "Landed minimisation wave"
  capturing the per-item action table and the deferral notes for
  §3.2 / §3.3.

- **2026-05-25** — `JavaModelExtensions.kt` retired entirely. Same
  critical-analysis lens that landed the γ TYPE_USE relocation
  earlier in the day was applied to the file's remaining two
  callback interfaces — `JavaFieldWithExternalInitializerResolution`
  and `JavaEnumValueAnnotationArgumentWithConstFallback` — and both
  premises held: the FIR-side helpers behind each callback
  (`resolveExternalFieldValue` + 3 helpers in `FirJavaFacade.kt`;
  `resolveConstFieldValue` + `extractEvaluatedConstValue` +
  `tryExtractConstantValue` in `javaAnnotationsMapping.kt`) only
  needed `FirSession.symbolProvider`, which `JavaResolutionContext`
  already carries. Both helpers were relocated into a new
  java-direct file
  (`compiler/java-direct/src/.../resolution/JavaExternalConstResolver.kt`,
  185 lines) hosting `FirSession.resolveExternalFieldValue` and
  `FirSession.resolveConstFieldValue` plus the const-extraction
  primitives, reshaped to use `getClassDeclaredPropertySymbols`
  rather than `firClass.declarations` to avoid needing
  `DirectDeclarationsAccess` opt-in. `JavaResolutionContext` got two
  thin wrappers (`resolveExternalFieldValue(classQualifier, fieldName)`,
  `resolveConstFieldValue(classId, fieldName)`); both fall through
  cleanly to `null` when `nullableSymbolProvider == null` (parsing-
  only fixtures), so the three
  `JavaParsingAnnotationsTest.testEnumValueArgument*` tests pass
  unchanged. `JavaFieldOverAst.initializerValue` now calls the
  external resolver inline (the
  `JavaFieldWithExternalInitializerResolution` callback override
  was deleted). `JavaAnnotationOverAst.createAnnotationArgumentFromValue`'s
  `REFERENCE_EXPRESSION` arm now performs the const-vs-enum
  disambiguation at model-construction time, emitting
  `JavaLiteralAnnotationArgumentOverAst` when the reference resolves
  to a `const val` and falling back to
  `JavaEnumValueAnnotationArgumentOverAst` otherwise (matching
  PSI/javac-wrapper structure-build-time behaviour). FIR-side
  cleanup: `FirJavaFacade.kt`'s `lazyInitializer` collapses to a
  single `javaField.initializerValue?.createConstantIfAny(session)`
  read (−67 lines on the file);
  `javaAnnotationsMapping.kt`'s enum-value arm drops the cast +
  `resolveConstFieldValue` chain and 3 now-orphaned helpers
  (−66 lines on the file). `JavaModelExtensions.kt` deleted
  outright (−73 lines). Suite results:
  `:compiler:java-direct:test --tests "JavaUsingAst{Phased,Box}TestGenerated"`
  = **2793/2793 green** (`BUILD SUCCESSFUL in 41s`);
  `:compiler:java-direct:test --tests "JavaParsing*Test"` = green;
  `:compiler:fir:analysis-tests:test --tests "PhasedJvmDiagnosticLightTreeTestGenerated.*" --rerun`
  = 0 new failures (`BUILD SUCCESSFUL in 1m 11s`). Cumulative
  2026-05-25 file-size deltas after both cleanups:
  `JavaTypeConversion.kt` 707 → 546, `FirJavaFacade.kt` ≈838 → 771,
  `javaAnnotationsMapping.kt` ≈524 → 458,
  `JavaModelExtensions.kt` 73 → **deleted**,
  `JavaResolutionContext.kt` ≈715 → 761,
  `JavaModelSessionAccess.kt` 79 → 175;
  new file `JavaExternalConstResolver.kt` 185 lines. FIR-jvm module
  net ≈−367 LoC; java-direct module net ≈+337 LoC;
  **codebase net ≈−30 LoC plus one deleted file plus three retired
  callback interfaces**. The public Java-model interface surface in
  `core/compiler.common.jvm/.../structure/*` is back to its
  pre-java-direct shape; rule 7 of `AGENT_INSTRUCTIONS.md` is
  satisfied; the file that *named* the entire model→FIR-callback
  bridge pattern is gone. Outstanding items: doc updates in
  `TYPE_USE_ANNOTATION_HANDLING_2026_05_04.md` §3-5,
  `INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md` §3, and
  `FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` §11-12 — all three
  still describe the retired callbacks as load-bearing.

- **2026-05-25** — Category γ (TYPE_USE annotation filtering) relocated
  from FIR-jvm to java-direct. The "Critical analysis (2026-05-25)"
  section of `implDocs/JTC_CLEANUP_2026_05_24.md` empirically falsified
  the `TYPE_USE_ANNOTATION_HANDLING_2026_05_04.md` §5 claim that the
  filter cannot move into java-direct without breaching Architecture
  Decision #1 — `JavaResolutionContext` has already carried a
  `FirSession` end-to-end since Step 4.5a (`CompilationUnitContext.kt:21`),
  and `JavaAnnotationOverAst.classId` already resolves through it. The
  cleanup deletes `filterTypeUseAnnotationsIfNeeded`,
  `isTypeUseAnnotationClass`, `hasTypeUseTarget`, `isTypeUseElement`,
  the `additionalTypeUseAnnotations` defensive filter, and the
  `JavaTypeWithExternalAnnotationFiltering` interface (≈161 lines on
  `JavaTypeConversion.kt`, 16 lines on `JavaModelExtensions.kt`).
  Replacement lives on the java-direct side: a new
  `JavaModelTypeUseClassIdCache : FirSessionComponent` (in
  `JavaModelSessionAccess.kt`) backs a `ConcurrentHashMap<ClassId, Boolean>`
  cache keyed off `ClassId` (no FQN→`ClassId` re-probe, no cross-package
  PSI-fallback guard); `FirSession.isTypeUseAnnotationClass(classId)`
  +`computeIsTypeUseAnnotationClass`/`hasTypeUseTarget`/`isTypeUseElement`
  port the `@Target` walk into the same file. `JavaResolutionContext`
  exposes the helper to the model side; `JavaTypeOverAst.annotations`
  now pre-filters `memberAnnotations` lazily through it. Registration
  hooks into `JavaClassFinderOverAstImpl.init` alongside the existing
  `registerJavaModelInFlightResolutionsIfAbsent`. The
  `needsTypeUseAnnotationFiltering` perf gate is gone: PSI's hot path
  carries no closure anymore (the call site reads `type.annotations`
  directly), and the per-`ClassId` cache amortises the symbol lookup
  on the java-direct side. **One parsing-level test
  (`JavaParsingMembersTest.testVarargsParameterType`) was updated**:
  its old contract — `JavaTypeOverAst.annotations` includes unfiltered
  member annotations — no longer holds in dummy-session parsing mode
  (the new pre-filter calls into `cycleSafeClassLikeSymbol`, which
  returns null without a `FirSymbolProvider`, so TYPE_USE-ness is
  conservatively `false`). The annotation is still parsed and captured
  on the parameter (`regularParam.annotations` / `varargParam.annotations`)
  — the test asserts that directly now, and the end-to-end propagation
  contract is covered by the `JavaUsingAst*` integration suite. Two
  obsolete test snippets in `JavaParsingAnnotationsTest.kt` that
  exercised the retired `filterTypeUseAnnotations` callback were
  dropped at the same time. Suite results:
  `:compiler:java-direct:test --tests "JavaUsingAst{Phased,Box}TestGenerated"`
  = **2793/2793 green** (`BUILD SUCCESSFUL in 42s`);
  `:compiler:java-direct:test --tests "JavaParsing*Test"` = green
  (`BUILD SUCCESSFUL in 1m 33s`, 0 failures);
  `:compiler:fir:analysis-tests:test --tests "PhasedJvmDiagnosticLightTreeTestGenerated.*" --rerun`
  = **0 new failures** (`BUILD SUCCESSFUL in 1m 16s`). Net file deltas:
  `JavaTypeConversion.kt` 707 → **546** (net debt vs pre-java-direct
  cut from +423 to +262), `JavaModelExtensions.kt` 73 → 57,
  `JavaModelSessionAccess.kt` 79 → 175, `JavaResolutionContext.kt`
  +12, `JavaTypeOverAst.kt` net +5, `JavaClassFinderOverAstImpl.kt`
  +5. Codebase net: ≈**−74 LoC** (plus the doc refresh in
  `implDocs/JTC_CLEANUP_2026_05_24.md` "Post-cleanup section
  (2026-05-25)"). Follow-up items: perf re-measurement against
  `KotlinFullPipelineTestsGenerated` (to confirm the
  `needsTypeUseAnnotationFiltering`-gate-motivating regression cannot
  re-fire under the cache); same critical-analysis lens for the other
  two callbacks in `JavaModelExtensions.kt`
  (`JavaFieldWithExternalInitializerResolution`,
  `JavaEnumValueAnnotationArgumentWithConstFallback`) — if both
  relocatable, the whole file can be deleted next iteration. Doc-level
  obsolescence: `TYPE_USE_ANNOTATION_HANDLING_2026_05_04.md` §3-5 still
  treat the FIR-side filter as load-bearing; flagged for revision.

- **2026-05-24** — D1+D2+D3 cleanups in `JavaTypeConversion.kt` based on a
  sub-block empirical probe (16 markers across the file, full java-direct
  suite). Removed empirically dead code: ~37 lines of expanded null-branch
  machinery (`mapJavaToKotlin`/`readOnlyToMutable`/`outerTypeArgs`/
  `isRawType`/raw projection — all 0 hits / 2793 tests), ~18 lines of
  raw-type detection on `JavaClassifierType` block (`hasTypeParams=true`
  case — 0 hits), and inlined the `JavaTypeParameterWithFirSymbol` shortcut
  on the `JavaTypeParameter ->` branch (0 hits despite
  `FirBackedJavaTypeParameter` being in production). Net: -71 lines on
  `JavaTypeConversion.kt`. java-direct suite + PSI regression green.
  **Validation pending against `KotlinFullPipelineTestsGenerated` /
  `IntelliJFullPipelineTestsGenerated`** — if those corpora exercise the
  removed sub-blocks, revert is required. Full sub-block hit table in
  `implDocs/JTC_CLEANUP_2026_05_24.md`.

- **2026-05-24** — D2-A: synthetic supertypes (`java.lang.Object`,
  `java.lang.annotation.Annotation`, `java.lang.Enum<E>`) now resolve
  `classifier` model-side via `JavaResolutionContext` + `FirBackedJavaClassAdapter`.
  `SimpleClassifierType` and `EnumSupertypeForJavaDirect` now take a
  resolution context and lazy-resolve. Empirically, the
  `JavaTypeConversion.kt`'s `null ->` branch goes from **5013 hits / 2793 tests**
  (pre-D2-A) to **178 hits / 2793 tests** (~28× reduction). Synth supertypes
  fully eliminated from the null path; residual hits are JLS-misses on
  `JavaClassifierTypeOverAst` and binary-classpath misses on
  `PlainJavaClassifierType` (PSI-era binary code path, out of java-direct
  scope).

- **2026-05-24** — Implicit-permits sealed-class resolution moved into the
  model. `JavaClassOverAst.deriveImplicitPermittedTypes` now wraps the
  resolved nested `JavaClassOverAst` in a new `ResolvedJavaClassifierType`
  (`classifier` is the real `JavaClass`), so `FirJavaFacade`'s
  `setSealedClassInheritors` `classifier == null` fallback is no longer
  reached for implicit-permits Java sealed classes. The fallback was
  empirically the **only** live driver of the FIR null-branch in
  `setSealedClassInheritors` post-Step-4.5c — that branch is now deleted.
  Explicit cross-file `permits` already routed through `FirBackedJavaClassAdapter`
  via Step 4.5c; this iteration closes the implicit-permits gap.

- **2026-05-20** — Lombok-plugin compatibility with `java-direct`. Two fixes:
  1. **`JavaImportResolver.extractFragmentedImports`** — recover single-segment
     star imports (`import lombok.*;`) when the parser fragments the file root
     because of leading `// FILE: …` comments (Lombok testdata pattern). Previous
     guard required a dot in the recovered FQN and silently dropped `lombok`.
  2. **`JavaField.hasInitializer`** added to the public interface (rule §7
     exception — see [`AGENT_INSTRUCTIONS.md`](AGENT_INSTRUCTIONS.md)). Required
     because the Lombok K2 generators (`AllArgsConstructorGeneratorPart`,
     `RequiredArgsConstructorGeneratorPart`) previously cast `source.psi as PsiField`
     to call `hasInitializer()`, returning `false` for any non-PSI Java-model
     impl (including `JavaClassOverAst`). The cast was itself a PSI leak in the
     K2 plugin — net debt **reduction**, not an addition: PSI is removed from the
     plugin call path. Subinterface-on-`compiler/java-direct/` doesn't fit because
     PSI's `JavaFieldImpl` also has to expose this so PSI-loaded fields keep
     their non-constant-initializer detection. Implementations: PSI
     `JavaFieldImpl`, java-direct `JavaFieldOverAst`, ASM `BinaryJavaField`,
     `javac-wrapper` `TreeBasedField`/`SymbolBasedField`/`MockKotlinField`,
     reflection `ReflectJavaField`. `FirJavaField` gains
     `lazyHasInitializer` / `hasInitializer`; `FirJavaFacade` populates it;
     `SignatureEnhancement` propagates it on copy.

- **2026-05-11** — Cat E ASM `Frame.merge` crashes resolved: traced to
  `JavaFieldOverAst.initializerValue` not coercing the evaluated constant to
  the field's declared primitive type. All 11 java-direct-only IJ FP failures
  now pass.
- **2026-05-08 → 2026-05-10** — IJ FP regression delta cleanup (Cat A-E):
  inherited-nested-class lookup over binary supertypes, private interface
  methods, Scala companion-module `$` filter, qualified raw-form nested
  classes, cross-language `ConstantEvaluator`, star-imported binary
  supertypes, `@NotNull T[]` double application, and nested-class
  explicit-import `ClassId` splitting.
- **2026-05-08** — `LazySessionAccess` re-entrance guard (KT-74097 / same-thread
  `PUBLICATION` lazy re-entrance), `extractStaticImports` parser-shape fix,
  nested-record implicit `static` (JLS §8.10.3).
- **2026-05-06 → 2026-05-07** — Step 4.5a-c of
  `implDocs/FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md`: public Java-model
  interface rollback completed (`resolve(...)`, `resolveAnnotation(...)`,
  `resolveEnumClass(...)`, `containingClassIds`, `isResolved` deleted from
  `core/compiler.common.jvm/.../structure/`).
- **2026-05-04 → 2026-05-05** — Merged refactoring plan landed (PSI removal
  × resolver unification, Stages 1-4); `BinaryJavaClassFinder` follow-ups.
- **2026-04-28 → 2026-04-30** — Test framework wiring fix; PSI-removal Phase 1
  (`BinaryJavaClassFinder` behind `kotlin.javaDirect.useBinaryClassFinder`
  flag, default-OFF in production); shared-FIR PSI-path regression gating.

For full root-cause analyses, fixes, and test results, see
`implDocs/archive/ITERATION_RESULTS_2026_05_11.md`.

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

## `testJavaSrcWrongPackage` `.out` update under unconditional `java-direct` — 2026-05-25

### Overview

Shared CLI diagnostic test
`org.jetbrains.kotlin.cli.CliTestGenerated.DiagnosticTests.testJavaSrcWrongPackage`
had been failing on `rr/ic/direct-java` since `JvmFrontendPipelinePhase`
started installing `java-direct` unconditionally for every source
session. The fixture places `A.java` declaring `package foo;`
physically at the source root (not under `foo/`) and a Kotlin file
referencing bare `A`. Under PSI, the layout produced a
self-inconsistent two-error diagnostic chain (the indexer hands back
`<root>.A` from `JvmDependenciesIndex` but the resulting `PsiClass`
reports `qualifiedName = foo.A`). Under `java-direct`, the fixture
fails the package/directory consistency check from
`JavaPackageIndexer.indexPackageFromDirectories` (which mirrors
`javac`) — `A.java` is registered under its declared package `foo`
but **not** under `<root>`, so the `.kt`'s bare `A` produces two
`unresolved reference 'A'` errors. The fix is a pure test-expectation
update (`.out` rewritten); the new diagnostic is also a cleaner
cause-of-failure shape than the legacy diagnostic it replaces.

### Investigation summary

The failure mode reflects a long-standing asymmetry between two
layers of the PSI-based Java loader:

| Layer | What it does | Disagrees on |
|---|---|---|
| `KotlinCliJavaFileManagerImpl.findVirtualFileForTopLevelClass` (via `JvmDependenciesIndex`) | Indexes every `.java` file by *physical path* | `A.java` at `<root>/` is registered under `<root>.A`. |
| `PsiJavaFile.getPackageName` → `PsiClass.qualifiedName` | Reads the *declared* `package` statement | The same file's `PsiClass` self-reports `foo.A`. |

K2 asks for `<root>.A`, gets back the `A.java` `VirtualFile`,
materialises a `PsiClass` whose `qualifiedName` is `foo.A`, cannot
reconcile the two, and emits `RETURN_TYPE_MISMATCH` +
`CANNOT_ACCESS_CLASS`.

`java-direct` deliberately does not replicate that split. The
authoritative invariant lives at
`compiler/java-direct/src/.../JavaPackageIndexer.kt:172–176`:

```kotlin
/**
 * Indexes a single package by scanning its directory in each source root.
 * Files with mismatched package/directory are skipped, matching javac behavior.
 */
private fun indexPackageFromDirectories(packageFqName: FqName): Map<String, List<FileEntry>> { … }
```

A softening of the rule lives at `JavaPackageIndexer.kt:98–110` (the
dir-roots-only hoist), which registers top-level `.java` files of a
directory root under their **declared** package — making `foo.A`
discoverable. It does **not** make `<root>.A` discoverable, which is
the lookup that the `.kt`'s bare `A` needs. The unresolved-reference
diagnostic is therefore by design.

### Changes

The only file changed is the expected output:

```diff
--- a/compiler/testData/cli/jvm/diagnosticTests/javaSrcWrongPackage.out
+++ b/compiler/testData/cli/jvm/diagnosticTests/javaSrcWrongPackage.out
-compiler/testData/cli/jvm/diagnosticTests/javaSrcWrongPackage.kt:1:24: error: return type mismatch: expected '<root>.A.Nested', actual 'foo.A.Nested!'.
+compiler/testData/cli/jvm/diagnosticTests/javaSrcWrongPackage.kt:1:13: error: unresolved reference 'A'.
 fun test(): A.Nested = A().nested()
-                       ^^^^^^^^^^^^
-compiler/testData/cli/jvm/diagnosticTests/javaSrcWrongPackage.kt:1:28: error: cannot access class 'foo.A.Nested'. Check your module classpath for missing or conflicting dependencies.
+            ^
+compiler/testData/cli/jvm/diagnosticTests/javaSrcWrongPackage.kt:1:24: error: unresolved reference 'A'.
 fun test(): A.Nested = A().nested()
-                           ^^^^^^
+                       ^
 COMPILATION_ERROR
```

No production source change. A new analysis doc
`compiler/java-direct/implDocs/JAVA_SRC_WRONG_PACKAGE_2026_05_25.md`
(271 lines) was added capturing the PSI/`java-direct` semantic
divergence, the `JavaPackageIndexer` invariant, the dir-roots-only
hoist subtlety, the rule-§6 exception rationale, and an open backlog
note on whether the fixture should be reshaped to make its intent
explicit (or replaced by a fixture that triggers a genuine
cross-language FQN mismatch through a path surviving `javac`'s
rules — e.g. two source roots, one with `<root>/A.java` declaring
`<root>` and another with `foo/A.java` declaring `foo`, then a Kotlin
file pinning one of the two FQNs via `import`).

### Test Results

- `:compiler:tests-integration:test --tests
  "org.jetbrains.kotlin.cli.CliTestGenerated\$DiagnosticTests.testJavaSrcWrongPackage"`
  → `BUILD SUCCESSFUL` (was: `1 test completed, 1 failed`).
- Manual compiler invocation against the fixture
  (`dist/kotlinc/bin/kotlinc compiler/testData/cli/jvm/diagnosticTests/javaSrcWrongPackage.kt
  compiler/testData/cli/jvm/diagnosticTests/javaSrcWrongPackage -d $TMP`)
  produced the matching two `unresolved reference 'A'` lines (cols 13
  and 24) modulo the framework's `COMPILATION_ERROR` trailer.

### Files Modified

| File | Change |
|------|--------|
| `compiler/testData/cli/jvm/diagnosticTests/javaSrcWrongPackage.out` | Replaced the two legacy errors (line 1:24 `return type mismatch …` + line 1:28 `cannot access class 'foo.A.Nested'`) with two `unresolved reference 'A'` errors at 1:13 and 1:24. Kept the trailing `COMPILATION_ERROR` line. |
| `compiler/java-direct/implDocs/JAVA_SRC_WRONG_PACKAGE_2026_05_25.md` | New 271-line analysis doc — fixture description, PSI/`java-direct` semantic divergence, `JavaPackageIndexer` invariant + dir-roots-only hoist subtlety, before/after diagnostic comparison, rule-§6 exception rationale, verification record, open backlog note. |

### Key Learnings

- **PSI's `KotlinCliJavaFileManagerImpl` is structurally non-`javac`
  for misplaced-package layouts.** The split between
  `JvmDependenciesIndex`'s physical-path indexing and
  `PsiClass.qualifiedName`'s content-driven FQN computation lets a
  file at `<root>/A.java` declaring `package foo;` appear under *both*
  `<root>.A` *and* `foo.A` — `<root>.A` from the disk index,
  `foo.A` from the parsed file. Any test asserting a specific
  diagnostic on that layout is implicitly asserting the PSI
  implementation quirk, not a Kotlin language contract.
  `java-direct`'s `JavaPackageIndexer` makes the consistent choice
  (register under the declared package only), which is also what
  `javac` does.

- **The dir-roots-only hoist at `JavaPackageIndexer.kt:98–110` is
  precisely the test-infrastructure shim that keeps non-mirroring
  layouts discoverable** — but only under their *declared* package.
  `foo.A` resolves; `<root>.A` does not. Tests that reference such
  a file must use the declared FQN (`foo.A`) or an explicit
  `import foo.A` rather than the bare top-level name. The fixture
  here predates that invariant and was wired to assert the PSI
  quirk.

- **Rule §6 exception calibration.** `AGENT_INSTRUCTIONS.md` rule §6
  generally forbids test-data updates to make `java-direct` tests
  pass; the exception applies when (a) the fixture is a shared
  upstream test (not `java-direct`'s own corpus), (b) the new
  behaviour is documented design intent (here:
  `JavaPackageIndexer.kt:174` comment + the explicit `javac` parity
  goal), and (c) the test contract is preserved (compilation still
  fails; only diagnostic wording / column changes). All three apply
  here; the unmute is safe.

- **Open-question hygiene.** The fixture name (`javaSrcWrongPackage`)
  and the legacy `.out` suggest its original intent was to assert
  *some* failure on a misplaced-package layout — without specifying
  which one. Now that the PSI and `java-direct` paths produce
  different shapes, the fixture's intent is worth pinning down
  explicitly; recorded in `JAVA_SRC_WRONG_PACKAGE_2026_05_25.md` §7
  as a backlog item.

---

## Fresh `fir-jvm` diff audit + §3.4 / §3.14 minimisation wave — 2026-05-25

### Overview

Two-step iteration on top of the already-landed γ TYPE_USE relocation
and `JavaModelExtensions.kt` retirement. **Step 1** (analysis): a
ground-up audit of every line in the `fir-jvm` module changed between
HEAD (`3637c96c96b0`) and base `ff12cbb3d915`, ignoring the conclusions
of prior `JTC_CLEANUP_2026_05_24.md` and `ITERATION_RESULTS.md` entries
and re-deriving the per-cluster justification from first principles.
Result: `implDocs/FIR_JVM_DIFF_ANALYSIS_2026_05_25.md` (776 lines, then
extended to 815 in step 2), enumerating the 11 distinct logical change
clusters in the `+397 / −53` `fir-jvm` diff and grading each by
liveness, rule-§7 status, and rollback feasibility. **Step 2**
(implementation): after the user confirmed broader-corpus validation
of the committed branch had already been done, the realistic §4
minimisation budget was applied — §3.4 interface deletion + §3.14
mechanical cleanup. §3.12-D1 and D2 were verified to already be at
HEAD's minimal shape (the 2026-05-24 D1+D2+D3 cleanup had landed
them); §3.2 cache relocation and §3.3 java-direct-private subinterface
were declined as net codebase washes.

### Investigation summary

Per-cluster grading recorded in `FIR_JVM_DIFF_ANALYSIS_2026_05_25.md`
§3:

| Cluster | LoC | Status |
|---|---:|---|
| `F1` — `FirJavaField.lazyHasInitializer` | +9 | Live consumer (Lombok K2 generators); rule-§7 exception logged 2026-05-20. |
| `C1` — `FirJavaClass.directSupertypeClassIds` cache | +28 | Live consumer (Step 4.5b); cache key correctness verified. |
| `S1` — `MutableJavaTypeParameterStack.containingClassSymbol` | +10 | Live consumer (`FirJavaFacade` `convertJavaClassToFir`). |
| `S2` — `JavaTypeParameterWithFirSymbol` interface | +21 | **Dead post-2026-05-24-D3.** Sole call-site already deleted; interface still implemented by `FirBackedJavaTypeParameter` but never `is`-checked. **Candidate for §3.4 deletion.** |
| `H1`/`H3`/`H4` — source-Java guards in `FirJavaFacade` | +mixed | Live. |
| `H2`/`H5` — enum origin + `lazyHasInitializer` populator | +mixed | Live (paired with `F1`). |
| `J1…J5` — `JavaTypeConversion` deltas | net −large | Already-landed reductions from 2026-05-24 + γ TYPE_USE. |
| `A1` — `javaAnnotationsMapping` graceful enum fallback | +18 | Live happy path (kills original `requireNotNull` crash for KT-47702 static-imported enum constants); **inner `if (fallbackClassId != null)` recompute is structurally dead** — the outer `?:` chain already absorbs it. Plus unused `symbolProvider` import. **Candidate for §3.14 deletion (~−7 LoC).** |

§4 aggregate minimisation budget: ≈ −26 LoC on `fir-jvm` from
probe-gated `S2` + safe mechanical `A1`. The user's broader-corpus
safety guarantee removed the "probe-gated" qualifier on `S2`.

### Changes

**§3.4 — `JavaTypeParameterWithFirSymbol` deletion.**

- `compiler/fir/fir-jvm/src/.../MutableJavaTypeParameterStack.kt`:
  deleted the entire `JavaTypeParameterWithFirSymbol` interface
  declaration (21 lines including KDoc); the trailing blank line of
  the file was also removed for a net `−19 / +0`.
- `compiler/java-direct/src/.../resolution/FirBackedJavaClassAdapter.kt`:
  removed the `org.jetbrains.kotlin.fir.java.JavaTypeParameterWithFirSymbol`
  import; `FirBackedJavaTypeParameter` no longer extends the interface
  — `override val firTypeParameterSymbol: FirTypeParameterSymbol` is
  now just `val firTypeParameterSymbol: FirTypeParameterSymbol` on the
  adapter (kept because the wrapper's `equals` / `hashCode` /
  `toString` and the `computeIsRaw` traversal both still need a
  stable symbol-backed identity, and FIR's
  `outer.typeParameters` lookup needs the wrapper to point back at
  the real `FirTypeParameterSymbol` for `Name` / `bounds` accessors).
  Stale KDoc citing the retired interface was rewritten in the
  surrounding class-level and member-level KDoc blocks.
- `compiler/java-direct/src/.../model/JavaTypeOverAst.kt`: the
  cross-file branch's comment in `computeClassifier()` was rewritten
  to no longer cite `JavaTypeParameterWithFirSymbol` — it now
  truthfully states that the outer-class chain's
  `FirBackedJavaTypeParameter` wrappers are consumed by the
  qualified-form raw-detection walk in `computeIsRaw` for counts only;
  FIR's own `is JavaTypeParameter ->` branch in `JavaTypeConversion`
  is never reached for them under the model's resolver invariants
  (the stack-lookup fallback there would not find them anyway).

**§3.14 — `javaAnnotationsMapping.kt` mechanical cleanup.**

- Removed unused
  `import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider`.
- Collapsed the structurally-dead `if (fallbackClassId != null) { … }`
  inner branch inside the `JavaEnumValueAnnotationArgument →` arm.
  The outer expression
  `val fallbackClassId = expectedArrayElementTypeIfArray?.lowerBoundIfFlexible()?.classId`
  was already used by the outer fallback chain; the inner `if`
  re-tested it and rebuilt an `enumEntryDeserializedAccessExpression`
  that is functionally identical to the now-already-attempted upper
  branch's `enumClassId ?: …` shape. Kept only the graceful
  `buildErrorExpression` arm with the existing
  `ConeSimpleDiagnostic("Cannot resolve enum annotation argument: …",
  DiagnosticKind.Java)` payload — preserving the
  `requireNotNull`-replacement behavior the cluster was introduced
  for. Net `−7 / +0` on the file.

**§3.12-D1 / D2 — pre-landed.**

Verified during step 1 that the `null →` arm in
`JavaTypeConversion.kt` is already the 22-line trivial path
(`resolveTypeName` + `constructClassType` + the live
`JTC_NULL_PROJ_BUILD` (5 hits) and `JTC_NULL_PROJ_LOWER` (155 hits)
paths) and that the raw-detection `else` clause on the
`JavaClassifierType ->` block is already gone. The 2026-05-24
D1+D2+D3 cleanup had landed both; the `FIR_JVM_DIFF_ANALYSIS` doc
was updated in §8 to record this as "pre-landed".

**§3.2 / §3.3 — declined.**

`directSupertypeClassIds` cache relocation to a `FirSessionComponent`
(§3.2) and a java-direct-private
`JavaClassifierTypeWithContainingClassIds` subinterface (§3.3 option 2)
are both net codebase washes: each one shifts ~25 LoC from `fir-jvm`
into `java-direct` while complicating the call surface. §4 of the
analysis doc flags them as "only worth doing if the project explicitly
wants to tighten the FIR-jvm / java-direct boundary". Both deferred
pending a scoped boundary-tightening effort.

### Test Results

- **Compile-only verification:**
  `./gradlew :compiler:fir:fir-jvm:compileKotlin :compiler:java-direct:compileKotlin`
  → exit 0.
- **Repo-wide reference check:** `search_contents_by_grep` for
  `JavaTypeParameterWithFirSymbol` against `*.kt` / `*.java` returns
  no remaining code references; only `.md` mentions in the analysis
  doc, the prior `JTC_CLEANUP_2026_05_24.md`, and historical
  iteration-results entries.
- **Suite re-run:** `JavaUsingAst{Phased,Box}TestGenerated` and the
  `PhasedJvmDiagnosticLightTreeTestGenerated` PSI regression gate
  were **not** re-run in this session. The user's explicit statement
  that "I already checked the committed changes against broader
  corpus, so we can assume that commited variant is safe" was the
  gating for landing §3.4 without re-running the 2793-test suite —
  every mutation in this iteration is a strict-subset deletion of
  HEAD code (no behavioral additions), so the gating broader-corpus
  result transitively applies.

### Files Modified

| File | Change |
|------|--------|
| `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/MutableJavaTypeParameterStack.kt` | Deleted `JavaTypeParameterWithFirSymbol` interface (21 lines including KDoc) and trailing blank line. Net −19 LoC. |
| `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/javaAnnotationsMapping.kt` | Removed unused `symbolProvider` import; collapsed structurally-dead `if (fallbackClassId != null)` inner branch in `JavaEnumValueAnnotationArgument →` arm; kept graceful `buildErrorExpression` fallback. Net −7 LoC. |
| `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/resolution/FirBackedJavaClassAdapter.kt` | Dropped supertype `JavaTypeParameterWithFirSymbol` and its import from `FirBackedJavaTypeParameter`; kept `firTypeParameterSymbol` as plain `internal val` for the adapter's own identity. Refreshed class-level and member-level KDoc no longer citing the retired interface. Net −5 LoC. |
| `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/model/JavaTypeOverAst.kt` | Rewrote the cross-file-branch comment in `computeClassifier()` to no longer cite `JavaTypeParameterWithFirSymbol`. Comment-only; net 0. |
| `compiler/java-direct/implDocs/FIR_JVM_DIFF_ANALYSIS_2026_05_25.md` | Created in step 1 (776 lines). Extended in step 2 with §8 "Landed minimisation wave" recording the per-item action table and the §3.2 / §3.3 deferral notes (+39 lines, total 815). |

Net `git diff --stat`: 4 source files changed, 29 insertions(+), 61
deletions(-) — plus the new + extended analysis doc.

`fir-jvm` diff vs `ff12cbb3` shrinks from `+397 / −53` to approximately
`+371 / −53`. Java-direct side: −5 LoC on
`FirBackedJavaClassAdapter.kt`, comment-only refresh in
`JavaTypeOverAst.kt`.

### Key Learnings

- **Fresh diff audits surface stale "still load-bearing" claims that
  prior iteration docs cement in place.** The §3.4
  `JavaTypeParameterWithFirSymbol` interface had been carried in
  `fir-jvm` since the original callback-based resolution era; the
  2026-05-24 D3 entry inlined its sole call site but explicitly left
  the interface in place ("so java-direct's `FirBackedJavaTypeParameter`
  implementer continues to type-check"). A fresh audit ignoring that
  decision discovered that `FirBackedJavaTypeParameter` does not
  actually *need* the supertype — its `firTypeParameterSymbol` field
  is used by the adapter's own identity / equality / traversal code,
  not by any `is JavaTypeParameterWithFirSymbol →` test in FIR. The
  interface was dead the moment D3 inlined the call site; the prior
  doc preserved a now-vestigial abstraction.

- **"Probe-gated" can mean "broader-corpus-gated", not
  "instrumentation-gated".** The analysis doc's §4 budget marked
  §3.4 as "probe-gated" pending an instrumentation rerun against
  `KotlinFullPipelineTestsGenerated` /
  `IntelliJFullPipelineTestsGenerated`. The user shortcut the rerun
  by stating that the *committed* HEAD had already been validated
  against those corpora — and since §3.4 is a strict deletion of code
  that the committed HEAD never executes (the call site was inlined
  on 2026-05-24-D3), the broader-corpus result transitively applies.
  This is a recurring pattern: if the deletion target is empirically
  dead at HEAD and HEAD is broader-corpus-clean, no fresh probe is
  required.

- **Comment-only KDoc decay is a code smell signaling deeper dead
  abstractions.** The KDoc on `FirBackedJavaTypeParameter` and the
  cross-file branch of `JavaClassifierTypeOverAst.computeClassifier`
  both cited `JavaTypeParameterWithFirSymbol` as the consumer that
  motivated the wrapper. The fact that both KDocs had to be updated
  in this iteration to *truthfully* describe the live consumer
  (qualified-form raw-detection walk in `computeIsRaw` reading
  `outer.typeParameters` for counts) shows the abstraction had been
  detached from its real consumer for some time. Fresh audits should
  cross-check KDoc claims against actual call-site inventories.

- **"Structurally-dead inner branch" is a recurring `javac`-grade
  cleanup category.** §3.14's inner `if (fallbackClassId != null)`
  recompute is the second instance this week of an inner branch
  whose condition is already absorbed by a containing `?:` chain
  (the first was D1's `null →` arm's `mapJavaToKotlin` inner
  recompute). These are not detected by Kotlin's "unreachable code"
  or by IntelliJ's "redundant null check" inspections because the
  inner branch was *originally* live — it became dead when the outer
  chain absorbed it during a later refactor. Periodic fresh audits
  catch these; the inspections do not.

---

## D1+D2+D3 cleanups in `JavaTypeConversion.kt` — empirically dead sub-blocks deleted — 2026-05-24

### Overview

Sub-block empirical probe (16 markers across `JavaTypeConversion.kt`, full
java-direct suite) revealed that several sub-blocks added during the
java-direct effort were never reached in the `JavaUsingAst*` corpus
post-D2-A. Removed three categories of dead code from the file:

- **D1** — expanded `null ->` branch sub-blocks (`mapJavaToKotlin{,IncludingClassMapping}`,
  `readOnlyToMutable`, `typeParameterSymbols` load, `outerTypeArgs`
  recovery, `isRawType` computation, RAW projection arm, OUTER projection
  arm). All seven probed markers showed **0 hits** in 2793 tests. Replaced
  with the minimal `resolveTypeName → constructClassType` shape that
  receives 100% of the empirical traffic (160 live hits).
- **D2** — raw-type detection `else` clause on the `JavaClassifierType`
  block (`isRawType = isRaw || run { … hasTypeParams … }`). The `run`
  clause produced `hasTypeParams = true` **0 times** in 2793 tests.
  Reduced to `if (!isRaw && classifier?.isTriviallyFlexible() == true)`.
- **D3** — inlined `JavaTypeParameterWithFirSymbol` shortcut on the
  `JavaTypeParameter ->` branch. The shortcut fired **0 times** despite
  `FirBackedJavaTypeParameter` being a real, in-production implementer.
  Replaced `(classifier as? JavaTypeParameterWithFirSymbol)?.firTypeParameterSymbol ?: javaTypeParameterStack[classifier]`
  with `javaTypeParameterStack[classifier]`.

Net: `JavaTypeConversion.kt` 707 → 636 lines (-71). The
`JavaTypeParameterWithFirSymbol` interface itself is left in place
(`compiler/fir/fir-jvm/src/.../MutableJavaTypeParameterStack.kt`) so
java-direct's `FirBackedJavaTypeParameter` implementer continues to
type-check; the inlined call site no longer special-cases it.

### Empirical justification

Sub-block hit counts (full `JavaUsingAstPhasedTestGenerated` +
`JavaUsingAstBoxTestGenerated` run, 2793 tests):

| Sub-block (deleted) | Hits |
|---|---:|
| `JTC_RAW_DETECT_HIT` (raw detection on `JavaClassifierType` block) | 0 |
| `JTC_RAW_OUTER_SAVE_HIT` (raw-detection outer-args save) | 0 |
| `JTC_JTP_FIRSYM_HIT` (JavaTypeParameter shortcut) | 0 |
| `JTC_NULL_MAP_HIT` (null-branch mapJavaToKotlin) | 0 |
| `JTC_NULL_ROM_HIT` (null-branch readOnlyToMutable) | 0 |
| `JTC_NULL_OUTER_HIT` (null-branch outerTypeArgs) | 0 |
| `JTC_NULL_RAW_HIT` (null-branch isRawType=true) | 0 |
| `JTC_NULL_PROJ_OUTER` (null-branch OUTER projection arm) | 0 |
| `JTC_NULL_PROJ_RAW` (null-branch RAW projection arm) | 0 |

Sub-blocks kept (still live):

| Sub-block (kept) | Hits |
|---|---:|
| `JTC_TYPEUSE_OPT_HIT` (TYPE_USE filter opt-in) | 11841 |
| `JTC_JTP_STACK_HIT` (JavaTypeParameter stack lookup) | 47253 |
| `JTC_EMPTY_ATTRS_HIT` (empty-attrs short-circuit) | 2837 |
| `JTC_NULL_PROJ_LOWER` (null-branch lowerBound projection) | 155 |
| `JTC_NULL_PROJ_BUILD` (null-branch buildTypeProjections) | 5 |
| `JTC_TRUNC_HIT` (wrong-arity truncation) | 4 |
| `JTC_JC_OUTER_HIT` (JavaClass-branch outerTypeArgs) | 2 |

Full probe methodology and revised cleanup-floor analysis in
`implDocs/JTC_CLEANUP_2026_05_24.md`.

### Risks / validation pending

The probe corpus is `JavaUsingAst*` (2793 tests). The removed sub-blocks
**may fire on broader corpora**:

- `KotlinFullPipelineTestsGenerated` (414 modules, 109 with Java sources)
- `IntelliJFullPipelineTestsGenerated` (446 modules, Java-heavy)
- `PhasedJvmDiagnosticLightTreeTestGenerated.*` PSI regression suite —
  already green post-cleanup, but probes the PSI path, not java-direct's
  null-classifier sub-blocks specifically.

If broader-corpus runs surface any of the deleted scenarios — most
plausible: a Java source class with a bare reference to a raw Kotlin
collection like `List` where the bare name resolves to `java.util.List`
and gets read-only-mapped to `kotlin.collections.List` — revert is
required per `AGENT_INSTRUCTIONS.md` "any regression → revert".

### Test Results

- `JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated`:
  **2793 / 2793 green** (`BUILD SUCCESSFUL in 43s`, 0 failures).
- `PhasedJvmDiagnosticLightTreeTestGenerated.*` (PSI regression gate):
  green.

### Files Modified

| File | Change |
|------|--------|
| `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt` | -71 / +0 net (94 deletions, 23 insertions for replacement min-shape + comments). `null ->` branch reduced from ~70 lines to ~13. Raw-type detection in `JavaClassifierType` block reduced from ~18 lines to inline check. `JavaTypeParameter ->` shortcut inlined. |

### Key Learnings

- **Static analysis under-counts dead code.** The prior JTC analysis doc
  (same file, earlier today) marked categories α and β as "required"
  based on code-reading. Sub-block probing showed ~62 of those "required"
  lines are dead in the suite. Static claims must be empirically
  validated before being treated as load-bearing.
- **Per-sub-block markers reveal more than top-level markers.** D2-A's
  earlier probe measured only the top of the `null ->` branch
  (5013 → 178 hits). The follow-up probe split that 178 into 14 internal
  sub-paths and found 12 of them dead. Top-level instrumentation
  characterises *entry* traffic; sub-block instrumentation characterises
  *what the code actually does* with that traffic.
- **`FirBackedJavaTypeParameter` exists in production but never reaches
  `JavaTypeConversion.kt`'s `JavaTypeParameter ->` branch in the suite.**
  Either the cross-file-inner-class scenarios that should produce these
  instances aren't exercised, or those instances reach FIR through a
  different conversion path (the JavaClass-branch outer-args recovery
  may absorb them via `findOuterTypeArgsFromHierarchy`). Verifying
  against IJ FP is required to confirm safe removal of the shortcut.
- **Comments that describe rationale ("required for cyclic type bounds",
  "Step 4.5c adapter architecture") can outlive the code path they
  describe.** Several removed comments cited reverted-prototype
  regressions or specific JLS scenarios that no longer reach the deleted
  branches post-D2-A.

---

## D2-A: synthetic-supertype resolution moved into the model + path B investigation — 2026-05-24

### Overview

Eliminated the `SimpleClassifierType` / `EnumSupertypeForJavaDirect`
contribution to `JavaTypeConversion.kt`'s `null ->` branch by giving both
synthetic types a `JavaResolutionContext` and lazy-resolving their
`classifier` through the same path
`JavaClassifierTypeOverAst.computeClassifier()` uses
(`resolutionContext.resolve(name)` → `classifierAdapterFor(it)` →
`FirBackedJavaClassAdapter`). With the synthetic supertypes resolved
up-front, the null branch traffic from java-direct drops from ~5000
to ~180 hits per full-suite run. The residual hits decompose into two
categories — *path B* (model-side `JavaClassifierTypeOverAst` with all
JLS resolution steps missing) and *path C* (binary-loaded
`PlainJavaClassifierType` with no resolved classifier). Path C is
PSI-era binary code (`frontend.common.jvm/.../structure/impl/classFiles/`)
and out of java-direct's scope; path B is documented in detail below.

### Changes

- `compiler/java-direct/src/.../model/JavaTypeOverAst.kt` — both
  `SimpleClassifierType` and `EnumSupertypeForJavaDirect` now take a
  `JavaResolutionContext` constructor parameter. The `classifier` getter
  is replaced with a `lazy(LazyThreadSafetyMode.PUBLICATION)` delegate
  computing
  `resolutionContext.resolve(classifierQualifiedName)?.let { resolutionContext.classifierAdapterFor(it) }`.
  `classifierAdapterFor` returns a `FirBackedJavaClassAdapter` on any
  session with a `nullableSymbolProvider` (production), `null` on the
  bare-bones parsing-fixture sessions.
- `compiler/java-direct/src/.../model/JavaClassOverAst.kt` — three
  construction sites in `supertypes` pass `memberResolutionContext`:
  `EnumSupertypeForJavaDirect(this)` →
  `EnumSupertypeForJavaDirect(this, memberResolutionContext)`,
  `SimpleClassifierType("java.lang.annotation.Annotation")` →
  `SimpleClassifierType("java.lang.annotation.Annotation", memberResolutionContext)`,
  `SimpleClassifierType("java.lang.Object")` →
  `SimpleClassifierType("java.lang.Object", memberResolutionContext)`.

Net diff: +16 / -6 lines across the two model files. No FIR-side change.

### Empirical verification

Instrumented `JavaTypeConversion.kt:352` (`null ->` branch) with
`System.err.println("JTC2_NULL_BRANCH_HIT: qualified=$qualifiedName classifierType=${this::class.simpleName}")`,
ran the full java-direct suite, then reverted. Pre-D2-A baseline:

| classifierType | Sample qualifiedName | Total hits |
|---|---|---|
| `SimpleClassifierType` | `java.lang.Object`, `java.lang.annotation.Annotation` | ~3000+ |
| `EnumSupertypeForJavaDirect` | `java.lang.Enum` | hundreds |
| `JavaClassifierTypeOverAst` | `T`, `F`, `O`, `Z`, `x`, `A`, `B`, `Bar`, `Int`, `List`, `None`, `Target`, `ObjectAssert`, `java.util.ArrayDeque` | ~50 |
| `PlainJavaClassifierType` | `A`, `Base`, `dep.Callback`, `java.io.PrintWriter`, `java.lang.StackTraceElement`, `test2.Row` | ~50 |
| **Total** | | **5013** |

Post-D2-A:

| classifierType | Total hits |
|---|---|
| `SimpleClassifierType` | **0** |
| `EnumSupertypeForJavaDirect` | **0** |
| `JavaClassifierTypeOverAst` | ~74 |
| `PlainJavaClassifierType` | ~50 |
| **Total** | **178** |

### Test Results

- `JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated`:
  **2793 / 2793 green** (`BUILD SUCCESSFUL in 40s`, 0 failures).
- `PhasedJvmDiagnosticLightTreeTestGenerated.*` PSI regression gate:
  green.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/model/JavaTypeOverAst.kt` | `SimpleClassifierType` and `EnumSupertypeForJavaDirect` accept a `JavaResolutionContext`; lazy-resolve `classifier` via that context. |
| `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/model/JavaClassOverAst.kt` | Three construction sites in `supertypes` pass `memberResolutionContext`. |

### Path B investigation

Path B = `JavaClassifierTypeOverAst.computeClassifier()` returns `null`
in production. A second instrumentation pass added
`System.err.println("JCO_NULL: name=$rawTypeName partsSize=${parts.size}")`
just before the final `return null` in
`JavaTypeOverAst.kt:computeClassifier()` and ran the full suite.

**Twelve distinct `name` values surfaced, all `partsSize=1`:**
`T`, `F`, `O`, `Z`, `B`, `x`, `A` (type-parameter-shaped) and `Bar`,
`Target`, `ObjectAssert`, `Int`, `None`, `List`, `ArrayDeque`
(class-name-shaped). Each test class contributes 1-12 hits; the heaviest
concentrators are
`JavaUsingAstPhasedTestGenerated$Tests$Multiplatform$DirectJavaActualization`,
`…$PlatformTypes$NullabilityWarnings`,
`…$Inference$UpperBounds`,
`…$PlatformTypes$RawTypes`,
`…$TestsWithStdLib$Annotations$ProhibitPositionedArgument`.

The 20 total `JCO_NULL` hits are lower than the ~74 path-B hits at the FIR
`null ->` site because `computeClassifier()` also returns `null` (without
the `JCO_NULL` println) when multi-part navigation through
`findInnerClass(...)` misses an inner name — that branch returns null
**before** reaching the final fallthrough probe. The 74 - 20 ≈ 54
remaining hits land there.

#### Why each category fails

**Category B1 — type-parameter-shaped names (`T`, `F`, `O`, `Z`, `B`, `x`, `A`).**
`JavaScopeForContext.findTypeParameter` returns `typeParametersInScope[name]`,
populated via `withTypeParameters(...)` at member context construction in
`JavaClassOverAst.memberResolutionContext`
(`resolutionContext.withContainingClass(this).withTypeParameters(typeParameters)`).
If the type-parameter scope is empty at construction time of the
`JavaClassifierTypeOverAst` — e.g. the type ref lives in a context the
`memberResolutionContext` chain hasn't enriched yet — `T` falls through.
`findInheritedTypeParameter` (low-priority, outer-class inherited) likewise
reads from a separate `inheritedTypeParametersInScope` map populated via
`withInheritedTypeParameters(...)`. If neither chain ran for this ref's
context, the type-param lookup fails.

**Category B2 — class-name-shaped names (`Bar`, `Target`, `ObjectAssert`, `Int`, `None`, `List`, `ArrayDeque`).**
These are simple references that JLS-style resolution can't see:
- Not in an explicit single-type import.
- Not declared in the containing class's scope (inner / inherited /
  outer-walk / same-file).
- Not in `java.lang`.
- Not in any star-imported package.
- And `resolutionContext.resolve(name)` returns null because none of the
  five JLS steps + `tryResolve` via `FirSession.symbolProvider` succeeds
  at the simple-name granularity. (Conservatively the model does **not**
  iterate every known package to find a class named `Bar`; that's
  FIR's `findClassIdByFqNameString` job.)

For B2, the FIR-side `null ->` branch invokes `findClassIdByFqNameString`
(`JavaTypeConversion.kt:563`), which walks `FirSymbolNamesProvider`'s
known packages and probes each `(packageFqName, name)` split via
`symbolProvider.getClassLikeSymbolByClassId`. This is the same probe
the model can't do without exhaustively scanning all packages — a cost
the model is intentionally not paying on the hot AST classifier path.

#### Solution directions

**Direction B1-fix: enrich the resolution context where it's not enriched today.**

Hypotheses for the leaks:
1. **Static nested class referencing outer's type parameter.** A
   `static class Inner` inherits no type parameters per JLS 8.5.1; but
   when constructing `JavaClassOverAst.memberResolutionContext` for the
   *outer* class's members that mention `Inner.something`, the type
   ref to `Inner` itself might be evaluated through a context chain that
   skipped `withInheritedTypeParameters(...)`. Mostly a hypothesis —
   needs targeted reproduction.
2. **Type ref constructed before the containing class's
   `typeParameters` lazy is materialised.** `JavaClassOverAst.typeParameters`
   is `lazy(LazyThreadSafetyMode.SYNCHRONIZED)` (`JavaClassOverAst.kt:87-91`).
   A type ref evaluated mid-`typeParameters`-compute would observe
   `typeParameters = emptyList()` and `withTypeParameters` would no-op
   (`JavaScopeForContext.kt:85` returns `this` on empty).
3. **Type ref in default annotation argument expressions** —
   `convertJavaAnnotationMethodToValueParameter` /
   `JavaAnnotationOverAst` construction may build types with a context
   that's missing class-level type params.

Recommended next step: re-run the probe with a richer payload — print
the containing class FQName, the
`resolutionContext.containingClass?.fqName`, and the set of
`typeParametersInScope` keys — then bisect the 20 failing test paths to
pin down which scenario each hits. Repro one of the 7 distinct names in
a minimal `// FILE: *.java` block, fix the context wiring,
verify with the suite.

**Direction B2-fix: not recommended in isolation.**

Pushing JLS-conservative resolution past the five steps would mean
duplicating `findClassIdByFqNameString`'s package walk inside
`JavaResolutionContext.resolve(...)`. That defeats the existing
lazy-/cache-friendly contract of the AST classifier path: every
simple-name probe would have to scan the full package list of the
session's `FirSymbolNamesProvider` before being able to say "no". The
FIR-side fallback is the correct location for this work; the model
shouldn't replicate it. The remaining ~74 path-B hits (or whatever a
B1 fix reduces them to) are not a defect — they're the boundary of
JLS-strict model resolution.

**Direction B3 — eliminate `null ->` branch entirely (not recommended now).**

After B1 fix, the only residual sources of `classifier == null` in
java-direct's `JavaClassifierTypeOverAst` would be the genuine JLS-misses
(B2). Combined with binary `PlainJavaClassifierType` misses (path C,
out-of-scope here), the FIR `null ->` branch would still be reached by
~50-130 calls per full-suite run — *not* dead code. Removing it would
regress those calls. The branch's machinery (raw-type detection,
outer-args recovery, `mapJavaToKotlinIncludingClassMapping`) remains
load-bearing. Leave alone.

### Out of scope

- `JavaTypeConversion.kt:163-184` (raw-type detection via
  `classifierQualifiedName` + `resolveTypeName`) — still fires for path B
  and path C; same boundary as the `null ->` branch.
- `findClassIdByFqNameString` (`JavaTypeConversion.kt:563-615`) — symbol
  provider package walk; reachable from path B/C/raw-type detection.

### Key Learnings

- **D2-A's win is concentrated at three names.** `java.lang.Object`,
  `java.lang.annotation.Annotation`, `java.lang.Enum` together account
  for ~97% of pre-D2-A null-branch hits (every Java source class
  produces one and every enum produces an Enum supertype). Resolving
  three names model-side closes the bulk of the null traffic without
  touching FIR.
- **`classifierAdapterFor` is the universal "resolve to adapter" path
  the model now uses uniformly.** D1 plumbed it through
  `JavaClassifierTypeOverAst.computeClassifier()`; this iteration uses
  the same primitive for `SimpleClassifierType` and
  `EnumSupertypeForJavaDirect`. Future synthetic types should follow the
  same shape rather than hardcoding `classifier = null`.
- **`JavaResolutionContext.resolve("java.lang.Enum")` etc. works the
  first time.** No special-casing for "external" JDK classes was needed
  in the synth types — `resolveNestedClassToClassId` correctly probes
  `(pkg=java.lang, class=Enum)` via the FIR symbol provider and
  succeeds for every JDK class on the classpath.
- **Gradle fork stderr aggregation is *per fork*, not per testcase.**
  Probes via `System.err.println` land in whichever XML file the JVM
  fork happened to be writing — not necessarily the XML for the actual
  triggering test. For test-data attribution, instrumentation must
  include identifying context (containing class, FQN) in the payload;
  the XML filename alone is misleading.
- **`partsSize=1` is sufficient to characterise the residual path B
  population.** All 12 distinct names in the residual are single-segment
  (no dots), which matches both B1 (type-param-shaped) and B2 (bare
  simple class names). Multi-part null returns (via the `findInnerClass`
  miss in `computeClassifier`'s mid-function `return null`) account for
  the gap between the 20 `JCO_NULL` hits and the ~74 `JTC2_NULL_BRANCH_HIT`
  hits for `JavaClassifierTypeOverAst` — adding a second print at the
  multi-part branch would expose those cases for a future iteration.

---

## Implicit-permits sealed-class resolution — `FirJavaFacade` null-branch deleted — 2026-05-24

### Overview

Removed the last live consumer of the `classifier == null` fallback in
`FirJavaFacade.createFirJavaClass`'s `setSealedClassInheritors` lambda by
making `JavaClassOverAst.deriveImplicitPermittedTypes` emit a
`JavaClassifierType` whose `classifier` is the already-resolved nested
`JavaClassOverAst`. After Step 4.5c routed explicit cross-file `permits`
through `FirBackedJavaClassAdapter`, an empirical probe of the full
java-direct suite showed every `classifier == null` hit on the
`setSealedClassInheritors` path came from the synthetic
`SimpleClassifierType` produced by `deriveImplicitPermittedTypes` — case 1
in the investigation. Resolving the nested class up-front in the model
turns that case into the non-null `classifier as? JavaClass` branch and
makes the FIR fallback dead.

### Investigation summary

1. **Run 1** — instrumented the `FirJavaFacade.kt` null-branch with
   `System.err.println("JD_NULL_BRANCH_HIT: …")` and ran
   `JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated`
   (2793 tests). Unique hits:
   ```
   sealedClass=/SameFile     qualified=SameFile.A     classifierType=SimpleClassifierType
   sealedClass=/SameFile     qualified=SameFile.B     classifierType=SimpleClassifierType
   sealedClass=/SameFile.B   qualified=SameFile.B.C   classifierType=SimpleClassifierType
   sealedClass=/SameFile.B   qualified=SameFile.B.D   classifierType=SimpleClassifierType
   ```
   100% `SimpleClassifierType`. Zero `JavaClassifierTypeOverAst`.
2. **Run 2** — replaced the entire null-branch with
   `return@mapNotNullTo null`. `BUILD FAILED in 48s`; exactly 2 failures
   (`testJavaSealedClassExhaustiveness`, `testJavaSealedInterfaceExhaustiveness`),
   both implicit-permits scenarios from `javaSealedClassExhaustiveness.kt` /
   `javaSealedInterfaceExhaustiveness.kt`. `sealedJavaCrossFilePermits.kt`
   (explicit cross-file permits) did **not** fail — confirming Step 4.5c's
   adapter covers that path.

Conclusion: case 1 was empirically the only live driver. Cases 2 (bare-bones
session, no `nullableSymbolProvider`) and 3 (`resolutionContext.resolve(...)`
miss) are theoretically reachable but unreached in production.

### Changes

- `compiler/java-direct/src/.../model/JavaTypeOverAst.kt` — added
  `ResolvedJavaClassifierType(resolvedClass: JavaClass)`, mirroring the
  existing `JavaClassifierTypeForEnumEntry` shape: `classifier` returns the
  passed-in `JavaClass` directly, `classifierQualifiedName` reads
  `fqName?.asString() ?: name.asString()`. KDoc cites the
  `setSealedClassInheritors` consumer that requires a non-null
  `classifier`.

- `compiler/java-direct/src/.../model/JavaClassOverAst.kt` —
  `deriveImplicitPermittedTypes` no longer constructs
  `SimpleClassifierType("$myFqName.$innerName")`; instead resolves the
  nested class via the existing `findInnerClass(Name)` API (cached at
  `JavaClassOverAst.kt:133`) and wraps the result in
  `ResolvedJavaClassifierType`. Drop on `findInnerClass` returning null
  (defensive — current `.filter` ensures the inner class extends/implements
  the sealed class so the lookup should always succeed).

- `compiler/fir/fir-jvm/src/.../FirJavaFacade.kt` —
  `setSealedClassInheritors` lambda's else-branch deleted; collapsed to:
  ```kotlin
  val classifier = classifierType.classifier as? JavaClass ?: return@mapNotNullTo null
  JavaToKotlinClassMap.mapJavaToKotlin(classifier.fqName!!) ?: classifier.classId
  ```
  Shared FIR file — PSI regression gate run before merge.

### Test Results

- `JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated`:
  **2793/2793 green** (`BUILD SUCCESSFUL in 48s`, 0 failures).
  Previously-failing tests under deletion (`testJavaSealedClassExhaustiveness`,
  `testJavaSealedInterfaceExhaustiveness`) now pass on top of the model fix.
- `PhasedJvmDiagnosticLightTreeTestGenerated.*` (PSI regression gate):
  green, 0 failures.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/model/JavaTypeOverAst.kt` | Added `ResolvedJavaClassifierType` wrapping a resolved `JavaClass`. |
| `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/model/JavaClassOverAst.kt` | `deriveImplicitPermittedTypes` resolves inner class via `findInnerClass(...)` and wraps in `ResolvedJavaClassifierType`. |
| `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/FirJavaFacade.kt` | `setSealedClassInheritors` null-branch deleted; classifier-null entries dropped via `?: return@mapNotNullTo null`. |

Net diff: +24/-13 lines across the three files.

### Key Learnings

- **The `ITERATION_RESULTS_2026_05_11.md:3769` "regression catcher" claim
  for `sealedJavaCrossFilePermits.kt` was stale post-Step-4.5c.** The doc
  was written before the adapter half of Step 4.5b/c landed. After Step
  4.5c, that test passes regardless of whether the FIR null-branch is
  present — its classifier is no longer null. The implicit-permits cases
  (`SameFile` in `javaSealedClassExhaustiveness.kt` / `…Interface…`) are
  the real regression catchers for any future deletion of the
  `setSealedClassInheritors` fallback.

- **Synthetic `JavaClassifierType` types are the residual classifier=null
  sources in the model.** `SimpleClassifierType` and
  `EnumSupertypeForJavaDirect` hard-code `classifier = null` because they
  have no `JavaResolutionContext` and were intended to be resolved
  FIR-side. They still feed `java.lang.Object` /
  `java.lang.annotation.Annotation` / `java.lang.Enum<E>` synthetic
  supertypes and the now-also-fixed implicit-permits inheritor list. Any
  future iteration that wants to eliminate the `null ->` branch in
  `JavaTypeConversion.kt` (still live for these synthetic supertypes plus
  binary `PlainJavaClassifierType` plus `JavaClassifierTypeOverAst` JLS
  misses) needs to plumb `JavaResolutionContext` into the synthetics or
  introduce a `ResolvedJavaClassifierType`-style wrapper for them.

- **Probing with `System.err.println` + JUnit XML grep is the cheapest way
  to enumerate live consumers of a suspicious branch.** Gradle aggregates
  `system-err` at the fork level (not per-testcase), so XML attribution is
  approximate, but `sort -u` over all `*.xml` matches reveals every
  distinct call shape in one run. Used here twice (once for `FirJavaFacade`,
  once for `JavaTypeConversion`) without modifying test fixtures.

- **Coverage-gap documentation decays.** The 2026-04-28 coverage-gap
  analysis in `implDocs/archive/ITERATION_RESULTS_2026_05_11.md:3733-3838`
  documented `sealedJavaCrossFilePermits.kt` as the canonical regression
  catcher for the FIR null-branch. Subsequent Step-4.5b/c refactors
  re-wrote the resolution path under it without invalidating the doc
  claim. When a refactor changes which scenarios reach a given branch, the
  test-to-branch mapping must be re-checked, not assumed.

---

## `JavaFieldAndKotlinProperty` HeaderMode unmute — 2026-05-20

### Overview

After Stage-1.5 / 1.6 direct-injection wiring (`ac8736eae6a8`,
`JvmFrontendPipelinePhase` unconditionally uses `createJavaDirectSourceJavaFacadeBuilder`),
3 codegen tests in
`compiler/testData/codegen/boxJvm/javaFieldAndKotlinProperty/` started passing in
the `FirLightTreeHeaderModeCodegenTestGenerated.BoxJvm.JavaFieldAndKotlinProperty`
runner. Since they carry an `IGNORE_HEADER_MODE: JVM_IR` mute (KT-56386 — wrong
field access for fields shadowed by Kotlin private properties), the codegen
suppressor reported them as muted-but-passing with
`"Looks like this test can be unmuted. Remove JVM_IR from the IGNORE_HEADER_MODE
directive for FIR for JVM_IR"`.

Affected tests:
- `testJavaFieldKotlinPropertyJavaPackagePrivate`
- `testJavaProtectedFieldAndKotlinInvisibleProperty`
- `testJavaProtectedFieldAndKotlinInvisiblePropertyReference`

### Root cause

`AbstractFirHeaderModeCodegenTestBase` (`compiler/tests-common-new/.../runners/codegen/AbstractFirHeaderModeCodegenTest.kt`)
wires `BlackBoxCodegenSuppressor` with `customIgnoreDirective = IGNORE_HEADER_MODE`
and uses the CLI pipeline (`setupJvmPipelineSteps`). With `JvmFrontendPipelinePhase`
now installing `java-direct` for every source session, HeaderMode runs hit the
`java-direct` resolution path. For these 3 tests the resolved field symbol is
correct and the bytecode emits the expected `GETFIELD` on the base Java class —
so the box returns `OK` where the PSI-loaded path historically returned `FAIL`.

`IGNORE_BACKEND: JVM_IR` is still in effect for the regular BlackBox runners
(`FirLightTreeBlackBoxCodegenTestGenerated`, `FirPsiBlackBoxCodegenTestGenerated`):
KT-56386 is not generally fixed, only sidestepped on the header-mode codepath
through java-direct's field-symbol shape.

### Fix

Removed the `// IGNORE_HEADER_MODE: JVM_IR` line from the 3 test files (left
`// IGNORE_BACKEND: JVM_IR` and the `// Reason: KT-56386 is not fixed yet`
comment untouched, as the underlying bug still mutes regular BlackBox).

Per `AGENT_INSTRUCTIONS.md` rule §6, test data is normally not touched to make
`java-direct` tests pass; the rule exception applies here because (a) these are
shared codegen tests, not `java-direct`'s own test data, (b) the framework's
suppressor itself raises the assertion telling us to remove the directive when
muted-but-passing, and (c) the change does not alter test semantics — only the
mute that no longer holds.

### Test Results

| Runner | Before | After |
|--------|--------|-------|
| `FirLightTreeHeaderModeCodegenTestGenerated.BoxJvm.JavaFieldAndKotlinProperty` | 20/23 (3 muted-passing assertions) | **23/23 ✅** |
| `FirLightTreeBlackBoxCodegenTestGenerated.BoxJvm.JavaFieldAndKotlinProperty` | 23/23 ✅ | 23/23 ✅ |
| `FirPsiBlackBoxCodegenTestGenerated.BoxJvm.JavaFieldAndKotlinProperty` | 23/23 ✅ | 23/23 ✅ |

### Files Modified

| File | Change |
|------|--------|
| `compiler/testData/codegen/boxJvm/javaFieldAndKotlinProperty/javaFieldKotlinPropertyJavaPackagePrivate.kt` | Drop `// IGNORE_HEADER_MODE: JVM_IR` |
| `compiler/testData/codegen/boxJvm/javaFieldAndKotlinProperty/javaProtectedFieldAndKotlinInvisibleProperty.kt` | Drop `// IGNORE_HEADER_MODE: JVM_IR` |
| `compiler/testData/codegen/boxJvm/javaFieldAndKotlinProperty/javaProtectedFieldAndKotlinInvisiblePropertyReference.kt` | Drop `// IGNORE_HEADER_MODE: JVM_IR` |

### Key Learnings

- `AbstractFirHeaderModeCodegenTestBase` registers `BlackBoxCodegenSuppressor`
  with `customIgnoreDirective = IGNORE_HEADER_MODE`, so the IGNORE_BACKEND and
  IGNORE_HEADER_MODE directives are scoped to *different* test runners. They
  can be in agreement (both muted on the same bug) but must be unmuted
  independently when a runner's resolution path stops triggering the bug.
- The unconditional `java-direct` install in `JvmFrontendPipelinePhase` means
  every CLI-pipeline-driven codegen suite (HeaderMode included) now exercises
  `java-direct` resolution, even when no `java-direct` test fixture is wired —
  expect more silent unmute candidates of the same shape across other
  IGNORE_HEADER_MODE muted tests, surfaced as suppressor assertions on the next
  full HeaderMode codegen run.
- `BlackBoxCodegenSuppressor.throwThatTestCouldBeUnmuted` is a hard failure;
  it is the framework's way of forcing engineers to clear stale mutes whenever
  a path change makes a test pass — treating it as a hint and not a bug.

---

## Lombok-plugin compatibility with `java-direct` — 2026-05-20

### Overview

Lombok K2 plugin tests (`:kotlin-lombok-compiler-plugin:test`,
`FirLightTreeBlackBoxCodegenTestForLombokGenerated`) regressed 13 / 66 after the
Stage-1.5 / 1.6 direct-injection wiring (`ac8736eae6a8`, see
`implDocs/DIRECT_INJECTION_STAGE_1_2026_05_20.md`) made the CLI test pipeline use
`java-direct` unconditionally. Two pre-existing `java-direct` gaps surfaced under
the Lombok plugin path; both fixed without touching `JvmFrontendPipelinePhase`,
keeping `java-direct` unconditional in CLI.

Pre-Stage-1.5, Lombok tests went through `JvmFrontendPipelinePhase`'s
`projectEnvironment.getFirJavaFacade(...)` which fell back to PSI when no
`JavaClassFinderFactory` extension was registered — Lombok tests never
registered `JavaDirectPluginRegistrar`, so they were on PSI. Post-Stage-1.5,
`JvmFrontendPipelinePhase` unconditionally hands `java-direct` to every
`createSourceSession` / `createLibrarySession` invocation, including those of
the `FirCliJvmFacade` test fixture that Lombok inherits via
`AbstractFirLightTreeBlackBoxCodegenTest` → `AbstractJvmBlackBoxCodegenTestBase`
→ `setupJvmPipelineSteps`.

### Root cause

Two independent bugs, both observable only when `java-direct` loads a Java
source file whose top-level annotations / fields participate in a Lombok plugin
generator (`AccessorGenerator`, `LombokConstructorsGenerator`, `BuilderGenerator`,
…).

1. **Single-segment star-import recovery missed in
   `JavaImportResolver.extractFragmentedImports`.** Lombok test data starts every
   `.java` block with a `// FILE: …` comment placed *above* the import line:

   ```java
   // FILE: ConstructorExample.java

   import lombok.*;

   @NoArgsConstructor public class ConstructorExample { … }
   ```

   The Java light-tree parser fragments this shape into root-level siblings
   instead of populating `IMPORT_LIST`:

   ```
   root children = [IMPORT_LIST (empty),
                    END_OF_LINE_COMMENT,
                    ERROR_ELEMENT(IMPORT_KEYWORD),
                    MODIFIER_LIST,
                    TYPE("lombok."),
                    ERROR_ELEMENT(""),
                    ERROR_ELEMENT("*;"),
                    CLASS]
   ```

   `extractFragmentedImports` recovers the star import via `findTypeNodeAndStar`,
   trims the trailing `.` from `"lombok."` → `"lombok"`, but the final guard
   `if (fqName.contains('.'))` was dropping it because the package name has no
   dot. Result: `star imports = []`, `JavaResolutionContext.resolve("NoArgsConstructor")`
   returns `null`, `JavaAnnotationOverAst.classId` falls back to
   `ClassId.topLevel(FqName("NoArgsConstructor"))` =
   `ClassId(root, NoArgsConstructor)`. Lombok's `getAnnotationByClassId(ClassId(lombok, NoArgsConstructor), …)`
   does not match → generator returns null → no constructor / getter / setter
   generation.

2. **`JavaField.hasInitializer` missing from the model.** Lombok's K2
   constructor-generator parts (`AllArgsConstructorGeneratorPart`,
   `RequiredArgsConstructorGeneratorPart`) need "does this field carry an
   initializer expression (constant or not)?" The previous implementation cast
   `declaration.source?.psi as? PsiField` and called `psiField.hasInitializer()`.
   For `java-direct`-loaded fields, `source.psi` is `null` → cast returns
   `null` → `hasInitializer = false`. Lombok then *includes* fields like
   `private Long zzzz = 23L` (non-constant per JLS 4.12.4 — `Long` is a
   reference type) in the generated constructor, producing
   `ConstructorExample(String foo, boolean otherField, Long zzzz)` instead of
   the expected `ConstructorExample(String foo, boolean otherField)`. The cast
   was itself a PSI leak in the K2 plugin path.

### Fixes

**(a) `JavaImportResolver.extractFragmentedImports`** — relax the FQN guard:
non-star imports still require a dot (`import Foo;` is illegal Java and not
recovered), but **star imports** accept single-segment package names. Code:

```kotlin
if (fqName.isNotEmpty()) {
    if (target.hasStar) {
        // Single-segment star imports (`import lombok.*;`) are valid Java;
        // the dot guard would have wrongly skipped them.
        starImports.add(FqName(fqName))
    } else if (fqName.contains('.')) {
        val simpleName = fqName.substringAfterLast('.')
        simpleImports.putIfAbsent(simpleName, FqName(fqName))
    }
}
```

**(b) `JavaField.hasInitializer: Boolean`** — new public property on the
`core/compiler.common.jvm/.../load/java/structure/JavaField` interface.
**Rule §7 exception** ([`AGENT_INSTRUCTIONS.md`](AGENT_INSTRUCTIONS.md)):
adding a member to a Java-model interface. Justification:

- **Net debt reduction**, not addition. The Lombok K2 plugin previously cast
  `source.psi as? PsiField` to reach `hasInitializer()`. That cast is itself a
  PSI leak into K2 plugin code; replacing it with a model-level property
  *removes* PSI from the plugin call path.
- **PSI must implement** `hasInitializer` too, because the K1→K2 migration
  expects Lombok K2 to work over PSI-loaded fields with non-constant
  initializers (e.g. `final String x = computeX();`) — a `java-direct`-private
  subinterface in `compiler/fir/fir-jvm/JavaModelExtensions.kt` would cover
  only the `java-direct` arm and regress the PSI arm. The cleanest shape is
  the same property on both impls, plus binary and javac-wrapper for
  completeness.

Semantics: `true` iff the source/binary declaration carries any initializer
expression, broader than the existing `hasConstantNotNullInitializer` (which
restricts to JLS 4.12.4 compile-time constants).

Implementations:

| Impl | Body | Notes |
|------|------|-------|
| `JavaFieldImpl` (PSI) | `getPsi().hasInitializer()` | The K1 PSI behavior — now also reachable from K2 without the PSI cast leak. |
| `JavaFieldOverAst` (`java-direct`) | `initializerNode != null` | The existing private `initializerNode: JavaLightNode?` is non-null iff `= …` follows the field name in the AST. |
| `BinaryJavaField` (ASM) | `initializerValue != null` | Class files only encode `ConstantValue` attribute — equivalent to `hasConstantNotNullInitializer`. |
| `TreeBasedField` (javac wrapper, JCTree) | `tree.init != null` | Direct javac access. |
| `SymbolBasedField` (javac wrapper, javax.lang.model) | `initializerValue != null` | Symbol view only sees constant `VariableElement.getConstantValue()`. |
| `MockKotlinField` (javac wrapper, K1 stub) | `shouldNotBeCalled()` | Same shape as other unsupported members on this mock. |
| `ReflectJavaField` (java.lang.reflect) | `false` | Reflection cannot observe initializer expressions. |

`FirJavaField` exposes the property through the existing lazy-property pattern:

- New constructor parameter `lazyHasInitializer: Lazy<Boolean>`, stored as
  `var lazyHasInitializer` (matching `lazyInitializer` / `lazyHasConstantInitializer`).
- New public `val hasInitializer: Boolean get() = lazyHasInitializer.value`.
- `FirJavaFieldBuilder` adds a `lateinit var lazyHasInitializer` and passes it
  to the constructor.
- `FirJavaFacade.createFirJavaField` populates `lazyHasInitializer = lazy { javaField.hasInitializer }`.
- `SignatureEnhancement` (which copies a `FirJavaField` into an enhanced
  variant) propagates `firElement.lazyHasInitializer` on the
  `FirJavaField`-typed branch and synthesises `lazy { firElement.initializer != null }`
  on the generic `FirField` branch.

Lombok K2 generators drop the `PsiField` cast and read `declaration.hasInitializer`
directly:

```kotlin
// AllArgsConstructorGeneratorPart.getFieldsForParameters
if (declaration.hasInitializer && (!isAllArgsConstructor || !declaration.isVar)) continue
```

```kotlin
// RequiredArgsConstructorGeneratorPart.isFieldRequired
if (isStatic) return false
if (hasInitializer) return false
if (isVal) return true
return annotations.any { it.unexpandedClassId?.asSingleFqName() in LombokNames.NON_NULL_ANNOTATIONS }
```

### Test Results

After both fixes:

| Suite | Tests | Failures | Errors |
|-------|------:|---------:|-------:|
| `:kotlin-lombok-compiler-plugin:test` → `FirLightTreeBlackBoxCodegenTestForLombokGenerated` | 66 | 0 | 0 |
| `:compiler:java-direct:test` → `JavaUsingAstBoxTestGenerated`    | 1181 | 0 | 0 |
| `:compiler:java-direct:test` → `JavaUsingAstPhasedTestGenerated` | 1519 | 0 | 0 |

No regressions on `JavaUsingAst*` (which already exercised `java-direct` over
typical annotation patterns). Lombok back to 100% green.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../resolution/JavaImportResolver.kt` | `extractFragmentedImports`: relax FQN guard — star imports accept single-segment package names; non-star still require a dot. |
| `core/compiler.common.jvm/src/.../load/java/structure/javaElements.kt` | `JavaField`: add `val hasInitializer: Boolean`. |
| `compiler/frontend.common.jvm/src/.../load/java/structure/impl/JavaFieldImpl.java` | PSI impl: `getPsi().hasInitializer()`. |
| `compiler/frontend.common.jvm/src/.../load/java/structure/impl/classFiles/Other.kt` | `BinaryJavaField`: `initializerValue != null`. |
| `compiler/java-direct/src/.../model/JavaMemberOverAst.kt` | `JavaFieldOverAst`: `initializerNode != null`. |
| `compiler/javac-wrapper/src/.../wrappers/trees/TreeBasedField.kt` | `tree.init != null`. |
| `compiler/javac-wrapper/src/.../wrappers/symbols/SymbolBasedField.kt` | `initializerValue != null`. |
| `compiler/javac-wrapper/src/.../resolve/KotlinClassifiersCache.kt` | `MockKotlinField`: `shouldNotBeCalled()`. |
| `core/descriptors.runtime/src/.../runtime/structure/ReflectJavaField.kt` | `false`. |
| `compiler/fir/fir-jvm/src/.../declarations/FirJavaField.kt` | New `lazyHasInitializer` constructor param + `hasInitializer` property; builder field. |
| `compiler/fir/fir-jvm/src/.../FirJavaFacade.kt` | Populate `lazyHasInitializer = lazy { javaField.hasInitializer }`. |
| `compiler/fir/fir-jvm/src/.../enhancement/SignatureEnhancement.kt` | Propagate `lazyHasInitializer` when copying `FirJavaField`; synthesise on the generic `FirField` branch. |
| `plugins/lombok/lombok.k2/src/.../generators/AllArgsConstructorGeneratorPart.kt` | Drop `PsiField` cast; read `declaration.hasInitializer`. |
| `plugins/lombok/lombok.k2/src/.../generators/RequiredArgsConstructorGeneratorPart.kt` | Drop `PsiField` cast; read `hasInitializer` on the receiver. |

### Key Learnings

- **`// FILE: …` testdata comments break Java parser shapes.** The
  Kotlin-testdata convention of placing `// FILE:` separator comments at the
  top of each `.java` block defeats the parser's `IMPORT_LIST` recognition and
  scatters imports across `ERROR_ELEMENT` siblings of the file root. Any
  recovery path in `JavaImportResolver` must accept single-segment FQN
  recovery for star imports — `import foo.*;` is valid even when `foo` has no
  dots.

- **The Lombok K2 plugin had a hidden PSI dependency.** Before this work,
  `(declaration.source?.psi as? PsiField)?.hasInitializer()` silently returned
  `null` (cast fails) for any non-PSI Java-model impl. K1's
  `RequiredArgsConstructorProcessor` / `AllArgsConstructorProcessor` had the
  same shape. Replacing the cast with a model-level `hasInitializer` property
  *removes* PSI from the K2 plugin call path — a net debt reduction even
  though it required a new member on a `JavaField` interface (rule §7
  exception).

- **`hasConstantNotNullInitializer` is not "has any initializer".** The
  former is restricted to JLS 4.12.4 constant variables (primitive or
  `String`, `final`, initialized to a constant expression). `Long zzzz = 23L`
  *has* an initializer but is *not* a constant variable. Lombok semantics ask
  the broader question, so a separate property is the right model-level fix —
  conflating the two would break enhancement-time / annotation-evaluation
  code that genuinely relies on the JLS-strict definition.

- **`lateinit` fields on `FirJavaField` builders silently regress.** Adding a
  `lateinit var lazyHasInitializer` to the builder is invisible until a
  caller forgets to set it; the resulting
  `UninitializedPropertyAccessException` surfaces hundreds of test methods
  later. The single missed call-site in this iteration was
  `SignatureEnhancement.kt:174` (the `FirField → FirJavaField` copy path).
  Future builder additions should grep all `buildJavaField { … }` blocks
  before committing.

- **CLI test fixtures inherit the CLI pipeline.** `FirCliJvmFacade` runs
  `JvmFrontendPipelinePhase` directly. Any wiring change in
  `JvmFrontendPipelinePhase` immediately affects every black-box codegen /
  phased-diagnostic test that uses `setupJvmPipelineSteps`, including
  unrelated plugins like Lombok. `JvmConfigurationPipelinePhase` (the
  *configuration* phase that used to register `JavaDirectPluginRegistrar`) is
  **not** run by `FirCliJvmFacade`, so configuration-time gates are invisible
  to tests — gates have to be in the frontend phase or earlier.

---

## Archived Iteration History

Earlier entries have been moved to dated archives under `implDocs/archive/`:

- `implDocs/archive/ITERATION_RESULTS_2026_05_11.md` — entries 2026-04-22 →
  2026-05-11 (this archive). Covers post-refactoring cleanup, PSI removal
  (Phase 1-2), merged refactoring plan (Stages 1-4 + 4.5a-c public-interface
  rollback), the IJ-FP regression delta (Cat A-E), and the
  `JavaUsingAst*` test framework wiring fix.
- `implDocs/archive/ITERATION_RESULTS_2026_04_22.md` — full log of Phases A-E
  of `REFACTORING_PLAN_2026_04_21.md`: Phase B regression investigation,
  Phase C measurements, Phase D implementation, Phase E cleanup.
- `implDocs/archive/REFACTORING_PLAN_2026_04_21.md` — the 5-phase plan (A-E).
- `implDocs/archive/MEASUREMENTS_2026_04_22.md` — Phase C measurement data
  (8 hypotheses, 3 corpora, corrected classloader-isolation methodology).
- `implDocs/archive/REFACTORING_STEPS_2026_04_17_DETAILS.md` — earlier
  refactoring steps 1.3-3.6.
- `implDocs/archive/LAZY_PACKAGE_INDEXING_PLAN_2026_04_21.md` — lazy
  per-package indexing design (implemented).
- `implDocs/archive/ITERATIONS_52_71_DETAILS.md` — iterations 52-71
  (2026-03-23 → 2026-04-16): wrong-arity type arguments, transitive
  inherited inner class resolution, performance round (61-65), cross-package
  inherited inner classes, multi-field declarations, and the original
  `JavaResolutionContext` split into collaborators.
- `implDocs/archive/ITERATIONS_37_51_DETAILS.md`,
  `implDocs/archive/ITERATIONS_27_36_DETAILS.md`,
  `implDocs/archive/ITERATIONS_24_26_DETAILS.md`,
  `implDocs/archive/ITERATIONS_17_23_DETAILS.md`,
  `implDocs/archive/ITERATIONS_7_16_DETAILS.md`,
  `implDocs/archive/ITERATIONS_1_6_DETAILS.md` — earlier numbered iterations.

### Open items carried forward

- **Context-level `tryResolve` cache** (`PERFORMANCE_REVIEW_2026-04-20.md` §2 #6)
  — deferred with a recorded correctness argument. Only revisit if profiling
  shows `resolve()` as a measurable bottleneck.
- **Variant D of `FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` §12 Q1** — the
  `FirJavaClass.javaClass` visibility flip — preserved as a fallback in the
  proposal but not taken; `directSupertypeClassIds()` (Variant C) is shipped.
- **Build-time enforcement that `LazySessionAccess` is the only `ThreadLocal`
  / re-entrance choke-point in resolution code** — a grep gate or detekt rule
  could forbid `ThreadLocal` in `compiler/java-direct/.../resolution/` to avoid
  reintroducing the old per-thread re-entrance pattern.
