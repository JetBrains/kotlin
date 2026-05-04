# Java-Direct: Iteration Results Log

**Current status**: 1178/1178 box + 1513/1513 phased (2793/2793, 100%). Phased and
box generators now actually route `// FILE: *.java` blocks through java-direct AST;
prior numbers were against PSI loading (see 2026-04-28 entry).

**Last Updated**: 2026-04-30 (Phase 1 of PSI replacement implemented behind a default-OFF flag)

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

## Phase 1: `BinaryJavaClassFinder` landed behind default-OFF flag — 2026-04-30 (later still)

### Overview

Implemented Phase 1 of the PSI removal plan documented in
`implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`: an index-based, PSI-free
`BinaryJavaClassFinder` (placed inside the `java-direct` module) backed by the same
`JvmDependenciesIndex` / `KotlinClassFinder` snapshot the deserializer already uses, plus the
existing ASM-based `BinaryJavaClass`. It replaces the legacy PSI binary half of
`CombinedJavaClassFinder` when the `kotlin.javaDirect.useBinaryClassFinder` system property
is `true`. Default is `false`, so existing production behaviour is unchanged.

### Changes

- Added `compiler/java-direct/src/.../BinaryJavaClassFinder.kt`. ~205 lines. Mirrors
  `KotlinCliJavaFileManagerImpl.findClass` for binary classes (top-level virtual file lookup
  via `JvmDependenciesIndex.findClassVirtualFiles`, ASM materialization via `BinaryJavaClass`,
  inner classes via `BinaryJavaClass.findInnerClass`, per-call fresh
  `ClassifierResolutionContext` for type-parameter / inner-class isolation, scope-free resolver
  for cross-classpath references).
- Added `compiler/cli/src/.../extensions/BinaryJavaClassFinderInputs.kt`: a small data carrier
  (`JvmDependenciesIndex` + `GlobalSearchScope` + `enableSearchInCtSym`) plumbed through
  `JavaClassFinderFactory`. The carrier exists to avoid a circular dependency: `compiler/cli`
  cannot reference types from `compiler/java-direct`, so `cli` ships the *inputs* and the
  factory in `java-direct` constructs the actual finder.
- `JavaClassFinderFactory.createJavaClassFinder` now takes an optional
  `binaryClassFinderInputsProvider: (() -> BinaryJavaClassFinderInputs?)?` parameter (default
  `null`). Lazy provider returns `null` outside CLI environments (e.g. LL-FIR), in which case
  the factory falls back to the legacy PSI default — preserves existing behaviour for non-CLI
  callers.
- `VfsBasedProjectEnvironment.getFirJavaFacade` plumbs the inputs lazily by downcasting
  `VirtualFileFinderFactory.getInstance(project)` to `CliVirtualFileFinderFactory` and
  reading its `index` / `enableSearchInCtSym`.
- `CliVirtualFileFinderFactory.index` and `enableSearchInCtSym` are now `val` (publicly
  readable) so the environment can hand them off to the factory.
- `JavaDirectPluginRegistrar.JavaClassFinderOverAstFactory.createJavaClassFinder` now reads
  the system property `kotlin.javaDirect.useBinaryClassFinder` (default `false`). When `true`
  and inputs are available, the binary half of `CombinedJavaClassFinder` is the new
  `BinaryJavaClassFinder`; otherwise the legacy PSI `defaultFinderProvider()` is used.
- `compiler/java-direct/build.gradle.kts`: added a one-line `systemProperty` passthrough so
  the flag flows from `-Pkotlin.javaDirect.useBinaryClassFinder=true` into the test JVM.

### Test Results

- **Default (flag OFF)**: `JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated`
  = **2692/2692 (100%)**. No regression vs. baseline.
- **Flag ON** (`-Pkotlin.javaDirect.useBinaryClassFinder=true`): **2686/2692 (99.78%)**. Six
  remaining test-data divergences (all `assertEqualsToFile` diffs in the diagnostic phase),
  documented as Phase-1 follow-up work below.

### Phase-1 follow-up work

The six failures under flag ON are documented for a follow-up iteration; the flag stays
default-OFF so production parity is preserved while these are triaged:

1. `JavaUsingAstPhasedTestGenerated.Tests.Imports.testEnumEntryVsStaticAmbiguity4`
2. `JavaUsingAstPhasedTestGenerated.ResolveWithStdlib.J_k.testAnnotationWithEnum`
3. `JavaUsingAstPhasedTestGenerated.Tests.Properties.testProtectedGetterWithPublicSetter`
4. `JavaUsingAstPhasedTestGenerated.Tests.testProtectedWithGenericsInDifferentPackage`
5. `JavaUsingAstPhasedTestGenerated.Tests.Regressions.testKt57845`
6. `JavaUsingAstPhasedTestGenerated.Tests.SmartCasts.Inference.testSyntheticPropertyOnUnstableSmartcast`

All six are `Actual data differs from file content: *.kt` diagnostic-phase divergences (no
crashes, no compile errors). They likely involve subtle differences between PSI's package
enumeration and the index-based `knownClassNamesInPackage` (e.g. how multi-file packages with
mixed Java/Kotlin sources are unioned across source ∪ binary halves), or how
`BinaryJavaPackage` reports `mayHaveAnnotations` differently from `JavaPackageImpl`.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../BinaryJavaClassFinder.kt` | New: ~205 lines, the index-based finder (Phase 1 stepping stone). |
| `compiler/cli/src/.../extensions/BinaryJavaClassFinderInputs.kt` | New: small data carrier for the cli→java-direct plumbing. |
| `compiler/cli/src/.../extensions/JavaClassFinderFactory.kt` | Added `binaryClassFinderInputsProvider` parameter. |
| `compiler/cli/src/.../VfsBasedProjectEnvironment.kt` | Plumbs inputs lazily via `CliVirtualFileFinderFactory` downcast. |
| `compiler/cli/cli-base/src/.../CliVirtualFileFinderFactory.kt` | Made `index` / `enableSearchInCtSym` public. |
| `compiler/java-direct/src/.../JavaDirectPluginRegistrar.kt` | Reads the system-property flag and selects which binary finder to inject. |
| `compiler/java-direct/build.gradle.kts` | One-line `systemProperty` passthrough for the flag. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; updated `Last Updated` line. |

### Key Learnings

- **`ClassifierResolutionContext` is mutable** — it accumulates type parameters and
  inner-class info across every `BinaryJavaClass` it materializes. Sharing one instance
  across `findClass` calls leaks type parameters from one class into the resolution of an
  unrelated one (symptom: "Unresolved type for E"). The fix is to construct a fresh context
  per top-level `findClass` invocation, exactly as `KotlinCliJavaFileManagerImpl.findClass`
  line 151 does.
- **The internal resolver must use `allScope` (not the finder's `scope`)** for cross-class
  references inside bytecode signatures — otherwise references to JDK classes from a
  library-scoped finder fail silently. Mirrors the same `allScope` choice in the reference
  implementation.
- **Circular module-dependency avoidance** — `compiler/cli` cannot depend on
  `compiler/java-direct`, so the cli-side environment ships *inputs* (an index handle, a
  scope, a flag) rather than constructing the `JavaClassFinder` itself; the `java-direct`
  factory builds the finder from those inputs.
- **Default-OFF flag** is a real safety net — even with all the structural plumbing in
  place, a single edit error (forgotten function-signature change, stale build) shows up as
  "BUILD FAILED" but **the test results directory still has the *previous* run's XMLs**,
  giving a misleadingly clean count. Always verify test results were freshly written
  *after* the BUILD FAILED was resolved.

---

## Design doc revision: three-phase plan for PSI removal — 2026-04-30 (later)

### Overview

Reframed `implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` from a single-step
`BinaryJavaClassFinder` proposal into an explicit three-phase plan: Phase 1 lands
`BinaryJavaClassFinder` as a short-lived stepping stone (not kept across releases);
Phase 2 collapses the abstraction, moves binary lookups into
`JvmClassFileBasedSymbolProvider`, and makes `FirJavaFacade.classFinder` source-only;
Phase 3 keeps PSI for **1–2 releases as a source-only fallback** behind a flag, after
which PSI is removed from the JVM-FIR / `java-direct` compilation path entirely. No
production source files were modified.

### Changes

- Rewrote §0 Executive Summary with three replacement diagrams (today / Phase 1 /
  end-state) and an explicit "PSI removed at end of Phase 3" goal.
- Restructured §2.1 around strategic goals (across all phases) plus per-phase
  constraints; added the IntelliJ-platform-dependency removal goal.
- Marked the existing `BinaryJavaClassFinder` design (§2.2) and cycle-avoidance (§2.3)
  as Phase-1-specific.
- Added new §2.4 Phase 2 (structural refactoring) and new §2.5 Phase 3 (source-only
  PSI/AST switch).
- Renumbered the migration plan (§2.6) to span all three phases; renumbered Risks
  (§2.7) into per-phase subsections including indirect-caller audit, narrowed
  `FirSession.javaSymbolProvider` semantics, AST/PSI source parity gate, and PSI
  removal blast radius.
- Renumbered Alternatives (§2.8) to record explicitly that "Keep `BinaryJavaClassFinder`
  long-term" and "Keep PSI as a binary-side fallback" were considered and rejected.

### Test Results

N/A (documentation only).

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` | Three-phase plan revision (§0, §2.1–§2.8). |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; updated `Last Updated` line. |

### Key Learnings

- The transitional fallback for the PSI removal effort belongs on the *source* side
  (Phase 3), not on the binary side; binary PSI is removed at the end of Phase 1 with
  no transitional residence.
- `BinaryJavaClassFinder` is justified strictly as a risk-isolation device — observable
  equivalence with PSI, A/B-flag-flippable — and is dissolved in Phase 2; keeping it
  long-term would re-introduce a parallel class-finder abstraction on top of a symbol
  provider stack that already owns the data source.
- The dominant cost of the structural Phase 2 is the audit of the four indirect
  callers of `session.javaSymbolProvider` (`FirJvmConflictsChecker`,
  `FirDirectJavaActualDeclarationExtractor`, Lombok `AbstractBuilderGenerator`, and
  out-of-scope `KaFirJavaInteroperabilityComponent`); this is paid once and unblocks
  the contract narrowing of `FirSession.javaSymbolProvider` to "source-only Java".
- Phase 3's PSI removal completes the IntelliJ-platform-dependency shedding for the
  JVM-FIR / `java-direct` compilation path; full deletion of `JavaClassFinderImpl` is
  separate (K1 frontend and LL-FIR keep their own copies and are out of scope).

---

## Design doc: `PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` — 2026-04-30

### Overview

Design-only deliverable: a new `implDocs/` document that maps every PSI `JavaClassFinder` entry
point reachable in production with `java-direct` enabled, and proposes a `BinaryJavaClassFinder`
backed by `JvmDependenciesIndex` / `KotlinClassFinder` + `BinaryJavaClass` to replace the
PSI binary half of `CombinedJavaClassFinder`. No production source files are modified.

### Changes

- Added `compiler/java-direct/implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`.
- This entry in `ITERATION_RESULTS.md`.

### Test Results

N/A (documentation only).

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` | New design doc (Part 1: where PSI is used; Part 2: replacement plan). |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry. |

### Key Learnings

- `JavaClassFinderOverAstFactory` builds `CombinedJavaClassFinder(source, PSI-binary)` for **both** source and library FIR sessions in production — the test-fixture (`VfsBasedProjectEnvironmentOverAst`) only exercises PSI in the library session.
- `JvmClassFileBasedSymbolProvider.extractClassMetadata`'s no-metadata branch (`JvmClassFileBasedSymbolProvider.kt:180`) is the only place in FIR core that asks the facade to materialize a `JavaClass` from a binary `.class` — and the bytes are already in hand via `KotlinClassFinder.Result.ClassFileContent`, so a replacement does not need extra I/O.
- The `FirJavaFacade ↔ JvmClassFileBasedSymbolProvider` cycle is avoided by making `BinaryJavaClassFinder` a **peer** of the deserializer (both fed by `JvmDependenciesIndex`), not a wrapper around it.
- `JavaClassFinder.findClasses` (multi-result) has no production FIR caller — only PSI's own `JavaClassFinderImpl` and LL-FIR's `LLCombinedJavaSymbolProvider` use it; the replacement does not need to support it.

---

## PSI-path regression in shared FIR files: gate the java-direct fallbacks — 2026-04-29

### Overview

Investigated a ~5% regression on `KotlinFullPipelineTestsGenerated` (PSI path,
java-direct off) that appeared after the java-direct development cycle. Root cause: the
java-direct-specific resolution fallbacks added to shared FIR files run unconditionally
on the PSI/binary path even though they have no effect there. Closed by adding three
opt-in `Boolean` properties to the `JavaType` / `JavaField` / `JavaEnumValueAnnotationArgument`
interfaces (default `false`) and gating the FIR call sites on them. java-direct overrides
to `true` to keep the existing fallbacks active for its path.

### How the regression was identified

Branch state before the work: top two commits were
`66086559d511 ~ undo changes outside of java-direct` (reverts the shared-FIR/structure
changes accumulated during java-direct development) and
`a9e0e74fd498 ~ undo apply by default` (returns `LanguageFeature.JavaDirect` to
`sinceVersion = null` and the registrar guard back). With both reverted, the branch HEAD
is a pure baseline; reverting just the top commit re-applies the shared-FIR changes
without turning java-direct on.

Three SAME_THREAD measurements of `KotlinFullPipelineTestsGenerated` (single rep each,
XML test-phase wall time, build kept warm between runs) confirmed the regression as a
PSI-path issue, not a java-direct issue:

| Config | Time | Δ vs baseline |
|---|---:|---:|
| Baseline (HEAD as-is, external changes reverted) | 236.27s | — |
| Regression (revert of top commit, no fix) | 241.57s | **+2.24%** |
| With first gate (`couldBeConstReference`) | 230.30s | -2.53% |
| With all three gates | 235.30s | -0.41% |

The regression-vs-fix delta of ~5% matches the originally observed FP-test slowdown.
All "with-fix*" configurations are within single-run noise (~±2%) of baseline.

### Root cause

Three call sites in shared FIR files take a callback that's wasted on PSI/binary input:

1. **`javaAnnotationsMapping.toFirExpression`'s `JavaEnumValueAnnotationArgument` branch**
   calls `resolveConstFieldValue(session, classId, fieldName)` for every enum-shaped
   annotation argument — including dominant cases like `@Retention(RUNTIME)`,
   `@Target(METHOD)`, `@Target({TYPE, FIELD})`. The helper does
   `session.symbolProvider.getClassLikeSymbolByClassId(classId)`, allocates a
   `filterIsInstance<FirProperty>()` list of declarations, walks both the class and its
   companion, then probes `session.symbolProvider.getTopLevelPropertySymbols(...)`. PSI
   never reaches this code path with a real const-reference because PSI splits literal
   const refs (`KConstsKt.WARNING`) into `JavaLiteralAnnotationArgument` at structure-build
   time; only java-direct (which can't disambiguate at parse time) needs the fallback.

2. **`JavaTypeConversion.toFirJavaTypeRef` and `toConeTypeProjection`** both call
   `filterTypeUseAnnotations { fqName -> isTypeUseAnnotationClass(fqName, session) }` per
   type-ref / type-projection. PSI's `JavaTypeImpl` doesn't override
   `filterTypeUseAnnotations`, so the default impl just returns `annotations`; the cost
   is one closure capturing `session` plus a virtual-dispatch round-trip per call. Cheap
   per call, but `annotationBuilder` fires once per Java type ref during enhancement, so
   it adds up.

3. **`FirJavaFacade.convertJavaFieldToFir`'s `lazyInitializer`** falls back to
   `javaField.resolveInitializerValue { … }` when `initializerValue` is `null`. PSI's
   `JavaFieldImpl` doesn't override `resolveInitializerValue`, so the fallback returns
   `null` again — but at the cost of one closure capturing `session` and
   `classId.packageFqName`. Hits every cross-language const-evaluation site.

Other branches in the reverted commit (`setSealedClassInheritors` cross-file path,
`enumEntriesOrigin`, `isPrimary` for source records, the entire `null`-classifier branch
in `JavaTypeConversion`) are dead code on the PSI path because they're guarded by
`classifier == null` or `source == null` — so they cannot have caused the regression.

### Fix

Three `Boolean` opt-in properties (default `false`) — PSI/binary inherit the default and
never enter the costly branch; java-direct overrides to `true` and continues to take its
existing fallback path:

- `JavaEnumValueAnnotationArgument.couldBeConstReference` — gates `resolveConstFieldValue`.
  PSI structurally splits const-vs-enum at build time; java-direct can't, so it opts in.
- `JavaType.needsTypeUseAnnotationFiltering` — gates the `filterTypeUseAnnotations`
  callback closure. PSI's javac-wrapper pre-filters at the structure level; java-direct
  filters at FIR call time.
- `JavaField.supportsExternalInitializerResolution` — gates the
  `resolveInitializerValue` callback closure. PSI evaluates Java-side constants at
  structure-build time; java-direct uses the FIR callback for cross-language const refs.

Additionally, `resolveConstFieldValue` short-circuits when `firClass.classKind ==
ClassKind.ENUM_CLASS`. Real enum classes can only have const properties via their
companion (entries are `FirEnumEntry`, not `FirProperty`), and the top-level/facade
fallback doesn't apply to an `<EnumClass>.X` shape. This eliminates the
`filterIsInstance<FirProperty>()` allocation and the top-level lookup for the dominant
"actual enum entry" case on java-direct's own path.

### Files Modified

| File | Change |
|------|--------|
| `core/compiler.common.jvm/src/.../structure/annotationArguments.kt` | Add `JavaEnumValueAnnotationArgument.couldBeConstReference: Boolean = false` |
| `core/compiler.common.jvm/src/.../structure/javaTypes.kt` | Add `JavaType.needsTypeUseAnnotationFiltering: Boolean = false` |
| `core/compiler.common.jvm/src/.../structure/javaElements.kt` | Add `JavaField.supportsExternalInitializerResolution: Boolean = false` |
| `compiler/fir/fir-jvm/src/.../fir/java/javaAnnotationsMapping.kt` | Gate `resolveConstFieldValue` on `couldBeConstReference`; short-circuit `resolveConstFieldValue` for enum classes |
| `compiler/fir/fir-jvm/src/.../fir/java/JavaTypeConversion.kt` | Gate `filterTypeUseAnnotations` on `needsTypeUseAnnotationFiltering` at both call sites |
| `compiler/fir/fir-jvm/src/.../fir/java/FirJavaFacade.kt` | Gate `resolveInitializerValue` callback on `supportsExternalInitializerResolution` |
| `compiler/java-direct/src/.../model/JavaAnnotationOverAst.kt` | Override `couldBeConstReference = true` on `JavaEnumValueAnnotationArgumentOverAst` |
| `compiler/java-direct/src/.../model/JavaTypeOverAst.kt` | Override `needsTypeUseAnnotationFiltering = true` on `JavaTypeOverAst` |
| `compiler/java-direct/src/.../model/JavaMemberOverAst.kt` | Override `supportsExternalInitializerResolution = true` on `JavaFieldOverAst` |

### Test Results

- `kotlin-java-direct:test` (`JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated`):
  **2692/2692 green**, no FAILED markers. Run twice — once with only the first gate, once
  with all three gates plus the enum short-circuit. java-direct functionality preserved
  in both states.
- `KotlinFullPipelineTestsGenerated` (SAME_THREAD): see table above. Regression closed.

### Methodology notes

- `ExecutionMode.SAME_THREAD` was set in
  `GenerateModularizedIsolatedTests.kt:27` and the test class was regenerated via
  `:compiler:fir:modularized-tests:generateTests` so all 414 modules run sequentially —
  needed for stable wall-clock timing under SUM-not-MAX semantics. Revert before merge.
- The XML `time="…"` field in
  `compiler/fir/modularized-tests/build/test-results/test/TEST-…KotlinFullPipelineTestsGenerated.xml`
  is the right metric; Gradle's "BUILD SUCCESSFUL in Xm Ys" mixes test phase with build
  phase, and the build phase shrinks dramatically across runs as caches warm up,
  inflating the BUILD-SUCCESSFUL delta vs. real test-phase delta.
- Single-rep noise looked to be ~±2% on this corpus. Three reps each would tighten the
  signal, but the regression-vs-fix delta of ~+5% / +11s is well above noise on a single
  rep.

### Key Learnings

- **Adding overridable interface methods with default impls to shared types is not free
  for the default-path callers.** Even when the default impl is "return the same thing
  the caller already has", every call still pays a virtual-dispatch and a callback
  closure allocation. When the call site is hot (per-Java-type-ref or per-annotation-arg
  during FIR enhancement), this can cost a few percent on workloads that don't need the
  override at all. Pairing every such method with a `Boolean` "`needsX`" gate on the same
  interface is the cheap way to keep the API additive without taxing the default path.
- **`isResolved` is not a substitute for "needs the const fallback".** java-direct's
  `JavaEnumValueAnnotationArgumentOverAst.isResolved` returns `true` for the easy
  "simple-imported" case (where `enumClassId` is built from a known import), so gating on
  `!isResolved` would have skipped the const fallback for the very case it's needed —
  `@SomeAnno(SomeImportedClass.SOME_CONST)`. The right gate is "could this argument
  ever be a const reference" — orthogonal to "is the enum class identifier already
  known".
- **Enum classes never carry const FirProperty members directly.** Their entries are
  `FirEnumEntry`. Code that walks `firClass.declarations.filterIsInstance<FirProperty>()`
  looking for a const named like the entry will always come up empty. Detecting this
  shape upfront skips a list allocation and a top-level symbol probe per
  enum-typed annotation argument — meaningful on java-direct's path where the same
  fallback runs (`couldBeConstReference = true`).
- **Branches guarded by `classifier == null` / `source == null` cannot affect the PSI
  path.** Several reverted blocks in `FirJavaFacade` (`setSealedClassInheritors` cross-
  file lookup, `enumEntriesOrigin` for source enums, `isPrimary` for source records,
  the whole `null`-classifier branch in `JavaTypeConversion`) only fire for java-direct.
  Those should not be searched for the source of a PSI-only regression.

### Follow-ups not in this iteration

- Re-measure on `IntelliJFullPipelineTestsGenerated` (Java-heavy, ~10× annotation
  density vs. Kotlin pipeline). The two follow-up gates
  (`needsTypeUseAnnotationFiltering`, `supportsExternalInitializerResolution`) showed no
  measurable benefit on the Kotlin pipeline; their per-call cost is small and may need a
  larger / annotation-heavier corpus to surface in single-rep timing.
- Multi-rep run (3+ reps each) of all four configurations to tighten the noise envelope
  below ±1%.
- The same `resolveConstFieldValue` runs on java-direct's path (`couldBeConstReference =
  true`); for further tightening of the java-direct/PSI gap on this code path, look at
  caching the `(classId, fieldName) → constValue?` lookup at the session level — most
  call traffic is for a small set of well-known JDK enum entries that produce the same
  null answer many times over.

---

## Test framework wiring: java-direct AST was never used — 2026-04-28

### Overview

Follow-up on the "Coverage gap…" entry below. Investigating why
`testSealedJavaCrossFilePermits` failed with the FIR fix in place, instrumentation
revealed that `JavaUsingAstPhasedTestGenerated` did **not** route `// FILE: *.java`
blocks through java-direct's AST at all. The 7 placeholder tests passed for the same
reason: every Java class was loaded via PSI (`JavaClassFinderImpl`), so the
`classifier == null` branches in the four shared FIR files were never exercised.

After two infrastructure fixes (and a small `JavaPackageIndexer` extension), all 8
tests now actually drive java-direct, and the suite is **2793/2793** green.

### Root cause #1 — scope filter rejected directory source roots

`VfsBasedProjectEnvironment.getFirJavaFacade` passed a `findLocalFile` callback to
`JavaClassFinderFactory.createJavaClassFinder` that filtered through
`psiSearchScope.contains(vf)`:

```kotlin
{ localFs.findFileByPath(it)?.takeIf { vf -> psiSearchScope.contains(vf) } }
```

For the `<main>` module the scope is `AllJavaSourcesInProjectScope`, whose
`contains(file)` rejects directories (line 18: `(extension == "java" || ...) && !isDirectory`).
The factory uses the callback to resolve `configuration.javaSourceRoots` — *directory*
paths — so every entry came back `null`, the factory found 0 source roots, and fell
back to `defaultFinderProvider()` (PSI).

For `<regular dependencies of main>` the scope is `librariesScope` (no directory
check), so the dependency session got `CombinedJavaClassFinder` — but that session
never resolves user-Java classes referenced from Kotlin source.

**Design issue:** the `findLocalFile` callback conflated two things: path-to-VirtualFile
resolution (which can target a directory) and scope membership (defined at the
`.java`-file level). The PSI-based finder doesn't have this issue — it applies scope
inside its class-lookup methods, never to source-root paths.

**Fix (refactor):** drop `findLocalFile` from `JavaClassFinderFactory` API entirely.
The factory implementation resolves source-root paths directly via
`localFs.findFileByPath`. If an implementation needs class-file scope filtering, the
existing `scope` parameter is still there.

### Root cause #2 — package indexer rejected files whose disk path didn't match `package`

After fix #1, `J` from `testDottedJdkNestedClassFqn` resolved as `JavaClassOverAst`
correctly. But `testWithUnitType` regressed: `JavaUtils.java` (declaring `package test;`)
written flat at `java-sources/main/JavaUtils.java` became invisible. javac is tolerant
(it places `.class` by declared package, ignoring source location), but
`JavaPackageIndexer.tryBuildFileEntry` enforces directory-mirrors-package and skipped
the file. The lookup for `<root>/JavaUtils` matched the directory but failed parse-time
(declared `test`); the lookup for `test.JavaUtils` walked `test/` which doesn't exist.

**Fix:** in `JavaPackageIndexer.init`, after the file-root scan, also scan each
directory root's top-level `.java` files. Files declaring a non-root package are
registered in `fileRootIndex` under their declared package — so they're discoverable
even when disk path doesn't mirror the package. Top-level files declaring the root
package are still picked up by the regular root walk, so we skip them here to avoid
duplicates. Real-world layouts (`src/main/java/com/example/`) have no `.java` files
at the top of the source root, so this scan is essentially free for non-test workloads.

### Root cause #3 — failing test data was self-inconsistent

`sealedJavaCrossFilePermits.kt` declared `Base` as `sealed class` (non-abstract). The
java-direct path correctly registered `Sub1`/`Sub2` as inheritors, but
`FirJavaFacade.isJavaNonAbstractSealed` set `true` for non-abstract sealed Java
classes; `FirWhenExhaustivenessComputer` then required `is Base` in addition to
`is Sub1, is Sub2`. The `when` in the test had only Sub1/Sub2, so it was non-exhaustive
regardless of inheritor registration.

**Fix:** change Base to `abstract sealed`. Now `isJavaNonAbstractSealed` stays false
and `is Sub1, is Sub2` is exhaustive — the test cleanly catches the regression.

### How we diagnosed it

`JavaClassFinderOverAstFactory.createJavaClassFinder` was being called twice (once
for `<regular dependencies>`, once for `<main>`) with different `psiSearchScope`
hashes. Tracing `findLocalFile` per source-root path showed `resolved=null` for the
java-sources directory in the `<main>` call but `resolved=<path>` for the `<deps>`
call — confirming the scope filter was the discriminator. Tracing
`FirJavaFacade.findClass` showed `classFinder=JavaClassFinderImpl` (PSI) for `<main>`,
not `CombinedJavaClassFinder` — so user Java classes never reached java-direct.

Don't trust "test passes" as evidence that java-direct ran. Verify by stack-trace or
by checking which `JavaClassFinder` the source session's `JavaSymbolProvider` ended up
with.

### Status update for the gap-test table

The 8 tests under `compiler/testData/diagnostics/tests/jvm/javaDirectGap/` now all
actually exercise java-direct's AST. With the FIR fixes in place: all 8 pass. With the
shared FIR files reverted, `testSealedJavaCrossFilePermits` is the confirmed regression
catcher (the original design intent). The other 7 are positive coverage for
java-direct AST paths that were previously untested.

### Files Modified

| File | Change |
|------|--------|
| `compiler/cli/src/.../extensions/JavaClassFinderFactory.kt` | Drop `findLocalFile: (String) -> VirtualFile?` parameter; clarify `scope`/`localFs` doc |
| `compiler/cli/src/.../VfsBasedProjectEnvironment.kt` | Stop passing the broken scope-filter lambda |
| `compiler/java-direct/src/.../JavaDirectPluginRegistrar.kt` | Resolve source roots via `localFs::findFileByPath` (no callback) |
| `compiler/java-direct/src/.../JavaPackageIndexer.kt` | Pre-index top-level `.java` files declaring non-root packages |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/sealedJavaCrossFilePermits.kt` | `sealed` → `abstract sealed` so the `when` is exhaustive without `is Base` |

### Test Results

`./gradlew :kotlin-java-direct:test` — 2793 tests, 0 failures. (Up from 2679/2681
because the 8 javaDirectGap tests now run, and `JavaUsingAstPhasedTestGenerated`'s
existing tests are also routed through java-direct AST instead of PSI.)

### Key Learnings

- **`JavaUsingAstPhasedTestGenerated` did NOT exercise java-direct before this fix.**
  The pluggable `JavaClassFinderFactory` was registered, the AST finder was even
  *constructed*, but for the `<main>` module its source roots were filtered out and
  the factory fell back to PSI. Existing 1454 phased + 1168 box tests were green
  through pure PSI paths — they validated FIR behavior, not java-direct.
- **API design: don't conflate path resolution with class-scope membership.** The
  `findLocalFile` callback's contract was "scope-restricted path resolution", but
  `AbstractProjectFileSearchScope` is a class-file scope, not a path scope. Source
  roots are directories; passing them through a class-scope filter is meaningless.
- **java-direct's package indexer assumed standard Java layout.** Test frameworks
  often write files flat regardless of `package` declaration. javac is tolerant about
  this; java-direct now is too, for top-level files of a directory root.
- **Verifying that a test exercises java-direct requires instrumentation.** Stack
  trace `JavaSymbolProvider.classCache` lookups, check the `classFinder` field on
  `FirJavaFacade`. Tests passing or failing is not evidence of which finder served
  the classes.

---

## Coverage gap: shared FIR regressions invisible to java-direct suite — 2026-04-28

### Overview

Investigation of why the java-direct suite (1168/1168 box, 1454/1456 phased) stayed
green while `KotlinFullPipelineTestsGenerated` started failing 57 modules after the
shared FIR files (`FirJavaFacade.kt`, `JavaTypeConversion.kt`, `javaAnnotationsMapping.kt`,
`ConeRawScopeSubstitutor.kt`) were dropped from a clean-branch cherry-pick.

### Root cause of the coverage gap

The shared FIR files contain java-direct-specific branches that fire only when
`JavaClassifierType.classifier == null` (i.e. the type points outside java-direct's
source index — JDK, library binaries, sibling source files not yet indexed at the
time of access). The java-direct test data in
`testData/codegen/box{,Jvm}` and `testData/diagnostics/...` overwhelmingly references
classes that ARE in the same `// FILE:` group, so java-direct resolves their classifier
locally and the new branches never run. Real-world Kotlin modules
(`KotlinFullPipelineTestsGenerated`) compile a single Java source set that references
many types from JARs on the classpath — that is where `classifier == null` is the rule
rather than the exception.

Empirical evidence: `analysis-api-impl-base` failed with
`MISSING_DEPENDENCY_CLASS: Cannot access class 'List'` on a Kotlin call to a Java
method whose return type is `java.util.List<String>` (star-imported in `JdkClassFinder.java`).
The classifier is null in java-direct's model; the reverted FIR `null` branch
collapses to `ClassId.topLevel(FqName(classifierQualifiedName))` and drops every
type argument, raw-type inference, nested-FQN split, and inherited-inner-class lookup.

### Changes

Added a new test data directory `compiler/testData/diagnostics/tests/jvm/javaDirectGap/`
with 8 phased/diagnostic tests targeting individual shared-FIR branches:

| File | Targets | Status with reverted FIR |
|------|---------|--------------------------|
| `sealedJavaCrossFilePermits.kt` | `FirJavaFacade.setSealedClassInheritors` cross-file `permits` (classifier == null branch) | **FAILS** — `NO_ELSE_IN_WHEN` because inheritors aren't registered |
| `nonAbstractSealedJava.kt` | `FirJavaFacade.isJavaNonAbstractSealed` flag | passes (path not exercised by current Kotlin code) |
| `javaRecordExplicitCanonicalConstructor.kt` | `FirJavaFacade.isCanonicalRecordConstructorForSource` (source-based finder) | passes (test infra still uses javac for record bytecode) |
| `javaConstFieldFromKotlinTopLevel.kt` | `FirJavaFacade.lazyInitializer` cross-language const callback | passes (annotation arg path not strict enough) |
| `javaUtilStarImportList.kt` | `JavaTypeConversion` null-classifier raw/type-arg path for `java.util.*` star-import | passes (test infra resolves via binary classpath) |
| `dottedJdkNestedClassFqn.kt` | `JavaTypeConversion.findClassIdByFqNameString` for `java.util.Map.Entry` | passes (binary classpath fallback) |
| `inheritedInnerFromKotlinSupertype.kt` | `JavaResolutionContext.resolveFromLocalScope` inherited-inner walk | passes (java-direct's own inheritance walk handles it) |
| `javaTypeUseAnnotation.kt` | `JavaTypeConversion.filterTypeUseAnnotations` callback | passes (filtering not observable in this scenario) |

The first one — cross-file sealed permits — is a confirmed regression catcher: it
fails today with the reverted FIR code, and will turn green once the
`setSealedClassInheritors` branch handling `classifier == null` is restored.

### Files Modified

| File | Change |
|------|--------|
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/sealedJavaCrossFilePermits.kt` | New: 3 sibling Java sources with `sealed permits`, plus Kotlin `when` |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/nonAbstractSealedJava.kt` | New: non-abstract sealed Java class |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/javaRecordExplicitCanonicalConstructor.kt` | New: Java record with explicit canonical constructor |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/javaConstFieldFromKotlinTopLevel.kt` | New: Java field initialized via `KConstsKt.FOO` |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/javaUtilStarImportList.kt` | New: Java star-import of `java.util.*`, `List<String>` and `Map.Entry` round trip |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/dottedJdkNestedClassFqn.kt` | New: Java method using `java.util.Map.Entry<...>` via dotted FQN |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/inheritedInnerFromKotlinSupertype.kt` | New: Java class extending Kotlin class, referencing inherited inner by simple name |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/javaTypeUseAnnotation.kt` | New: Java method with `@Target(TYPE_USE)` annotation on parameter and return |

The new tests are auto-picked up by `JavaUsingAstPhasedTestGenerated` (under
`Tests > Jvm > JavaDirectGap`) and by the PSI phased runner (which currently
ignores them — they're additional coverage for both).

### Test Results

`./gradlew :kotlin-java-direct:test --tests "*JavaDirectGap*"` — 8 tests run, 7 pass,
1 fails (`testSealedJavaCrossFilePermits`, as designed to catch the regression).

### Key Learnings

- **Test-data filter is necessary but not sufficient.** Including a file based on
  presence of `// FILE: *.java` matches the right shape but doesn't guarantee the
  scenarios reach java-direct-specific FIR branches. The dominant case in test data
  is "all referenced Java types live in sibling `// FILE:` blocks", which keeps
  classifier non-null and routes through the well-trodden `JavaClass` branch.
- **Cross-source-file references inside one module** (java-direct's "classifier == null"
  case) is the gap: Sub1 referenced from Base.java when both are in the same source
  set but processed at different times. The new sealed-permits test exercises exactly
  that.
- **Some scenarios that *should* fail with the reverted FIR don't, in our test infra.**
  Examples (records, type-use annotations, star-import generics) appear handled by
  binary-classpath fallback or by the test framework's javac step; they need a
  modularized-tests-style two-module setup to force binary loading. This is a known
  follow-up — the failing test plus the placeholder tests are still useful as
  documentation of the intended scenarios.
- **`MISSING_DEPENDENCY_CLASS` and `NO_ELSE_IN_WHEN` are good signals.** Both fire
  late in the FIR pipeline once a type's symbol can't be located; phased diagnostic
  tests surface them as `IllegalStateException` from the
  `NoFirCompilationErrorsHandler`. Watch for these strings when triaging future
  shared-FIR regressions.

### Follow-ups (not in this iteration)

- Lift `boxModernJdk/testsWithJava17/sealed` and `boxModernJdk/testsWithJava17/records`
  into `JavaUsingAstBoxTestGenerated` (currently excluded from the test data roots
  in `compiler/java-direct/testFixtures/.../TestGenerator.kt`). Sealed and record
  tests there have inline Java FILE blocks but go through a JDK-17-specific code
  path the java-direct generator doesn't currently cover.
- Investigate why the 4 placeholder tests don't trigger the reverted-state failure
  in our infra. If they truly can't, consider a small two-module fixture where the
  Java side is compiled to bytecode and re-fed to a second module — that mimics
  the real-world classpath scenario the modularized tests exercise.

---

## Post-refactoring review: readability cosmetics — 2026-04-22

### Overview

Independent code review (`implDocs/reviews/r1.md`) cross-checked against the completed
Phases A-E and Phase B regression reversals. Six low-risk readability items from the
review's suggestions 2-7 were applied.

### Changes

- **Trim `rawTypeNameParts` KDoc** (`JavaTypeOverAst.kt`) — replaced the inline "83%"
  measurement detail with a concise one-liner; the data lives in
  `archive/MEASUREMENTS_2026_04_22.md` §7.4.
- **Trim `CacheHelpers.kt` file KDoc** — consolidated the "why not `by lazy(PUBLICATION)`"
  and "why not explicit backing fields" rationale from 31 lines to 19, preserving the key
  insight (24B+8B per delegate × 200K instances).
- **Rename `findInPhase1JavaModel` → `walkJavaSourceSupertypes`** and
  **`findInPhase2ClassIdWalk` → `walkBinarySupertypes`** (`JavaInheritedMemberResolver.kt`)
  — the old "Phase 1 / Phase 2" names read as compilation phases; the new names describe
  the data source (Java model vs binary/Kotlin supertypes). Updated all KDoc references.
- **Rename `AggregatedInheritedInnerClassesHolder` → `InheritedInnerCache`** and
  **`aggregatedInheritedInnerClassesHolder` → `inheritedInnerCache`**
  (`JavaResolutionContext.kt`) — shorter name for the `@Volatile`-wrapped cache holder.
- **Add comment on `JavaAnnotationOverAst.resolve()`** — one-liner explaining that
  resolution is callback-based via `resolveAnnotation()`.
- **Trim static-inner-class context comment** (`JavaClassOverAst.kt:162-167`) — removed the
  3-line mirror explanation of the `else` branch; the first two lines already explain the
  non-static case and the code is self-evident.

### Test Results

No behavioral changes — renames and comment edits only. Compilation verified via IDE.

### Files Modified

| File | Change | Lines |
|------|--------|-------|
| `JavaTypeOverAst.kt` | Trim `rawTypeNameParts` KDoc | −1 |
| `CacheHelpers.kt` | Trim file-level KDoc | −12 |
| `JavaInheritedMemberResolver.kt` | Rename two methods + update KDoc | ~0 (rename) |
| `JavaResolutionContext.kt` | Rename class + field | ~0 (rename) |
| `JavaAnnotationOverAst.kt` | Add one-liner on `resolve()` | +1 |
| `JavaClassOverAst.kt` | Trim inner-class context comment | −3 |
| **Net** | | **−15 lines** |

### Key Learnings

- **Review after refactoring catches different things than review before.** The original
  review (r1.md) independently flagged `filterTypeUseAnnotations` caching and
  `resolveSimpleNameToClassIdImpl` extraction — both of which had already been tried and
  reverted (P1 and R12+O10). Cross-checking against the refactoring history before acting
  avoided re-introducing known regressions.
- **Method names that describe mechanism ("Phase 1/2") age worse than names that describe
  data source ("JavaSource/Binary").** The Phase 1/Phase 2 naming was clear when the two
  methods were freshly extracted in B.3 but confusing to a fresh reader.

---

## Archived Iteration History

All entries from the 2026-04-17 through 2026-04-22 refactoring work (Phases A-E of
`REFACTORING_PLAN_2026_04_21.md`) have been archived to:

- `implDocs/archive/ITERATION_RESULTS_2026_04_22.md` — full log with Phase B regression
  investigation, Phase C measurements, Phase D implementation, Phase E cleanup
- `implDocs/archive/REFACTORING_PLAN_2026_04_21.md` — the 5-phase plan (A-E)
- `implDocs/archive/MEASUREMENTS_2026_04_22.md` — Phase C measurement data (8 hypotheses,
  3 corpora, corrected classloader-isolation methodology)
- `implDocs/archive/REFACTORING_STEPS_2026_04_17_DETAILS.md` — earlier refactoring steps 1.3-3.6
- `implDocs/archive/LAZY_PACKAGE_INDEXING_PLAN_2026_04_21.md` — lazy per-package indexing design (implemented)

### Open items carried forward

- **Context-level `tryResolve` cache** (`PERFORMANCE_REVIEW_2026-04-20.md` §2 #6) — deferred
  with a recorded correctness argument. Only revisit if profiling shows `resolve()` as a
  measurable bottleneck.
