# Java-Direct: Iteration Results Log

**Current status**: 1178/1178 box + 1513/1513 phased (2793/2793, 100%). Phased and
box generators now actually route `// FILE: *.java` blocks through java-direct AST;
prior numbers were against PSI loading (see 2026-04-28 entry).

**Last Updated**: 2026-04-28 (test framework wired through java-direct)

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
