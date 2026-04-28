# Java-Direct: Iteration Results Log

**Current status**: 1168/1168 box + 1454/1456 phased (2679/2681, 99.9%), 2 known won't-fix.

**Last Updated**: 2026-04-22 (Phases A-E complete, archive reset)

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
