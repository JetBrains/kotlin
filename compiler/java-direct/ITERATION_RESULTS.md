# Java-Direct: Iteration Results Log

**Current status**: 1178/1178 box + 1513/1513 phased (2793/2793, 100%).

**Last Updated**: 2026-05-28 (Stage 2 §6.2 — narrow `JavaSymbolProvider`
to Java *source* classes via new source-only probes on `JavaClassFinder`
(`isInSourceIndex`, `hasPackageInSources`, `sourceClassNamesInPackage`),
overridden in `CombinedJavaClassFinder` to delegate to the source half
only and surfaced through `FirJavaFacade`. `JavaSymbolProvider`'s class-id
gate moves from `javaFacade.hasTopLevelClassOf(classId)` (source∪binary)
to `javaFacade.isInSourceIndex(classId)`; `hasPackage` routes to
`hasPackageInSources`; the symbol-names provider routes to
`sourceClassNamesInPackage`. For single-side finders (PSI's
`JavaClassFinderImpl`, plain `BinaryJavaClassFinder`, etc.) the new
probes have default implementations that coincide with the existing
methods — narrowing is a *no-op* there, confirmed by the PSI regression
gate (10787/10787 green). Binary-Java visibility for the three §6.1
callers (`FirJvmConflictsChecker`, `FirDirectJavaActualDeclarationExtractor`,
Lombok `AbstractBuilderGenerator`) is deferred to §6.3 — first attempt
to add a binary composite-walk fallback in the §6.1 helper crossed
session boundaries (dependency-module binaries leaked into main session
and triggered spurious CLASSIFIER_REDECLARATION in
`testVarargClassParameterOnJavaClass`) *and* crashed on local class ids
with `IllegalArgumentException: Local <local>/C should never be used to
find its corresponding classifier`; that walk was reverted and the
helper stays a thin `javaSymbolProvider?.getClassLikeSymbolByClassId(...)`
delegate. Each of the three callers is OK with source-only behavior on
the java-direct path: `FirDirectJavaActualDeclarationExtractor` already
filters `FirDeclarationOrigin.Java.Source`; Lombok `AbstractBuilderGenerator`
discovers Lombok-annotated *source* classes; `FirJvmConflictsChecker`
historically also flagged Kotlin vs binary-Java redeclarations but the
java-direct fixture set has no such case — §6.3 will restore that
coverage via a targeted (not-naive-composite) deserializer lookup.
Verification: `:compiler:java-direct:test --tests
"JavaUsingAst{Phased,Box}TestGenerated" --rerun` →
**2701/2701 green** (0 failures, 0 errors aggregated across
`TEST-*JavaUsingAst*.xml`); `:compiler:fir:analysis-tests:test --tests
"PhasedJvmDiagnosticLightTreeTestGenerated.*" --rerun` →
**10787/10787 green** (0 failures, 0 errors aggregated across the PSI
regression suite). Files: `core/compiler.common.jvm/src/.../load/java/JavaClassFinder.kt`
(+30 LoC — three new methods on the interface, all with safe defaults
preserving current behavior for single-side finders),
`compiler/java-direct/src/.../CombinedJavaClassFinder.kt`
(+14 LoC — three overrides delegating to `sourceFinder`),
`compiler/fir/fir-jvm/src/.../FirJavaFacade.kt`
(+19 LoC — three facade-surface methods over the new probes),
`compiler/fir/fir-jvm/src/.../java/JavaSymbolProvider.kt`
(+9 / −4 LoC — gate switch + helper KDoc rewrite).
Net: 4 source files, ≈+72/−4 LoC. No public Java-model interface
touched; the new `JavaClassFinder` methods have safe defaults so
existing impls don't need to change. Previous: 2026-05-28 (Stage 2 §6.1 —
indirect `javaSymbolProvider`
call-site audit, first sub-step of Stage 2 = Phase 2 of
[`implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md)).
Three sites that called `session.javaSymbolProvider?.getClassLikeSymbolByClassId(...)`
directly — `FirJvmConflictsChecker`, `FirDirectJavaActualDeclarationExtractor`,
and Lombok `AbstractBuilderGenerator` (two call sites) — now route
through a new Java-targeted lookup helper
`FirSession.getJavaClassLikeSymbolByClassId(classId)` in
`compiler/fir/fir-jvm/src/.../java/JavaSymbolProvider.kt`. The design
doc's prescription to re-route via `session.symbolProvider` + an
`origin is FirDeclarationOrigin.Java` filter was tried first and
produced 12 redeclaration / actualization test failures: the composite
`FirCompositeSymbolProvider.getClassLikeSymbolByClassId` uses
`firstNotNullOfOrNull`, so when a Kotlin class shares the `ClassId`
(the entire point of these diagnostics) the Kotlin source provider
wins and the Java symbol is hidden. The helper today wraps the direct
`javaSymbolProvider?.getClassLikeSymbolByClassId(...)` call (zero
behavioral delta vs. baseline), and §6.3 will extend it to also
consult the deserializer for binary `FirDeclarationOrigin.Java.Library`
results — one place to extend, three call sites pick up the new
behavior transparently. `FirDirectJavaActualDeclarationExtractor`
keeps its `javaSymbolProvider != null` JVM-session gate in
`initializeIfNeeded` (§6.2 only narrows what the provider *returns*,
not whether it's registered) and keeps the strict `Java.Source`
origin filter on the `extract` call (only Java source-class
actualizations are valid; binary Java classes are not candidates).
Verification: `:compiler:java-direct:test --tests
"JavaUsingAst{Phased,Box}TestGenerated" --rerun` → `BUILD SUCCESSFUL`,
**2701/2701 green on this worker**, 0 failures, 0 errors aggregated
across `TEST-*JavaUsingAst*.xml`. `:kotlin-lombok-compiler-plugin:test
--rerun` showed two failures
(`FirLightTreeBlackBoxCodegenTestForLombokGenerated.test*ConstructorStatic`)
which were **confirmed as baseline failures, not §6.1 regressions**:
`git stash`ing the four touched source files and re-running the same
two tests reproduced the same 2/2 failure shape. The failing fixtures
exercise `@*ArgsConstructor(staticName=...)`, handled by
`AbstractConstructorGeneratorPart.kt` — a Lombok class not touched
by §6.1; failure is `INVISIBLE_REFERENCE: Cannot access
'constructor(...)': it is private in 'ConstructorExample'`, i.e. the
static factory generator is not emitting the `staticName` factory on
the green tree (pre-existing on `rr/ic/direct-java`). PSI regression
suite not re-run for this audit step — the helper introduction is
purely additive and the three call-site rewrites inline the exact
same `javaSymbolProvider?.getClassLikeSymbolByClassId(...)` chain;
PSI suite will be required after §6.2 / §6.3 (those touch
`JavaSymbolProvider` / `JvmClassFileBasedSymbolProvider` semantics).
Files: `compiler/fir/fir-jvm/src/.../java/JavaSymbolProvider.kt`
(+22 LoC, new extension fn + KDoc),
`compiler/fir/checkers/checkers.jvm/src/.../FirJvmConflictsChecker.kt`,
`compiler/fir/fir2ir/jvm-backend/src/.../FirDirectJavaActualDeclarationExtractor.kt`,
`plugins/lombok/lombok.k2/src/.../AbstractBuilderGenerator.kt`. Net:
4 source files, ≈+30/−15 LoC. No public Java-model interface touched.
Follow-up filed in this entry: doc refresh for
`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` §2.4.4 and
`DIRECT_INJECTION_STAGE_1_2026_05_20.md` §6.1 to point at the helper
pattern (their original prescription is wrong).
Previous: 2026-05-26 (Same-day later #7: reverted the iteration-40
hunk in `core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaLoading.kt`.
The `isMethodWithOneObjectParameter` fallback arm that checked
`type.classifierQualifiedName == "java.lang.Object" || == "Object"` —
added in iteration 40 as a band-aid for the now-retired java-direct
*FIR-callback-based* classifier resolution path — was dead code under
the current synchronous in-resolver `JavaClassifierTypeOverAst.computeClassifier()`
(any unqualified `Object` is bound to `java.lang.Object` via
`resolveFromJavaLang` rank-5 before `isObjectMethodInInterface` is ever
called) *and* a JLS §6.4.1 violation in the contrived corner case it
claimed to handle (a nested or imported `Object` would shadow
`java.lang.Object` per JLS 6.4.1 — the previous-issue review surfaced
this with `public class SomeJavaClass { public Object foo() { … }
static class Object {} }`). Function now reads:
`val classifier = type.classifier as? JavaClass ?: return false; val
classFqName = classifier.fqName; return classFqName != null &&
classFqName.asString() == "java.lang.Object"` — matches the pre-iteration-40
shape exactly. Stale comment ("e.g., java-direct before FIR resolves
the type via callback") removed alongside the arm, since the callback
layer was retired in later iterations (cf. `JavaTypeOverAst.kt:31`,
`JavaExternalConstResolver.kt:35`, `JavaAnnotationOverAst.kt:125`).
Verification: `:compiler:java-direct:test
--tests "JavaUsingAst{Phased,Box}TestGenerated"` → `BUILD SUCCESSFUL`,
**2703/2703 green on this worker**, zero `FAILED`/`FAILURE` lines in
`$JD_TMP/jd_full.txt`. Net source diff: 1 file, +2/−7 LoC (1 production
source file: `javaLoading.kt`). No test fixtures touched, no other
production files touched. The behavioral delta is zero on the happy
path (`classifier is JavaClass` branch already binds `"Object"` to
`java.lang.Object` synchronously); the safety net we lose was a
JLS-incorrect guess for an effectively-unreachable null-classifier
state, so the removal is a small correctness improvement, not a
regression.
Previous: 2026-05-26 (Same-day later #6: added three regression
test fixtures in `compiler/testData/diagnostics/tests/jvm/javaDirect/`
pinning the Option-C JLS 6.4.1 import-shadowing behavior — the iteration
that landed #5 noted that "the dispatcher now has the right shape for
[the regression tests] but the tests themselves are not yet added", and
this iteration closes that follow-up. Three fixtures landed:
`staticImportOfMethodNotAType.kt` (rank-4 static-single arm — a
`import static a.C.foo;` where `foo` is a *method* and a same-package
class `b.foo` exists; pins that `tryResolve` cleanly falls through the
static-single bucket and the same-package class wins),
`memberTypeShadowsStaticTypeImport.kt` (rank-2 member-type vs rank-4
static-single-type — `import static a.C.Inner;` where the enclosing
`MyJavaClass` also declares a static nested `Inner`; pins
`resolveFromLocalScope` precedence and that the static-single bucket
does *not* get a rank-1/2 short-circuit),
`staticStarImportsNestedClass.kt` (rank-7 static-on-demand arm —
`import static b.C.*;` brings in nested `b.C.X`; pins the
`resolveFromStaticStarImports` step's `outerClassId.createNestedClassId(name)`
shape). The third fixture was originally drafted as a
`typeStarShadowsStaticStar.kt` rank-6-vs-rank-7 collision test, but
that shape was rejected at the `javac` Java-compilation step with
`error: reference to X is ambiguous, both class b.C.X in b.C and
class a.X in a match` — javac's strict interpretation of JLS 6.4.1
treats type-on-demand vs static-on-demand as equal-precedence in
collision (technically contra JLS 6.4.1 "A static-import-on-demand
declaration never causes any other declaration to be shadowed", but
`javac`'s actual behavior is what determines whether the test fixture
survives the Java compile phase). The collision shape is therefore
not testable end-to-end and the fixture was narrowed to pin only the
static-on-demand path. Verification:
`:compiler:java-direct:test --tests "JavaUsingAst{Phased,Box}TestGenerated"`
→ `BUILD SUCCESSFUL`, **2703/2703 green on this worker** (= 2700
baseline + 3 new fixtures), zero `FAILED`/`FAILURE` lines in
`$JD_TMP/jd_full.txt`. Net source diff: 0 source files; 3 new test
fixtures (~85 LoC total) + 1 regenerated `JavaUsingAstPhasedTestGenerated.java`
test class. No production code touched.
Previous: 2026-05-26 (Same-day later #5: Option C — full JLS
6.4.1 / 7.5 static-vs-type import split. `JavaImportResolver.extractImports`
now returns a four-bucket `JavaImports` holder (`simpleTypeImports`,
`staticSingleImports`, `typeStarImports`, `staticStarImports`) and
`JavaResolutionContext.resolveSimpleNameToClassIdImpl` was extended from a
6-step to a 7-step dispatcher: step 3 split into 3a single-type-import +
3b single-static-import (both JLS rank 4; static-single probed via
`resolveAsClassId` so a static type import resolves correctly through
its outer-class FqName); step 6 split into 6 type-import-on-demand
(JLS rank 6, `ClassId(pkg, name)` with the historical class-level
`import a.D.*;` fallback preserved for `testImportThriceNestedClass` /
`testNestedAndTopLevelClassClash`) and 7 static-import-on-demand
(JLS rank 7, strictly lower, `outerClassId.createNestedClassId(name)`
shape). `JavaSupertypeGraph.resolveSupertypeReference` mirrors the
ordering and now emits split candidates for static-on-demand outers.
`JavaEnumValueAnnotationArgumentOverAst.staticImportResolution` now
calls a new `JavaResolutionContext.getStaticImport` instead of the
conflated `getSimpleImport`, so a non-static type import can no longer
be misinterpreted as an implicit `Outer.member` enum-entry binding.
Two regressions during development pinned the class-level fallback in
the type-star step (Kotlin compiler historically accepts `import a.D.*;`
where `a.D` is a class — strictly illegal Java but the test suite
depends on it); restored that fallback alongside the static-star rank-7
step. Verification: `:compiler:java-direct:test
--tests "JavaUsingAst{Phased,Box}TestGenerated"` → `BUILD SUCCESSFUL`,
**2700/2700 green on this worker**, zero `FAILED`/`FAILURE` lines in
`$JD_TMP/jd_test_2.txt`. Net source diff: 5 files (`JavaImportResolver.kt`,
`CompilationUnitContext.kt`, `JavaResolutionContext.kt`,
`JavaSupertypeGraph.kt`, `JavaAnnotationOverAst.kt`); no public
Java-model interface touched.
Previous: 2026-05-26 (Same-day later #4: JLS 6.4.1 import-precedence
fix in `JavaResolutionContext.resolveSimpleNameToClassIdImpl`.
Code-review question: *"Is it actually correct that we prefer explicitly
imported classes to nested defined closer to us? `import java.util.List;
public class MyJavaClass { public static class List<F> {} … }`"*. Answer:
no — JLS §6.4.1 says a member type of the enclosing class shadows
single-type imports, and same-compilation-unit top-level types do too;
the old order (`resolveFromExplicitImport` first) violated this. Fixed
ordering, now JLS-correct: (1) member types of the enclosing class —
`resolveFromLocalScope`; (2) **same-compilation-unit** top-level types
— new `resolveFromSameCompilationUnit` driven by
`JavaScopeForContext.sameFileTopLevelClassProvider`; (3) single-type
imports — `resolveFromExplicitImport`; (4) **other-file same-package**
top-level types — `resolveFromSamePackage` (kept where the JLS puts it:
the import shadows cross-file same-package types per JLS §6.4.1); (5)
`java.lang.*`; (6) star imports. The same-compilation-unit vs cross-file
same-package distinction was previously invisible to the dispatcher
because both share `ClassId(packageFqName, simpleName)` — fixed by
gating Step 2 on `sameFileTopLevelClassProvider`. Promoted that
provider from `private val` to `val` on `JavaScopeForContext` so the
dispatcher can read it. Two prior failures during development —
`testCurrentPackageAndExplicitImport` and `testJavaSupertypeNameDisambiguation`
— actually exercised the cross-file same-package shadowing case and
ensured the fix matches the JLS direction: import shadows cross-file
same-package, import is *shadowed by* same-file top-level. Final
verification: 2793/2793 green. Net diff: 2 files, +99/−12 LoC (mostly
KDoc expansion across the six step helpers + 1 new helper).
Previous: 2026-05-26 (Same-day later #3: pure-rename refactor —
`JavaResolutionContext.resolveNestedClassToClassId` /
`resolveNestedClassToClassIdFromParts` renamed to
`resolveQualifiedNameToClassId` / `resolveQualifiedNameToClassIdFromParts`
to honestly describe what they do.)
Previous: 2026-05-26 (Same-day later #2: Option A refactor —
collapsed the duplicate `inheritedMemberResolver` reference that was
held by both `CompilationUnitContext` and `JavaScopeForContext`.
`CompilationUnitContext.inheritedMemberResolver` is now the single
source of truth on the resolver side (the resolver is per-compilation-unit
and scope-invariant, matching the class-level KDoc on
`CompilationUnitContext`: *"per-compilation-unit immutable data shared
across all scope variants"*). The `inheritedMemberResolver` ctor
parameter and field on `JavaScopeForContext` are gone; the sole reader
(step 3 of `findClassInCurrentScope`) now takes the resolver as a
method parameter, and the one external caller —
`JavaResolutionContext.findClassInCurrentScope` — passes
`unitContext.inheritedMemberResolver`. The three `with*` factory
copies on `JavaScopeForContext` and the factory call in
`JavaResolutionContext.create` no longer thread the resolver through.
Net diff +7/−7 LoC across 2 files; no public Java-model interface
touched; no behavior change.
Previous: 2026-05-26 (Same-day later #1: dropped the dead
`extraAnnotations` parameter from `createJavaType` and its three
private helpers — `tryCreateArrayOrVarargFromTypeNode`,
`createWildcardType`, `createClassifierOrPrimitive`. Every caller
(external + internal recursive) had been passing the default
`emptyList()` since Iteration 22 split the original `@NotNull`-carrying
role off into `memberAnnotations`; the only real producer of
TYPE-position annotations — the type-argument sibling-`ANNOTATION`
harvest for `List<@NotNull Integer>` — is computed locally inside
`createClassifierOrPrimitive` and passes straight to the
`JavaClassifierTypeOverAst` constructor, never round-tripping through
`createJavaType`. The outermost-array-dim `if (i == dims - 1) extraAnnotations`
branch was also dead (`extraAnnotations` was always empty); the
repeat-loop now just builds `JavaArrayTypeOverAst(typeNode, tree,
resolutionContext, result)`. Constructor parameter on the
`JavaTypeOverAst` subclasses preserved — it is still consumed by the
local `typeNodeAnnotations` path in `createClassifierOrPrimitive`. Net
diff +15/−21 LoC on `JavaTypeOverAst.kt`; no public Java-model
interface touched; no behavior change.)
Earlier: 2026-05-26 — collapsed duplicate `containingClass` field
between `JavaResolutionContext` and `JavaScopeForContext`:
`JavaScopeForContext.containingClass` is now the single source of truth on
the resolver side; the `containingClass` constructor parameter + field on
`JavaResolutionContext` were removed and the three readers
(`getAggregatedInheritedInnerClasses` outer-chain walk,
`resolveInheritedInnerClassToClassId` pass-through, `getContainingClassIds`)
rewritten to read `scopeResolver.containingClass`. `JavaClassOverAst.outerClass`
intentionally left untouched — it implements the public `JavaClass.outerClass`
contract consumed by FIR (Non-Negotiable Rule §7). Net diff −4 LoC across
2 files; no behavior change.
Earlier: 2026-05-25 — shared CLI diagnostic
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

- **2026-05-26 (same-day later #7)** — Reverted the iteration-40 hunk in
  `core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaLoading.kt`.
  The `isMethodWithOneObjectParameter` fallback arm
  (`type.classifierQualifiedName == "java.lang.Object" || == "Object"`)
  was a band-aid added in iteration 40 for the now-retired java-direct
  FIR-callback-based classifier resolution: at that time
  `JavaClassifierTypeOverAst` produced a `null` classifier for
  unqualified `Object` and deferred binding to `java.lang.Object` to a
  FIR-side callback, so any code reading the type before the callback
  fired had only the raw text `"Object"` to work with — and
  `isObjectMethodInInterface` (called early in `FirJavaFacade.processClassMembers`)
  was on that pre-callback path. That callback layer was retired in
  later iterations (cf. comment in `JavaTypeOverAst.kt:31` *"FIR-side
  `JavaTypeWithExternalAnnotationFiltering` callback bridge is no
  longer needed"*, and matching notes in
  `JavaExternalConstResolver.kt:35` and `JavaAnnotationOverAst.kt:125`);
  `JavaClassifierTypeOverAst.computeClassifier()` now runs the full
  simple-name resolution synchronously and bottoms out at the
  `resolveFromJavaLang` rank-5 step (`tryResolve(ClassId(java.lang,
  Object))`) for unqualified `Object`, so the classifier is bound to
  `java.lang.Object` *before* `isObjectMethodInInterface` is ever
  reached → the `classifier is JavaClass` branch already handles the
  case the iteration-40 fallback was added for. The fallback is
  therefore dead code on the happy path. It is also JLS-incorrect in
  the corner case the previous-issue review surfaced (`public class
  SomeJavaClass { public Object foo() { … } static class Object {} }`
  and `import com.foo.Object;` — both shadow `java.lang.Object` per
  JLS 6.4.1, but the bare-`"Object"` string match would silently
  equate them with `java.lang.Object`), so removing the arm is a
  small correctness improvement, not a regression. Function shape
  reverted to pre-iteration-40: `val classifier = type.classifier
  as? JavaClass ?: return false; val classFqName = classifier.fqName;
  return classFqName != null && classFqName.asString() ==
  "java.lang.Object"`. Stale 3-line comment removed alongside.
  Verification: `:compiler:java-direct:test
  --tests "JavaUsingAst{Phased,Box}TestGenerated"` → `BUILD
  SUCCESSFUL`, **2703/2703 green on this worker**, zero
  `FAILED`/`FAILURE` lines in `$JD_TMP/jd_full.txt`. Behavioral delta
  on the happy path: zero (the `is JavaClass` branch already binds
  `"Object"` synchronously). Net source diff: 1 file, +2/−7 LoC; no
  test fixtures touched, no other production files touched. The file
  goes back to being PSI-agnostic infrastructure with no
  java-direct-specific knobs.

- **2026-05-26 (same-day later #6)** — Added three regression test
  fixtures in `compiler/testData/diagnostics/tests/jvm/javaDirect/`
  pinning the Option-C JLS 6.4.1 import-shadowing behavior landed in
  #5. The #5 entry explicitly noted the regression tests "remain
  follow-ups: the dispatcher now has the right shape for them but the
  tests themselves are not yet added" — this iteration closes that
  follow-up. User selected `compiler/testData/diagnostics/tests/jvm/javaDirect/`
  (where the only prior fixture was `simpleHierarchy.kt`) as the
  destination, not the alternative `compiler/testData/diagnostics/tests/javac/imports/`
  proposed in the earlier write-up — the `jvm/javaDirect/` directory is
  routed through the `JavaUsingAstPhasedTestGenerated`'s test-data
  scan (via `compiler/java-direct/testFixtures/.../TestGenerator.kt`)
  on the existing `testData/diagnostics/tests` model root plus the
  `additionalFileFilter` that keeps only fixtures containing a
  `// FILE: *.java` block, so the new fixtures are auto-picked up by
  `JavaUsingAstPhasedTestGenerated$Tests$Jvm$JavaDirect` after a
  `:compiler:java-direct:generateTests` regeneration. Three fixtures
  landed: (a) `staticImportOfMethodNotAType.kt` — `import static a.C.foo;`
  where `foo` is a *static method* and a same-package class `b.foo`
  exists; pins that the static-single bucket's `tryResolve` cleanly
  falls through (`a.C.foo` is not a class) and rank-5 same-package
  resolution wins, returning `b.foo`. (b) `memberTypeShadowsStaticTypeImport.kt`
  — `import static a.C.Inner;` where the enclosing `MyJavaClass` also
  declares a nested `Inner`; pins that `resolveFromLocalScope` (rank 1)
  precedes the static-single bucket (rank 4) and the member type wins,
  so `make()` returns the enclosing `MyJavaClass.Inner`, not `a.C.Inner`.
  (c) `staticStarImportsNestedClass.kt` — `import static b.C.*;` brings
  in nested `b.C.X`; pins the rank-7
  `resolveFromStaticStarImports`/`outerClassId.createNestedClassId(name)`
  shape end-to-end. The original draft of (c) was a
  `typeStarShadowsStaticStar.kt` rank-6-vs-rank-7 collision test
  (`import a.*; import static b.C.*;` where both `a` and `b.C` expose
  a class `X`); that shape was rejected at the Java-compile step with
  `error: reference to X is ambiguous, both class b.C.X in b.C and
  class a.X in a match` — `javac` treats type-on-demand vs
  static-on-demand at equal precedence in collision (technically contra
  JLS 6.4.1 "static-import-on-demand never causes any other declaration
  to be shadowed", but `javac`'s actual behavior determines whether the
  fixture survives the Java compile phase). The collision shape is
  therefore not testable end-to-end via this harness; the fixture was
  narrowed to pin only the static-on-demand path. Workflow:
  `:compiler:java-direct:generateTests` to register the three new
  `@TestMetadata`-annotated methods on `JavaUsingAstPhasedTestGenerated$Tests$Jvm$JavaDirect`
  → `:compiler:java-direct:test --tests "JavaUsingAst{Phased,Box}TestGenerated"`
  → `BUILD SUCCESSFUL`, **2703/2703 green on this worker** (= 2700
  baseline + 3 new fixtures), zero `FAILED`/`FAILURE` lines in
  `$JD_TMP/jd_full.txt`. Net source diff: 0 source files; 3 new test
  fixtures (~85 LoC total) + 1 regenerated
  `JavaUsingAstPhasedTestGenerated.java` test class. No production
  code touched.

- **2026-05-26 (same-day later #5)** — Option C of the prior static-vs-type
  import analysis: full JLS 6.4.1 / 7.5 split with four parallel buckets and
  a 7-step dispatcher. Motivated by a code-review question that pointed
  out the line `simpleImports.putIfAbsent(simpleName, FqName(fqName))` in
  `JavaImportResolver.kt` conflated static and non-static imports, which
  per JLS work differently. The prior write-up enumerated three options;
  Option A landed an unspoken precursor (data-model split), Option B was
  a semantic filter via `tryResolve` caching, and Option C was the full
  7-rank JLS dispatcher. This iteration implements Option C directly.

  **Data model.** `JavaImportResolver.extractImports` now returns a new
  `JavaImports` holder with four buckets keyed by JLS 7.5 production:
  `simpleTypeImports` (`import a.b.C;`), `staticSingleImports`
  (`import static a.b.C.X;`), `typeStarImports` (`import a.b.*;`,
  values are *packages*), `staticStarImports`
  (`import static a.b.C.*;`, values are *outer-class* FqNames — not
  packages). `CompilationUnitContext` carries `imports: JavaImports`
  instead of the old `(simpleImports, starImports)` pair.

  **Dispatcher.** `JavaResolutionContext.resolveSimpleNameToClassIdImpl`
  is now a 7-step chain mirroring JLS 6.4.1 shadowing ranks: (1)
  `resolveFromLocalScope` — member types of the enclosing class; (2)
  `resolveFromSameCompilationUnit` — same-file top-level; (3a)
  `resolveFromExplicitImport` — `simpleTypeImports`, rank 4; (3b) **new**
  `resolveFromStaticSingleImport` — `staticSingleImports`, rank 4 (probed
  via `resolveAsClassId` so a static type import resolves through its
  outer-class FqName; method/field imports drop out cleanly when
  `tryResolve` returns false); (4) `resolveFromSamePackage` — cross-file
  same-package, rank 5; (5) `resolveFromJavaLang`; (6)
  `resolveFromTypeStarImports` — type-on-demand, rank 6 (`ClassId(pkg,
  name)` plus the historical class-level `import a.D.*;` fallback that
  the Kotlin compiler accepts as if `static` — strictly illegal Java but
  required by `testImportThriceNestedClass` and
  `testNestedAndTopLevelClassClash`); (7) **new**
  `resolveFromStaticStarImports` — static-on-demand, rank 7 (strictly
  lower than rank 6 per JLS 6.4.1), nested-class `ClassId` shape
  (`outerClassId.createNestedClassId(name)`).

  **Consumers rewired.** `JavaSupertypeGraph.resolveSupertypeReference`
  takes `JavaImports` and mirrors the new rank order: simple-type then
  static-single (`fqNameSplitCandidates`), then type-star (with
  same-file priority), then static-star (emits split candidates of
  `outerFqName.child(name)` for the FIR symbol provider to disambiguate).
  `JavaResolutionContext` accessors: `getSimpleImport(name)` becomes a
  type-first-then-static unified accessor for the model-side callers
  (`JavaTypeOverAst`, `JavaMemberOverAst`, `JavaAnnotationOverAst`);
  new `getStaticImport(name)` consults only the static-single bucket
  and is wired into
  `JavaEnumValueAnnotationArgumentOverAst.staticImportResolution`, so
  a non-static type import can no longer be misinterpreted as an
  implicit `Outer.member` enum-entry binding (the conceptual bug the
  code-review question flagged for `getSimpleImport`).
  `getFirstStarImportCandidate` now reads only `typeStarImports`
  (static-star would need a nested shape, which this best-effort
  helper is not for); `getImports()` returns `JavaImports` directly.

  **Regressions during development.** The naïve "drop the class-level
  fallback from `resolveFromStarImports` and trust the rank-7
  `resolveFromStaticStarImports` to cover all class-level cases"
  broke `testImportThriceNestedClass` and
  `testNestedAndTopLevelClassClash` (`$JD_TMP/jd_test.txt`,
  `2700 tests completed, 2 failed`). Root cause: those tests use
  *non-static* `import a.D.*;` (strictly illegal Java but
  Kotlin-compiler-permissive) and rely on the class-level fallback
  inside `resolveFromTypeStarImports`. We cannot tell parser-side
  whether the dotted prefix is a package or a class; the strictly
  static-only rank-7 step only fires for entries that came from
  `import static`. Fix: restore the class-level fallback in
  `resolveFromTypeStarImports` (probe `ClassId(pkg, name)` first;
  on miss, treat the entry as a class and probe
  `outerClassId.createNestedClassId(name)`), while keeping the
  rank-7 step in place for genuine `import static` *star* entries.

  **Verification.**
  `:compiler:java-direct:test --tests "JavaUsingAst{Phased,Box}TestGenerated"`
  → `BUILD SUCCESSFUL`, **2700/2700 green on this worker**, zero
  `FAILED` / `FAILURE` lines in `$JD_TMP/jd_test_2.txt`. (The
  worker-local suite size of 2700 vs the historical 2793 reflects
  session-level test filtering, not a regression — the same 2700 was
  the denominator in the failing earlier run.) Net source diff:
  5 files (`JavaImportResolver.kt`, `CompilationUnitContext.kt`,
  `JavaResolutionContext.kt`,
  `compiler/java-direct/src/.../util/JavaSupertypeGraph.kt`,
  `compiler/java-direct/src/.../model/JavaAnnotationOverAst.kt`); no
  public Java-model interface touched. Options B (lazy
  symbol-provider-probe caching to filter method/field static imports
  out of the type-classification path) and the additional regression
  tests called out in the prior analysis (`import static a.b.C.foo;`
  with collision; `import a.*; import static b.C.*;` rank-6 vs rank-7
  ordering) remain follow-ups: the dispatcher now has the right shape
  for them but the tests themselves are not yet added.

- **2026-05-26 (same-day later #4)** — JLS 6.4.1 import-precedence fix
  in `JavaResolutionContext.resolveSimpleNameToClassIdImpl`. Motivated
  by a code-review question: *"Is it actually correct that we prefer
  explicitly imported classes to nested defined closer to us? `import
  java.util.List; public class MyJavaClass { public static class
  List<F> {} … }`"*. Answer: no — per JLS §6.4.1 a member type of the
  enclosing class shadows single-type imports, and same-compilation-unit
  top-level types also shadow imports; the old order
  (`resolveFromExplicitImport` first) violated this. Fixed ordering,
  now JLS-correct: (1) member types of the enclosing class —
  `resolveFromLocalScope`; (2) **same-compilation-unit** top-level
  types — new `resolveFromSameCompilationUnit` driven by
  `JavaScopeForContext.sameFileTopLevelClassProvider`; (3) single-type
  imports; (4) **other-file same-package** top-level types —
  `resolveFromSamePackage` (kept where JLS puts it: the import shadows
  cross-file same-package types); (5) `java.lang.*`; (6) star imports.
  The same-compilation-unit vs cross-file same-package distinction was
  previously invisible to the dispatcher because both share
  `ClassId(packageFqName, simpleName)` — fixed by gating Step 2 on
  `sameFileTopLevelClassProvider` (promoted from `private val` to
  `val`). During development, two regressions surfaced after the naïve
  "just put `resolveFromLocalScope` first" reorder:
  `testCurrentPackageAndExplicitImport` (`b/T.java` does
  `import a.Y;` while `b/Y.java` is another file in package `b` —
  JLS says the import wins; the test expects that) and
  `testJavaSupertypeNameDisambiguation` (`Derived.java` does
  `import diff.Base;` while another file in the same root package
  also declares `Base` — same shadowing direction). Both pinned down
  that same-package needs to split into same-file (shadows import)
  vs other-file (shadowed by import). Final verification:
  `:compiler:java-direct:test --tests "JavaUsingAst{Phased,Box}TestGenerated"`
  → `BUILD SUCCESSFUL`, **2793/2793 green**, zero `FAILED`/`FAILURE`
  lines in `$JD_TMP/jd_test_6.txt`. Net source diff: 2 files,
  +99/−12 LoC (mostly KDoc expansion across the six step helpers +
  1 new helper). No public Java-model interface touched.

- **2026-05-26 (same-day later #3)** — Pure-rename refactor in
  `JavaResolutionContext.kt`:
  `resolveNestedClassToClassId(name, tryResolve)` →
  `resolveQualifiedNameToClassId(name, tryResolve)`, and its workhorse
  `resolveNestedClassToClassIdFromParts(parts, …)` →
  `resolveQualifiedNameToClassIdFromParts(parts, …)`. Motivated by a
  code-review question — "Is the name accurate?" — followed up by:
  "in Kotlin terms, it's rather `resolveQualifiedNameToClassId`?".
  Yes: the function is not nested-class-only. It implements JLS 6.5.2
  in two phases — (1) try every prefix split `Q.Id` as a nested class
  when `Q` is a class in scope (priority phase), then (2) fall back to
  the plain FQN-split fallback via `probeFqnSplits` for inputs like
  `java.util.Map`. Both phases live in the same body and both use
  `tryResolve`; the FQN fallback is *not* a nested-class concept. The
  new pair `resolveSimpleNameToClassId` ↔
  `resolveQualifiedNameToClassId` mirrors JLS §6.2's simple-vs-qualified
  name dichotomy and the surrounding Kotlin vocabulary (`FqName`,
  `KClass.qualifiedName`, `probeFqnSplits`). KDoc on both functions
  refreshed to describe both phases explicitly. A single stray
  reference to the old name lived in `JavaAnnotationOverAst.kt`'s
  `computeClassId` comment (line 60) and was also updated.
  Verification: `:compiler:java-direct:compileKotlin` +
  `compileTestKotlin` exit 0;
  `:compiler:java-direct:test --tests "JavaUsingAst{Phased,Box}TestGenerated"`
  → `BUILD SUCCESSFUL`, **2793 / 2793 green**, zero `FAILED` /
  `FAILURE` lines in `$JD_TMP/jd_test_4.txt`. Net diff: 2 files,
  +12/−7 LoC (KDoc expansion only). No public Java-model interface
  touched; pure rename, no behavior change.

- **2026-05-26 (same-day later #2)** — Collapsed the duplicate
  `inheritedMemberResolver` reference held by both
  `CompilationUnitContext` and `JavaScopeForContext` (Option A of the
  prior analysis). A code-review question asked whether the two
  fields could be collapsed and, if so, which holder should own the
  reference. Investigation showed they are by-construction the *same
  instance* — built once in `JavaResolutionContext.create` and handed
  to both holders — with no code path that lets them diverge
  (`CompilationUnitContext` is immutable; `JavaScopeForContext.with*`
  always pass it through unchanged). The resolver is genuinely
  *per-compilation-unit, scope-invariant* (its inputs are
  `packageFqName` / `classFinder` / `sameFileTopLevelClassProvider`,
  none of which depend on the scope frame), so Option A (own it on
  `CompilationUnitContext`) is the conceptually clean home —
  matching the class-level KDoc on `CompilationUnitContext`. Refactor:
  removed `inheritedMemberResolver` from the `JavaScopeForContext`
  constructor signature; added it as a parameter to
  `findClassInCurrentScope(name, inheritedMemberResolver)` (the sole
  reader, step 3 of the five-step lookup); rewrote the three `with*`
  copy calls to drop the now-removed argument; rewrote the one
  external caller `JavaResolutionContext.findClassInCurrentScope` to
  pass `unitContext.inheritedMemberResolver`; dropped the resolver
  argument from the `JavaScopeForContext(...)` call in
  `JavaResolutionContext.create`. Verification:
  `:compiler:java-direct:compileKotlin` + `compileTestKotlin` exit 0;
  `:compiler:java-direct:test --tests "JavaUsingAst{Phased,Box}TestGenerated"`
  → `BUILD SUCCESSFUL`, **2793 / 2793 green**, zero `FAILED` /
  `FAILURE` lines in `$JD_TMP/jd_test_3.txt`. Net diff: 2 files,
  +7/−7 LoC. No public Java-model interface touched; no behavior
  change. Contrast with the May-26 `containingClass` collapse (Option
  B-shape, owning on the scope): `containingClass` is genuinely a
  scope-frame anchor (changes per `withContainingClass`), so its
  natural home was `JavaScopeForContext`; `inheritedMemberResolver`
  is scope-invariant, so its natural home is `CompilationUnitContext`.

- **2026-05-26 (same-day later #1)** — Dropped the dead
  `extraAnnotations` parameter from `createJavaType` and its three
  private helpers (`tryCreateArrayOrVarargFromTypeNode`,
  `createWildcardType`, `createClassifierOrPrimitive`) in
  `JavaTypeOverAst.kt`. A code-review follow-up asked: *"it seems
  `extraAnnotations` is never passed to `createJavaType`, right?"*
  Audit of every call site confirmed: yes — all 8 external + internal
  recursive `createJavaType(…)` calls were passing the default
  `emptyList()` for that argument since Iteration 22 split the
  original `@NotNull`-on-method-MODIFIER_LIST role off into the
  separate `memberAnnotations` parameter. The only real producer of
  TYPE-position annotations — the type-argument sibling-`ANNOTATION`
  harvest for `List<@NotNull Integer>` introduced in Iteration 19 —
  computes its `typeNodeAnnotations` *locally* inside
  `createClassifierOrPrimitive` and feeds it straight into
  `JavaClassifierTypeOverAst(…, typeNodeAnnotations, …)`, never
  round-tripping the value through `createJavaType`. The
  outermost-array-dim `if (i == dims - 1) extraAnnotations else emptyList()`
  branch in `tryCreateArrayOrVarargFromTypeNode`'s repeat-loop was
  also dead for the same reason — the loop is now
  `JavaArrayTypeOverAst(typeNode, tree, resolutionContext, result)`
  with both annotation parameters defaulted to empty. Refactor: dropped
  the parameter from the five function signatures and the seven
  forwarding call sites; preserved the
  `extraAnnotations: Collection<JavaAnnotation> = emptyList()`
  *constructor parameter* on the `JavaTypeOverAst` subclasses
  (`JavaClassifierTypeOverAst`, `JavaPrimitiveTypeOverAst`,
  `JavaArrayTypeOverAst`, `JavaWildcardTypeOverAst`) and on
  `JavaTypeOverAst` itself — it is still consumed by the local
  `typeNodeAnnotations` path in `createClassifierOrPrimitive`'s
  JAVA_CODE_REFERENCE branch (the one place where the field is read
  back via `typePositionAnnotations`). Verification:
  `:compiler:java-direct:compileKotlin` + `compileTestKotlin` exit 0;
  `:compiler:java-direct:test --tests "JavaUsingAst{Phased,Box}TestGenerated"`
  → `BUILD SUCCESSFUL`, **2793 / 2793 green**, zero `FAILED` /
  `FAILURE` lines in `$JD_TMP/jd_test_2.txt`. Net diff: 1 file,
  +15/−21 LoC. No public Java-model interface touched; no behavior
  change.

- **2026-05-26** — Collapsed redundant `containingClass` field that was
  duplicated between `JavaResolutionContext` and `JavaScopeForContext`.
  A code-review question asked whether the three `outerClass` /
  `containingClass` fields carried on `JavaClassOverAst`,
  `JavaResolutionContext` and `JavaScopeForContext` were the same and
  could be collapsed. Investigation showed that on the resolver side
  the two were kept in lockstep — `JavaResolutionContext` stored its
  own `containingClass` next to `scopeResolver.containingClass`, and
  the only mutator (`withContainingClass`) updated both copies with
  the same reference; the constructor calls in `withTypeParameters` /
  `withInheritedTypeParameters` simply forwarded the value through, and
  the factory (`create`) never set it. The model-side
  `JavaClassOverAst.outerClass` is a different concept entirely — it
  implements the public `JavaClass.outerClass` contract consumed by
  FIR, and the equality with `JavaResolutionContext.containingClass`
  is a *construction invariant* (`findInnerClassImpl` always builds
  the inner with `outerClass = this` and
  `resolutionContext.withContainingClass(this)`), not a definitional
  identity — so it stays as the canonical source of truth on the
  model side and is untouched by this iteration (Non-Negotiable Rule
  §7). Refactor: dropped `private val containingClass: JavaClass? = null`
  constructor parameter / field on `JavaResolutionContext`; promoted
  `JavaScopeForContext.containingClass` from `private val` to plain
  `val` (visibility still capped by the `internal` class) so it is
  readable from `JavaResolutionContext`; rewrote the three readers
  (`getAggregatedInheritedInnerClasses` outer-chain walk at line 78,
  `resolveInheritedInnerClassToClassId` pass-through at line 623,
  `getContainingClassIds` at line 667) to consume
  `scopeResolver.containingClass`; dropped the now-redundant
  `containingClass = …` argument from the three internal
  `JavaResolutionContext(...)` constructor calls in
  `withTypeParameters` / `withInheritedTypeParameters` /
  `withContainingClass`. Verification:
  `:compiler:java-direct:compileKotlin` + `compileTestKotlin` exit 0;
  `:compiler:java-direct:test --tests "JavaUsingAst{Phased,Box}TestGenerated"`
  → `BUILD SUCCESSFUL`, **2793 / 2793 green**, zero `FAILED` /
  `FAILURE` lines in `$JD_TMP/jd_test.txt`. Net diff `git diff --stat`:
  2 files, 4 insertions, 8 deletions. No public Java-model interface
  touched; no behavior change.

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

## Stage 2 §6.2 — Narrow `JavaSymbolProvider` to Java source classes — 2026-05-28

### Overview

Second sub-step of Stage 2 (= Phase 2 of
[`implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md)),
following §6.1 (indirect call-site audit, landed same day). Goal:
narrow `JavaSymbolProvider` to *Java source classes only*, so binary
Java lookups can move into `JvmClassFileBasedSymbolProvider` in the
upcoming §6.3.

The shape — exactly as prescribed by
[`DIRECT_INJECTION_STAGE_1_2026_05_20.md`](implDocs/DIRECT_INJECTION_STAGE_1_2026_05_20.md)
§6.2 — is three new source-only probes plumbed through three layers:

1. **`JavaClassFinder` interface** (`core/compiler.common.jvm/.../load/java/JavaClassFinder.kt`)
   gets `isInSourceIndex(classId)`, `hasPackageInSources(fqName)`,
   and `sourceClassNamesInPackage(packageFqName)`. All three have
   **safe defaults** that coincide with the existing finder methods
   for single-side finders (PSI, reflect, javac, plain binary). So no
   existing impl needs to change — narrowing is a no-op on the
   non-java-direct path.
2. **`CombinedJavaClassFinder`** overrides all three to delegate to
   `sourceFinder` only (`JavaClassFinderOverAstImpl`).
3. **`FirJavaFacade`** surfaces the three probes so
   `JavaSymbolProvider` doesn't reach into the finder directly.

`JavaSymbolProvider` then:

- swaps its class-id gate from `javaFacade.hasTopLevelClassOf(classId)`
  (source∪binary) to `javaFacade.isInSourceIndex(classId)`;
- routes `hasPackage(fqName)` to `javaFacade.hasPackageInSources(fqName)`;
- routes `symbolNamesProvider.getTopLevelClassifierNamesInPackage`
  to `javaFacade.sourceClassNamesInPackage(packageFqName)`.

The source∪binary names *union* still works end-to-end: it is
reconstituted at the composite-symbol-names-provider layer where
`JavaSymbolProvider.symbolNamesProvider` (now source-only) is composed
with `JvmClassFileBasedSymbolProvider.knownTopLevelClassesInPackage`
(binary). The composite logic does not change.

### Initial misstep and revert (key learning)

The first cut of this iteration also extended the §6.1 helper
`FirSession.getJavaClassLikeSymbolByClassId(classId)` with a binary
composite-walk fallback — the intent being to recover binary-Java
visibility for the three §6.1 callers post-§6.2 narrowing. That walk
turned out to be wrong in two ways:

1. **Cross-session leakage.** Walking the composite tree reaches into
   dependency-session deserializers. In `testVarargClassParameterOnJavaClass`
   (`MODULE: lib` defines Java `class O`; `MODULE: main(lib)` defines
   Kotlin `class O`), the walk picked up `lib`'s binary `O` from
   main's composite — and `FirJvmConflictsChecker` reported a spurious
   `CLASSIFIER_REDECLARATION`. The original
   `JavaSymbolProvider`+`CombinedJavaClassFinder` did *not* see
   cross-module dependency binaries — its binary half was scoped to
   the source session's own scope.
2. **Local-class crash.** `provider.getClassLikeSymbolByClassId(localClassId)`
   on the deserializer asserts:
   `IllegalArgumentException: Local <local>/C should never be used to
   find its corresponding classifier`. The composite walk had no
   local-class filter, so any local Java declaration ran into this
   (visible in `testLocalEntities`,
   `testJavaAnnotationOnSecondaryConstructorOfLocalClass`,
   `testComplexAnnotations`, `testLocalClass`,
   `testLocalClassesAndAnonymousObjects`,
   `testLocalClassApproximationAfter`, `testLocalVsStatic`,
   `testJavaFieldAndKotlinPropertyReferenceFromInner` ×2,
   `testJavaProtectedFieldAndKotlinInvisiblePropertyReference`,
   `testJavaAnnotationOnSecondaryConstructorOfLocalClass`,
   `testKt52146_samWithSelfTypeAndStarProjection`,
   `testDeclaredMembers`, `testMethodsPriority`,
   `testNonSuperCallConstructorJavaSamePackage`,
   `testNonSuperCallConstructorJavaDifferentPackage` — 16 in total).

Combined, the fallback regressed 16/2701 java-direct tests in two
distinct clusters. Reverted; helper stays a thin
`javaSymbolProvider?.getClassLikeSymbolByClassId(...)` delegate.

Each of the three §6.1 callers is OK with source-only behavior:

- **`FirDirectJavaActualDeclarationExtractor`** already filters
  `FirDeclarationOrigin.Java.Source` — binary Java classes are not
  valid actualization candidates anyway.
- **Lombok `AbstractBuilderGenerator`** discovers Lombok-annotated
  *source* classes — Lombok generation doesn't apply to pre-compiled
  Java.
- **`FirJvmConflictsChecker`** historically reported both Kotlin vs
  Java-source AND Kotlin vs Java-binary redeclarations, but the
  java-direct test suite has no fixture for the binary-redeclaration
  case. §6.3 will restore that coverage via a *targeted* deserializer
  lookup (not a naive composite walk) that respects local-class
  filtering and scope-of-session.

### Changes

- `core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/JavaClassFinder.kt`:
  three new interface methods with safe defaults.
- `compiler/java-direct/src/.../CombinedJavaClassFinder.kt`:
  three overrides delegating to `sourceFinder`.
- `compiler/fir/fir-jvm/src/.../FirJavaFacade.kt`: three facade-surface
  methods (`isInSourceIndex` / `hasPackageInSources` /
  `sourceClassNamesInPackage`) over the finder probes.
- `compiler/fir/fir-jvm/src/.../JavaSymbolProvider.kt`: gate switch
  (`hasTopLevelClassOf` → `isInSourceIndex`); `hasPackage` →
  `hasPackageInSources`; `getTopLevelClassifierNamesInPackage` →
  `sourceClassNamesInPackage`; helper KDoc rewritten to document
  source-only behavior + the §6.3 follow-up.

### Test Results

- `:compiler:java-direct:test --tests "JavaUsingAst{Phased,Box}TestGenerated"
  --rerun` → **2701/2701 green**, 0 failures, 0 errors.
- `:compiler:fir:analysis-tests:test --tests
  "PhasedJvmDiagnosticLightTreeTestGenerated.*" --rerun` →
  **10787/10787 green**, 0 failures, 0 errors. Confirms the §6.2
  narrowing is a no-op for single-side finders (PSI's
  `JavaClassFinderImpl` etc.) — the safe defaults on the new
  `JavaClassFinder` methods preserve existing behavior.

### Files Modified

| File | Change |
|------|--------|
| `core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/JavaClassFinder.kt` | +30 LoC — `isInSourceIndex` / `hasPackageInSources` / `sourceClassNamesInPackage` interface methods, all with safe defaults. |
| `compiler/java-direct/src/.../CombinedJavaClassFinder.kt` | +14 LoC — three overrides delegating to `sourceFinder`. |
| `compiler/fir/fir-jvm/src/.../FirJavaFacade.kt` | +19 LoC — three facade-surface methods over the new probes. |
| `compiler/fir/fir-jvm/src/.../JavaSymbolProvider.kt` | +9 / −4 LoC — gate switch + helper KDoc rewrite. |

### Key Learnings

- **Don't naive-walk the composite for binary-Java fallback.** The
  composite symbol provider crosses module/session boundaries via
  dependency providers, surfacing binaries the original `JavaSymbolProvider`
  was *not* exposing. It also has no protection against local class ids,
  which the deserializer rejects with
  `IllegalArgumentException: Local <local>/X should never be used to
  find its corresponding classifier`. §6.3 must consult the
  *current-session* `JvmClassFileBasedSymbolProvider` directly, with
  a `classId.isLocal` filter at the top.
- **Safe defaults on the new `JavaClassFinder` methods are the right
  shape** for staying non-disruptive to non-java-direct callers: PSI
  / reflect / javac / plain binary finders inherit `isInSourceIndex =
  true` etc., so `JavaSymbolProvider`'s narrowing becomes a no-op for
  them. PSI regression suite (10787/10787) confirms.
- **The §6.1 helper's signature is right**, but its body is staying
  one delegate longer than expected. Binary-Java visibility is
  trivially restorable in §6.3 by adding a small
  `session.jvmClassFileBasedSymbolProvider?.getJavaLibraryClassLikeSymbol(classId)`
  call beside `javaSymbolProvider`, scoped to the current session.

### Follow-ups for the next iteration

- **§6.3 — Move binary lookups into `JvmClassFileBasedSymbolProvider`**:
  inline the helpers currently inside `BinaryJavaClassFinder`
  (`hasBinaryTopLevelClass`, `binaryNamesInPackage`,
  `hasBinaryPackage`, direct `BinaryJavaClass(virtualFile, ..., classContent = bytes)`
  materialization) into the deserializer at
  `JvmClassFileBasedSymbolProvider.kt:72, 139, 171, 180, 212`. At the
  same time extend `FirSession.getJavaClassLikeSymbolByClassId` with a
  scoped, local-class-filtered lookup on the *current-session*
  `JvmClassFileBasedSymbolProvider` for binary `Java.Library` symbols.
- **§6.4** — drop the source-side binary-finder dependency
  (`createJavaDirectSourceJavaFacadeBuilder` simplifies to source-only;
  library session no longer needs a `createJavaFacade` lambda).
- **§6.5** — delete `CombinedJavaClassFinder.kt` and
  `BinaryJavaClassFinder.kt`.

---

## Stage 2 §6.1 — Indirect `javaSymbolProvider` call-site audit — 2026-05-28

### Overview

First sub-step of Stage 2 (= Phase 2 of
[`implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md)).
Three FIR sites today call `session.javaSymbolProvider?.getClassLikeSymbolByClassId(...)` directly
in order to find Java declarations *despite* a Kotlin class sharing the same `ClassId` (the entire
point of the diagnostics involved: JVM redeclaration, Kotlin-to-Java direct actualization, and
Lombok builder discovery). These sites all rely on the fact that today's `JavaSymbolProvider` —
backed by `CombinedJavaClassFinder` — returns *both* source and binary Java symbols.

Once Stage 2 §6.2 narrows `JavaSymbolProvider` to source-only and §6.3 moves binary Java lookups
into `JvmClassFileBasedSymbolProvider`, these three sites must continue to see binary Java
symbols too. The natural-looking re-route `session.symbolProvider.getClassLikeSymbolByClassId(...)
+ origin filter` (as suggested by the design doc, §6.1) does **not** work, because
`FirCompositeSymbolProvider.getClassLikeSymbolByClassId` uses `firstNotNullOfOrNull` — when a
Kotlin class and a Java class share the same `ClassId`, the Kotlin source provider wins and the
Java symbol is hidden. The first attempt of this iteration tried that approach and produced 12
test failures, all on `JavaUsingAst*TestGenerated` redeclaration / actualization fixtures, which
confirmed the trap.

The right shape — and the one this iteration lands — is a small **Java-targeted lookup helper**
that wraps the direct `javaSymbolProvider` call today and, after §6.3, will *also* consult the
deserializer for binary `FirDeclarationOrigin.Java.Library` results. Each of the three call sites
goes through this helper, so the §6.3 follow-up extends the helper in one place rather than
visiting each call site again.

### Changes

- `compiler/fir/fir-jvm/src/.../java/JavaSymbolProvider.kt`:
  - New top-level extension `fun FirSession.getJavaClassLikeSymbolByClassId(classId: ClassId):
    FirRegularClassSymbol?`, alongside the existing `javaSymbolProvider` session-component accessor.
  - KDoc explains why `session.symbolProvider` cannot be used here, and notes the Stage 2 §6.3
    follow-up that will extend the helper to consult the deserializer for binary results.
  - Today's body: `javaSymbolProvider?.getClassLikeSymbolByClassId(classId)` — zero behavioral
    delta vs. the previous direct calls.

- `compiler/fir/checkers/checkers.jvm/src/.../declaration/FirJvmConflictsChecker.kt`:
  - `session.javaSymbolProvider?.getClassLikeSymbolByClassId(declaration.classId)` →
    `context.session.getJavaClassLikeSymbolByClassId(declaration.classId)`. Imports of
    `javaSymbolProvider` removed; `getJavaClassLikeSymbolByClassId` added.
  - Inline comment cites the §6.1 audit and the §6.3 follow-up.

- `compiler/fir/fir2ir/jvm-backend/src/.../FirDirectJavaActualDeclarationExtractor.kt`:
  - Constructor signature: `(JavaSymbolProvider, Fir2IrClassifierStorage)` → `(FirSession,
    Fir2IrClassifierStorage)`. The `javaSymbolProvider != null` gate is **kept** in
    `initializeIfNeeded` — it is the historical "is this a JVM session?" tell. Comment notes
    that the gate survives Stage 2 (§6.2 only narrows what `JavaSymbolProvider` *returns*, not
    whether it's registered).
  - The body of `extract(expectIrClass)` now reads
    `session.getJavaClassLikeSymbolByClassId(expectIrClass.classIdOrFail)?.takeIf { it.origin
    is FirDeclarationOrigin.Java.Source }` — preserves the strict `Java.Source` filter that
    excludes binary Java actualization (binary Java classes are not valid actualization
    candidates).

- `plugins/lombok/lombok.k2/src/.../generators/AbstractBuilderGenerator.kt`:
  - Two call sites (lines 169, 251 — `addBuilderMethods` and `createAndInitializeBuilders`) now
    use `session.getJavaClassLikeSymbolByClassId(...)`. Import of `javaSymbolProvider` removed;
    `getJavaClassLikeSymbolByClassId` added.

### Test Results

- `:compiler:java-direct:test --tests "JavaUsingAst{Phased,Box}TestGenerated"` (with
  `--rerun`) — **2701/2701 green** (0 failures, 0 errors). Aggregated from
  `compiler/java-direct/build/test-results/test/TEST-*JavaUsingAst*.xml`.
- `:kotlin-lombok-compiler-plugin:test --rerun` — 105 tests, 2 failures
  (`testNoArgsConstructorStatic`, `testAllArgsConstructorStatic`). **Baseline failures, not
  introduced by this iteration.** Confirmed by `git stash`-ing the four touched source files,
  re-running the two tests via `--tests` selectors, and observing the same 2/2 failure shape.
  The failing fixtures exercise `@*ArgsConstructor(staticName = ...)`, handled by
  `AbstractConstructorGeneratorPart.kt` — a Lombok class **not touched** by §6.1. Both failures
  produce `INVISIBLE_REFERENCE: Cannot access 'constructor(...)': it is private in
  'ConstructorExample'`, i.e. the static factory generator for `@*ArgsConstructor(staticName)`
  isn't emitting the static helper on the green tree (unrelated `AbstractConstructorGeneratorPart`
  bug pre-existing on `rr/ic/direct-java`).
- PSI regression suite (`PhasedJvmDiagnosticLightTreeTestGenerated.*` etc.) — **not re-run**
  for this audit step. Rationale: the change to `JavaSymbolProvider.kt` is purely *additive*
  (a new extension function); the changes in the three call sites inline the exact same lookup
  chain the previous code performed (`session.javaSymbolProvider?.getClassLikeSymbolByClassId(...)`).
  The PSI regression suite will be required after §6.2 / §6.3 land (those iterations touch shared
  semantics on `JavaSymbolProvider` and `JvmClassFileBasedSymbolProvider`).

### Files Modified

| File | Change |
|------|--------|
| `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaSymbolProvider.kt` | New `FirSession.getJavaClassLikeSymbolByClassId` extension (+22 LoC including KDoc). |
| `compiler/fir/checkers/checkers.jvm/src/org/jetbrains/kotlin/fir/analysis/jvm/checkers/declaration/FirJvmConflictsChecker.kt` | Route through helper; +5 / −5 LoC net incl. comment. |
| `compiler/fir/fir2ir/jvm-backend/src/org/jetbrains/kotlin/fir/backend/jvm/FirDirectJavaActualDeclarationExtractor.kt` | Constructor takes `FirSession` instead of `JavaSymbolProvider`; `extract` uses helper + `Java.Source` filter; `javaSymbolProvider` JVM-session gate kept in `initializeIfNeeded`. |
| `plugins/lombok/lombok.k2/src/org/jetbrains/kotlin/lombok/k2/generators/AbstractBuilderGenerator.kt` | Two `session.javaSymbolProvider?.getClassLikeSymbolByClassId` call sites routed through helper. |

### Key Learnings

- **Design-doc pitfall.** [`DIRECT_INJECTION_STAGE_1_2026_05_20.md`](implDocs/DIRECT_INJECTION_STAGE_1_2026_05_20.md)
  §6.1 and [`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md)
  §2.4.4 both prescribe the re-route as
  `session.symbolProvider.getClassLikeSymbolByClassId(...)` + `FirDeclarationOrigin.Java` filter.
  **That prescription is wrong** for the redeclaration / actualization case: the composite
  symbol provider's `firstNotNullOfOrNull` strategy hides the Java symbol the moment a Kotlin
  class shares the `ClassId` — which is precisely the case the three sites are about to
  diagnose / actualize. Both documents need a "Update" note pointing at the helper pattern
  used here (filed as follow-up below).
- **The `javaSymbolProvider != null` JVM-session gate is not §6.2 collateral.** §6.2 only
  narrows `JavaSymbolProvider.getClassLikeSymbolByClassId` to source-only; the *registration* of
  the provider on a JVM session remains. So all "is this a JVM session?" gates that read
  `session.javaSymbolProvider != null` (currently the only one is in
  `FirDirectJavaActualDeclarationExtractor.initializeIfNeeded`) survive without changes.
- **Baseline Lombok failure on `rr/ic/direct-java`.** Two pre-existing
  `FirLightTreeBlackBoxCodegenTestForLombokGenerated.test*ConstructorStatic` failures
  (`INVISIBLE_REFERENCE: Cannot access 'constructor(...)': it is private in
  'ConstructorExample'`). Unrelated to §6.1; flagged here so future iterations don't waste
  cycles re-bisecting. Most likely a `DeclarationWithValueAnnStatusTransformer` /
  `AbstractConstructorGeneratorPart` issue around the green-tree path's emission of the
  `staticName` factory.

### Follow-ups for the next iteration

- **§6.2 — `JavaSymbolProvider` source-only narrowing.** Per the design doc:
  `getClassLikeSymbolByClassId` gated on `javaFacade.isInSourceIndex(classId)`,
  `hasPackage` → `hasPackageInSources`, and `symbolNamesProvider.getTopLevelClassifierNamesInPackage`
  source-only. Union with binary names moves into the composite-symbol-names-provider layer.
- **§6.3 — Move binary lookups into `JvmClassFileBasedSymbolProvider`** and at the same time
  extend `FirSession.getJavaClassLikeSymbolByClassId` to also consult the deserializer for
  binary `Java.Library` symbols.
- **Doc refresh.** Update
  [`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`](implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md)
  §2.4.4 table and
  [`DIRECT_INJECTION_STAGE_1_2026_05_20.md`](implDocs/DIRECT_INJECTION_STAGE_1_2026_05_20.md)
  §6.1 to point at the helper pattern (the original `session.symbolProvider` + origin-filter
  prescription is wrong, see Key Learnings).

---

## Option C — JLS 6.4.1 / 7.5 static-vs-type import split (4-bucket model, 7-step dispatcher) — 2026-05-26 (same-day later #5)

### Overview

The previous code-review iteration (same-day #4) fixed the JLS rank
1–5 ordering inside `resolveSimpleNameToClassIdImpl` but left the
data-model conflation between static and non-static imports
untouched: both kinds were dumped into the same
`Map<String, FqName> simpleImports` and `List<FqName> starImports`,
indistinguishable downstream. A reviewer flagged
`simpleImports.putIfAbsent(simpleName, FqName(fqName))` in
`JavaImportResolver.kt` for exactly that reason.

The earlier analysis enumerated three options:

- **A** — Separate buckets, dispatcher unchanged.
- **B** — Separate buckets + lazy `tryResolve` probe to filter
  method/field static imports off the classifier path.
- **C** — Full JLS 6.4.1 7-rank dispatcher.

This iteration implements Option C. The dispatcher now honours the
strict JLS shadowing order including rank 7 (static-import-on-demand
strictly lower than rank 6 type-import-on-demand), and `JavaImports`
exposes the four buckets to every downstream consumer.

### Investigation summary

JLS divergence between static and non-static imports, restricted to
the classifier (type-name) resolution path:

| Form | JLS clause | What it imports | Rank (JLS 6.4.1) |
|------|-----------|-----------------|------------------|
| `import a.b.C;` | 7.5.1 | A type | 4 |
| `import static a.b.C.X;` | 7.5.3 | A type, method, or field | 4 |
| `import a.b.*;` | 7.5.2 | All types in package `a.b` | 6 |
| `import static a.b.C.*;` | 7.5.4 | All static members of `a.b.C` | **7** |

Key consequence: a static-import-on-demand is *strictly lower* than
a type-import-on-demand. Pre-Option-C, both were merged into a
single `starImports` list and probed via `ClassId(starPackage,
name)`, which (a) used the wrong `ClassId` shape for nested types
reached through `import static X.*;` (the right shape is
`ClassId(X.packageFqName, X.relativeClassName.child(name))`) and
(b) gave both bucket kinds equal rank.

Three concrete bugs the old conflation caused (all latent — the
2793-test suite did not exercise them heavily, since most fixtures
avoid `import static`):

1. **`getSimpleImport` in
   `JavaEnumValueAnnotationArgumentOverAst.staticImportResolution`.**
   The method *intended* to ask "did we `import static Outer.X;`?"
   so a bare `X` enum reference could be re-bound to `Outer.X`. But
   `getSimpleImport` conflated both buckets, so a *non-static*
   single-type import (`import a.b.X;`) silently masqueraded as
   the static binding, and the synthesised `(className, memberName)`
   pair was the wrong shape.
2. **`resolveFromStarImports` `ClassId` shape.** For
   `import static a.b.C.*;`, the entry was `a.b.C` (a class), but
   the probe `ClassId("a.b.C", name)` treated it as a package.
   This worked by accident for some FIR-symbol-provider fallback
   paths but is structurally wrong.
3. **Rank-6 vs rank-7 ordering.** A file with `import a.*;
   import static b.C.*;` where both expose a type `X` should
   resolve to `a.X` per JLS, but the merged step returned
   whichever iterated first.

### Changes

- `JavaImportResolver.kt`:
  - New `JavaImports` holder class with four buckets:
    `simpleTypeImports`, `staticSingleImports`, `typeStarImports`,
    `staticStarImports`. Each bucket's JLS clause and shadowing
    rank is documented in the holder's KDoc. `JavaImports.EMPTY`
    constant for default-argument call sites.
  - `extractImports` signature changed from `Pair<Map, List>` to
    `JavaImports`. Per-shape extractors (`extractNormalImports`,
    `extractStaticImports`, `extractErrorElementImports`,
    `extractFragmentedImports`) updated to route into the correct
    buckets — `extractStaticImports` is the only one that touches
    the `static*` buckets; error-recovered and fragmented imports
    are always typed because the recovery paths don't preserve a
    `static` keyword distinction.
  - `JavaImports.getSingleImport(simpleName)` — unified
    type-first-then-static lookup for model-side consumers that
    don't need to distinguish the two.
- `CompilationUnitContext.kt`:
  - `simpleImports`/`starImports` replaced by a single
    `imports: JavaImports` field; KDoc updated.
- `JavaResolutionContext.kt`:
  - Dispatcher `resolveSimpleNameToClassIdImpl` extended to 7
    steps (see Overview / Investigation table). KDoc rewritten.
  - `resolveFromExplicitImport` now reads
    `unitContext.imports.simpleTypeImports`; only the rank-4
    type case (no static).
  - New `resolveFromStaticSingleImport` — rank 4 (after rank-4
    type, ordering is no-op for well-formed Java). Uses
    `resolveAsClassId` so the imported `Outer.X` FqName resolves
    via longest-package-first split (yields
    `ClassId(Outer.packageFqName, Outer.relativeClassName.X)`
    when `X` is a nested type; misses cleanly when `X` is a
    method/field).
  - `resolveFromStarImports` replaced by two separate steps:
    - `resolveFromTypeStarImports` (rank 6) — reads
      `typeStarImports`. Primary probe `ClassId(pkg, name)`;
      class-level fallback retained for the Kotlin-permissive
      `import a.D.*;` (`a.D` is a class) case that
      `testImportThriceNestedClass` and
      `testNestedAndTopLevelClassClash` depend on. Ambiguity
      detection across multiple star entries preserved.
    - `resolveFromStaticStarImports` (rank 7) — reads
      `staticStarImports`. Uses `resolveAsClassId` on the outer
      FqName then `outerClassId.createNestedClassId(name)`.
      Same ambiguity detection.
  - Accessors:
    - `getSimpleImport(name)` becomes a thin delegate to
      `JavaImports.getSingleImport` (type-first-then-static).
    - New `getStaticImport(name)` — static-single bucket only.
    - `getImports()` returns `JavaImports` directly (was
      `Pair<Map, List>`).
    - `getFirstStarImportCandidate` reads only `typeStarImports`.
    - `isImportTargetAvailableAsJavaClass` uses the unified
      single-import accessor.
  - `companion object create()` and `extractImports(tree, root)`
    updated for the new signature.
- `compiler/java-direct/src/.../util/JavaSupertypeGraph.kt`:
  - `extractSupertypeRefsFromNode` and `resolveSupertypeReference`
    take `JavaImports` instead of `(simpleImports, starImports)`.
  - `resolveSupertypeReference` rank order: same-file source class,
    then `simpleTypeImports` else `staticSingleImports` via
    `fqNameSplitCandidates`, then `typeStarImports` (same-file
    priority + binary fallback), then `staticStarImports` emitting
    split candidates of `outerFqName.child(name)`.
- `compiler/java-direct/src/.../model/JavaAnnotationOverAst.kt`:
  - `JavaEnumValueAnnotationArgumentOverAst.staticImportResolution`
    now calls `resolutionContext.getStaticImport(text)` — the
    semantically-correct static-only lookup. The other
    `getSimpleImport` call sites in the model (which only need a
    yes/no on "any single-import?") continue to use the unified
    accessor.

### Test Results

- First test run (`$JD_TMP/jd_test.txt`): `2700 tests completed, 2
  failed` — `testImportThriceNestedClass` and
  `testNestedAndTopLevelClassClash`. Both exercise the
  Kotlin-permissive `import a.D.*;` where `a.D` is a class shape
  that the strictly-JLS-only static-on-demand step does not cover.
  Fix: restore the class-level fallback inside
  `resolveFromTypeStarImports` (Kotlin compiler historically
  accepts this and we cannot tell at parse time whether the
  dotted prefix is a package or a class). Static-on-demand step
  still fires only for entries that *did* come from
  `import static`, preserving the JLS rank-6 vs rank-7
  distinction for genuine static stars.
- Final test run (`$JD_TMP/jd_test_2.txt`):
  `:compiler:java-direct:test --tests
  "JavaUsingAst{Phased,Box}TestGenerated"` → `BUILD SUCCESSFUL`,
  zero `FAILED` / `FAILURE` lines. 2700 / 2700 green on this
  worker. (Worker-local suite size of 2700 vs the historical
  2793 reflects session-level test filtering, not a regression
  — the 2700 was also the denominator in the failing earlier
  run.)

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../resolution/JavaImportResolver.kt` | New `JavaImports` holder class; `extractImports` returns `JavaImports`; per-shape extractors route into 4 buckets |
| `compiler/java-direct/src/.../resolution/CompilationUnitContext.kt` | `simpleImports`/`starImports` → `imports: JavaImports` |
| `compiler/java-direct/src/.../resolution/JavaResolutionContext.kt` | 7-step dispatcher; new `resolveFromStaticSingleImport`; `resolveFromStarImports` split into `resolveFromTypeStarImports` + `resolveFromStaticStarImports`; new `getStaticImport`; `getImports()` returns `JavaImports`; `create()` and `extractImports(tree, root)` rewired |
| `compiler/java-direct/src/.../util/JavaSupertypeGraph.kt` | `extractSupertypeRefsFromNode` / `resolveSupertypeReference` take `JavaImports`; new rank ordering with split candidates for static-on-demand |
| `compiler/java-direct/src/.../model/JavaAnnotationOverAst.kt` | `JavaEnumValueAnnotationArgumentOverAst.staticImportResolution` now reads `getStaticImport` (was `getSimpleImport`, conflated buckets) |

### Key Learnings

- The `import a.D.*;` where `a.D` is a class is strictly illegal
  Java (per JLS 7.5.2 type-import-on-demand requires a *package*
  on the left), but the Kotlin compiler's historical behaviour
  treats it as if `static`. Two existing tests pin this
  permissive interpretation, so the class-level fallback inside
  the rank-6 type-star step must be retained — separating
  rank-6 from rank-7 by data-model alone is not enough.
- The data-model split *is* the load-bearing change that
  unblocks downstream correctness even if the dispatcher were
  left at rank-5: `JavaEnumValueAnnotationArgumentOverAst`
  immediately benefits from being able to ask the
  semantically-correct question
  `is there a *static* single-import for this bare name?`. The
  rank-7 step is the gravy; the data-model split is the cake.
- `resolveAsClassId` is the right tool for both the static-single
  step (Step 3b) *and* the class-level fallback inside the
  type-star step: it tries all package/class splits of an FqName,
  letting the FIR symbol provider's `tryResolve` pick the right
  one. The trivial `ClassId.topLevel` split is wrong for nested
  type imports (`import a.b.C.D;`).
- Two of the three additional regression-test ideas from the
  prior analysis (`import static a.b.C.foo;` collision and the
  rank-6-vs-rank-7 ordering test) are now blocked only on
  writing the fixtures — the dispatcher has the right shape
  for them. Worth adding when next touching the `imports/`
  test data directory.

---

## JLS 6.4.1 import-precedence fix in `resolveSimpleNameToClassIdImpl` — 2026-05-26 (same-day later #4)

### Overview

Code-review question on
`JavaResolutionContext.resolveSimpleNameToClassIdImpl`: *"Is it
actually correct that we prefer explicitly imported classes to nested
defined closer to us?"* — illustrated by:

```java
import java.util.List;

public class MyJavaClass {
    public static class List<F> {}
    public static void foobar(List<Object> x) {} // Resolved to MyJavaClass.List
}
```

Answer: no. Per JLS §6.4.1 (Shadowing):

- A *member type* of the enclosing class (own or inherited) **shadows**
  any single-type-import of the same simple name within the class body.
- A top-level type declared in the **same compilation unit** also
  **shadows** the single-type-import.
- Conversely, a single-type-import **shadows** top-level types declared
  in *other* compilation units of the same package.

The old order in `resolveSimpleNameToClassIdImpl` had
`resolveFromExplicitImport` *first* — JLS-incorrect on the first two
points. The example above happened to work in practice only because
`JavaTypeOverAst.computeClassifier` has an AST-side fast path
(`findClassInCurrentScope` consults syntactic scope, not imports) that
short-circuits before the `ClassId` dispatcher is reached. Other paths
(annotation references, qualified-name first segments, classes that
surface only via the symbol provider) hit the broken dispatcher
directly.

### Investigation summary

JLS §6.4.1 distinguishes *three* directions of shadowing in this area,
not two:

| Pair | Who shadows whom |
|------|------------------|
| Member type of enclosing class ↔ single-type-import | Member type wins |
| Same-compilation-unit top-level type ↔ single-type-import | Top-level type wins |
| **Other**-compilation-unit, same-package top-level type ↔ single-type-import | **Import** wins |

The dispatcher previously had no way to distinguish "same compilation
unit" from "same package, other file" because `resolveFromSamePackage`
probes the bare `ClassId(packageFqName, simpleName)`, which matches
*both* — and the dispatcher therefore could not implement the correct
JLS direction. The fix introduces a dedicated
`resolveFromSameCompilationUnit` step that is driven by
`JavaScopeForContext.sameFileTopLevelClassProvider` (the only source
of truth for "is `simpleName` declared as a top-level class in *this*
file?"). `resolveFromSamePackage` is left after the explicit import
to handle cross-file same-package types in the JLS-correct direction.

During development, a first naïve reorder — "just move
`resolveFromLocalScope` before `resolveFromExplicitImport`" without
splitting same-package by file — triggered two regressions that
exactly exercise the cross-file direction:

- `testCurrentPackageAndExplicitImport`: `b/T.java` does
  `import a.Y;` while `b/Y.java` exists as a separate compilation
  unit in package `b`. JLS says the import shadows `b.Y`, so
  `T.getY()` returns `a.Y` (which has `test()`). The naïve reorder
  picked `b.Y` (no `test()`) and broke the call.
- `testJavaSupertypeNameDisambiguation`: `Derived.java` does
  `import diff.Base;` while another file in the same root package
  also declares `Base`. JLS says `Derived extends diff.Base`; the
  naïve reorder picked the root-package `Base`, which has no `f()`.

Both pinned down that the resolver-side must split same-package into
"same file" (shadows import) and "other file" (shadowed by import) —
which is what the final fix does.

### Changes

- `JavaResolutionContext.kt`:
  - `resolveSimpleNameToClassIdImpl` body reordered to JLS 6.4.1
    priority: (1) `resolveFromLocalScope` → (2)
    `resolveFromSameCompilationUnit` → (3) `resolveFromExplicitImport`
    → (4) `resolveFromSamePackage` → (5) `resolveFromJavaLang` → (6)
    `resolveFromStarImports`. KDoc rewritten to enumerate the six
    steps and the JLS clauses each implements.
  - New `resolveFromSameCompilationUnit` helper: gates on
    `scopeResolver.sameFileTopLevelClassProvider(simpleName)` and only
    returns the `ClassId(packageFqName, simpleName)` when the simple
    name is declared as a top-level class in *this* file.
  - `resolveFromExplicitImport` KDoc renumbered to Step 3 and clarified
    re shadowing direction (shadowed by Steps 1–2, shadows Step 4).
  - `resolveFromLocalScope` KDoc renumbered to Step 1, with the JLS
    6.4.1 / 6.5.2 reasoning.
  - `resolveFromSamePackage` KDoc renumbered to Step 4 and restricted
    in its description to "other compilation unit" — Step 2 covers
    same-file.
  - `resolveFromJavaLang` and `resolveFromStarImports` renumbered to
    Step 5 / 6 respectively.
- `JavaScopeForContext.kt`:
  - `sameFileTopLevelClassProvider` promoted from `private val` to
    `val`, so `JavaResolutionContext.resolveFromSameCompilationUnit`
    can read it. KDoc added on the field naming both consumers
    (`findClassInCurrentScope` step 5 + the new dispatcher step).

### Test Results

- `:compiler:java-direct:compileKotlin` + `compileTestKotlin`: exit 0.
- First reorder attempt (`$JD_TMP/jd_test_5.txt`): `BUILD FAILED`, 2
  FAILED — `testCurrentPackageAndExplicitImport`,
  `testJavaSupertypeNameDisambiguation`. Both pinned the missing
  same-file / cross-file split.
- After splitting same-package into Step 2 / Step 4
  (`$JD_TMP/jd_test_6.txt`):
  `:compiler:java-direct:test --tests "JavaUsingAstPhasedTestGenerated" --tests "JavaUsingAstBoxTestGenerated"`
  → `BUILD SUCCESSFUL`, **2793 / 2793 green**, zero `FAILED` /
  `FAILURE` lines.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/resolution/JavaResolutionContext.kt` | Reordered `resolveSimpleNameToClassIdImpl` to JLS 6.4.1 priority; added `resolveFromSameCompilationUnit` step; renumbered + rewrote KDocs across all step helpers. |
| `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/resolution/JavaScopeForContext.kt` | Promoted `sameFileTopLevelClassProvider` from `private val` to `val`; added KDoc naming both consumers. |

`git diff --stat` source-side: 2 files changed, +99 / −12 LoC (mostly
KDoc expansion across the six step helpers + one new helper). No
public Java-model interface touched.

### Key Learnings

- **JLS 6.4.1 has three shadowing directions in the import-vs-package
  area, not two.** "Same package shadows import" is a true statement
  *only* for the same-compilation-unit case; cross-file same-package
  types are shadowed *by* the import. A correct dispatcher must keep
  the two cases on opposite sides of `resolveFromExplicitImport`.
  Test cases like `testCurrentPackageAndExplicitImport` exist
  precisely to pin this direction down.
- **`ClassId(packageFqName, simpleName)` is a lossy probe for JLS
  ordering.** Both same-file and cross-file same-package types share
  this `ClassId`, so the dispatcher cannot order them differently
  without an out-of-band signal. The signal we use is
  `JavaScopeForContext.sameFileTopLevelClassProvider`, which is the
  AST-side oracle for "declared in this file" — the same one
  `findClassInCurrentScope` step 5 already uses for the classifier
  fast path. Promoting it from `private` to ordinary `val` on the
  scope holder is the cleanest plumbing.
- **Existing AST fast path on classifier resolution can mask
  dispatcher bugs.** The motivating example
  (`MyJavaClass` with a nested `List` and `import java.util.List`)
  worked correctly in tests only because
  `JavaTypeOverAst.computeClassifier` consults `findClassInCurrentScope`
  before falling back to the `ClassId` dispatcher. Annotation
  references, qualified-name leftmost segments, and supertypes that
  surface via the symbol provider go straight through the dispatcher
  — and that is where the JLS-wrong order had been silently producing
  wrong answers (and where the fix has measurable effect).

---

## Rename `resolveNestedClassToClassId(FromParts)` → `resolveQualifiedNameToClassId(FromParts)` — 2026-05-26 (same-day later #3)

### Overview

Code-review naming follow-up. A reviewer asked whether
`JavaResolutionContext.resolveNestedClassToClassIdFromParts` is named
correctly — i.e. whether it really concerns itself only with nested
classes. Investigation showed: no, it is the *general dotted-name
resolver*. The body is two phases: (1) JLS-6.5.2 nested-class
priority (try every prefix split `Q.Id` as a nested class when `Q`
is a class in scope), followed by (2) a plain FQN-split fallback via
`probeFqnSplits` for inputs like `java.util.Map`. Both phases live
in the same body and both use `tryResolve`; the FQN fallback is *not*
a nested-class concept. The reviewer's follow-up — "in Kotlin terms,
it's rather `resolveQualifiedNameToClassId`?" — pins down the right
name: "qualified name" is the term the Kotlin/JVM compiler ecosystem
already uses for a dot-separated identifier path (`FqName`,
`KClass.qualifiedName`, `JvmClassName`, JLS §6.2's
simple-vs-qualified dichotomy).

This iteration applies that rename, pure-mechanical.

### Investigation summary

The body of the workhorse has three branches; only two are about
nested classes:

| # | Branch | Concerned with nested classes? |
|---|---|---|
| 1 | Prefix-by-prefix `outerClassId.createNestedClassId(nested)` probing (JLS 6.5.2 priority phase) | Yes |
| 2 | `finder.collectInheritedInnerClasses(outerClassId)[parts[1]]` aggregated-map probe (inherited-nested case) | Yes |
| 3 | `probeFqnSplits(parts, tryResolve)` (longest-package-first FQN-split scan) | **No** — pure `ClassId(packageFqName, relativeClassName)` resolution over every package-vs-class split |

Caller audit (within `JavaResolutionContext.kt` — both functions are
`private`):

- `resolveQualifiedNameToClassIdFromParts` is called recursively from
  itself (line 412) and from `resolveInheritedInnerClassToClassId`'s
  `resolveWithoutInheritance` callback (line 627).
- `resolveQualifiedNameToClassId` is called once from `resolve(name)`
  (line 365), on the `name.contains('.')` branch.

The reviewer's "in Kotlin terms" framing also matches the existing
vocabulary in the same file: the KDoc on `resolve(name)` already
calls dotted inputs "fully qualified name", and the fallback helper
itself is literally named `probeFqnSplits`. Keeping the *Qualified*
qualifier (not `FullyQualified`) is intentional — inputs like
`Map.Entry` after `import java.util.*` are only partially qualified
and still need to flow through `resolveSimpleNameToClassIdImpl` for
the leftmost part.

### Changes

- `JavaResolutionContext.kt`:
  - `resolveNestedClassToClassId` → `resolveQualifiedNameToClassId`
    (private wrapper that pre-splits the input).
  - `resolveNestedClassToClassIdFromParts` →
    `resolveQualifiedNameToClassIdFromParts` (private workhorse).
  - All internal references — the recursive call at line 412, the
    `resolveWithoutInheritance` callback at line 627, the call from
    `resolve(name)` at line 365, and the in-body comment at line 355
    — updated via `rename_element` to the new identifiers.
  - KDoc on the wrapper rewritten to describe the two-phase dispatch
    explicitly and cross-link `probeFqnSplits`.
  - KDoc on the workhorse rewritten to: (a) call it a "qualified-name
    resolution" workhorse, (b) describe both phases (JLS 6.5.2
    nested-priority + `probeFqnSplits` FQN-fallback), and (c) note
    that this is the entry point for *all* dotted Java type names —
    fully qualified ones like `java.util.Map` reach `tryResolve` only
    through the `probeFqnSplits` tail.
- `JavaAnnotationOverAst.kt`:
  - One stray comment reference to the old name inside
    `computeClassId` (line 60) updated to
    `resolveQualifiedNameToClassId`.

### Test Results

- `:compiler:java-direct:compileKotlin` + `compileTestKotlin`: exit 0.
- `:compiler:java-direct:test --tests "JavaUsingAstPhasedTestGenerated" --tests "JavaUsingAstBoxTestGenerated"`
  → `BUILD SUCCESSFUL`, **2793 / 2793 green**, zero `FAILED` /
  `FAILURE` lines in `$JD_TMP/jd_test_4.txt`, per the
  AGENT_INSTRUCTIONS protocol.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/resolution/JavaResolutionContext.kt` | Renamed the two private resolver functions to `resolveQualifiedNameToClassId(FromParts)`; rewrote their KDocs to describe both the JLS 6.5.2 nested-priority phase and the `probeFqnSplits` FQN fallback. |
| `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/model/JavaAnnotationOverAst.kt` | Updated the lone stray comment reference inside `computeClassId` (line 60) from `resolveNestedClassToClassId` to `resolveQualifiedNameToClassId`. |

`git diff --stat` source-side: 2 files changed, +12 / −7 LoC (KDoc
expansion only).

### Key Learnings

- **Name by role, not by JLS section.** The body of the workhorse is
  shaped by JLS 6.5.2's *priority* rule (nested-class first), but the
  function's *role* is broader: it is the dispatcher for dotted Java
  type names. The original name described the priority phase and
  hid the existence of the FQN-split fallback (without which inputs
  like `java.util.Map` would not resolve). Naming after role —
  *resolve a qualified name to a `ClassId`* — keeps the reader
  oriented in the much more common case where they are just trying
  to follow how `java.util.Map` becomes a `ClassId`.
- **"Qualified" beats "Dotted" in Kotlin/JVM code.** The Kotlin
  ecosystem uses *qualified* (`FqName`, `KClass.qualifiedName`,
  `JvmClassName`) and "qualified name" is also the term JLS §6.2
  uses for any dot-separated identifier path. *Dotted* is borrowed
  from Python/PEP terminology and would be the only place in the
  module that doesn't speak the local vocabulary. Renaming was a
  one-call `rename_element` change — but staying with the local
  vocabulary makes the file readable to anyone who already knows the
  rest of the compiler.
- **`rename_element` covers in-body comments too.** Both rename calls
  picked up not only the function definition and Kotlin call sites
  but also the in-body comment at line 355 that says "in
  `resolveNestedClassToClassId` probes the same ClassIds many
  times". The only reference the tool *didn't* touch was the one in
  a different file (`JavaAnnotationOverAst.kt`'s `computeClassId`),
  which was a structured KDoc/comment reference rather than a Kotlin
  identifier usage — that one had to be patched manually via
  `search_replace`.

---

## Collapse duplicate `inheritedMemberResolver` reference: own on `CompilationUnitContext`, thread into `JavaScopeForContext.findClassInCurrentScope` — 2026-05-26 (same-day later #2)

### Overview

Code-review follow-up to the *"Are the two `inheritedMemberResolver`
members on `JavaScopeForContext` and `CompilationUnitContext` the
same, and could they be collapsed?"* investigation. The analysis
showed: yes, they are the *same instance* — both wired up from the
single `JavaInheritedMemberResolver(...)` allocation in
`JavaResolutionContext.create` and never allowed to diverge by any
`with*` factory; and yes, they can be collapsed. The analysis also
offered two options:

- **Option A** — own on `CompilationUnitContext` (conceptually
  cleanest: the resolver is per-unit and scope-invariant, matching
  the unit-context KDoc).
- **Option B** — own on `JavaScopeForContext` (mechanically smallest,
  symmetric to the same-day `containingClass` collapse).

This iteration implements **Option A**. The deciding factor over
Option B is conceptual layering: unlike `containingClass`, which
legitimately belongs on the scope (it changes per
`withContainingClass`), `inheritedMemberResolver` does not depend on
any scope-frame state — its inputs (`packageFqName`, `classFinder`,
`sameFileTopLevelClassProvider`) are all per-file. Promoting the
scope-side copy would have kept duplication-by-instance off the
type, but the *responsibility* still belongs to the unit context.

### Investigation summary

The two holders' field values are kept in lockstep by construction:

```kotlin
// JavaResolutionContext.kt — `create` factory (pre-refactor)
val inheritedMemberResolver = JavaInheritedMemberResolver(
    packageFqName, classFinder, sameFileTopLevelClassProvider,
)
val scopeResolver = JavaScopeForContext(
    sameFileTopLevelClassProvider, containingClass = null,
    inheritedMemberResolver,            // → JavaScopeForContext.inheritedMemberResolver
)
val unitContext = CompilationUnitContext(
    packageFqName, simpleImports, starImports,
    inheritedMemberResolver, classFinder, // → CompilationUnitContext.inheritedMemberResolver
    session = session,
)
```

- `CompilationUnitContext` is immutable (plain `class` with `val`s) and
  is built once per file.
- `JavaScopeForContext.with*` (`withTypeParameters`,
  `withInheritedTypeParameters`, `withContainingClass`) carry the
  resolver through unchanged.
- `JavaResolutionContext.with*` reuse the same `unitContext`, so the
  unit-side reference is stable for the life of a file.

Reader audit:

| Holder | Read site | Method called | Returns |
|---|---|---|---|
| `JavaScopeForContext.inheritedMemberResolver` | step 3 of `findClassInCurrentScope` | `findInnerClassFromSupertypes(name, cls, mutableSetOf())` | `JavaClass?` |
| `CompilationUnitContext.inheritedMemberResolver` | `JavaResolutionContext.resolveInheritedInnerClassToClassId` | `resolveInheritedInnerClassToClassId(...)` | `ClassId?` |

Different methods on the same instance, for different downstream
consumers (structural model class vs FIR-side `ClassId`).

`JavaScopeForContext.findClassInCurrentScope` has only one external
caller: `JavaResolutionContext.findClassInCurrentScope`, which
already owns the unit context — so threading the resolver in as a
method parameter is a single-site change that does not require
adding a `unitContext` reference to the scope.

### Changes

- `JavaScopeForContext.kt`:
  - Removed `private val inheritedMemberResolver: JavaInheritedMemberResolver`
    from the constructor signature.
  - `findClassInCurrentScope` gained an `inheritedMemberResolver: JavaInheritedMemberResolver`
    parameter (consumed only at step 3 — the
    `findInnerClassFromSupertypes` BFS into the containing class's
    supertype hierarchy); refreshed the in-body comment to explain
    why the resolver is now caller-supplied (lives on
    `CompilationUnitContext`).
  - Dropped the `inheritedMemberResolver` argument from the three
    `with*` factory copies (`withTypeParameters`,
    `withInheritedTypeParameters`, `withContainingClass`).
- `JavaResolutionContext.kt`:
  - `findClassInCurrentScope(name)` delegate now reads
    `unitContext.inheritedMemberResolver` and passes it to
    `scopeResolver.findClassInCurrentScope(name, …)`.
  - In the `create` factory, the `JavaScopeForContext(...)`
    construction no longer receives `inheritedMemberResolver` as an
    argument; the resolver still lives on `CompilationUnitContext` as
    before.

### Test Results

- `:compiler:java-direct:compileKotlin` + `compileTestKotlin`: exit 0.
- `:compiler:java-direct:test --tests "JavaUsingAstPhasedTestGenerated" --tests "JavaUsingAstBoxTestGenerated"`
  → `BUILD SUCCESSFUL`, **2793 / 2793 green**, zero `FAILED` /
  `FAILURE` lines in `$JD_TMP/jd_test_3.txt`, per the
  AGENT_INSTRUCTIONS protocol.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/resolution/JavaScopeForContext.kt` | Dropped `inheritedMemberResolver` constructor parameter and field; added it as a parameter to `findClassInCurrentScope`; removed the argument from the three `with*` factory copies. |
| `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/resolution/JavaResolutionContext.kt` | `findClassInCurrentScope(name)` delegate now passes `unitContext.inheritedMemberResolver`; `create` factory no longer passes the resolver to `JavaScopeForContext(...)`. |

`git diff --stat`: 2 files changed, 7 insertions(+), 7 deletions(-).

### Key Learnings

- **Choose the holder by conceptual layer, not by mechanical
  proximity.** The previous same-day `containingClass` collapse
  (Option B-shape) routed the unified reference *up* into
  `JavaScopeForContext` because the data is a scope-frame anchor
  (changes per `withContainingClass`). This iteration does the
  opposite for `inheritedMemberResolver`: it routes the unified
  reference *down* to `CompilationUnitContext` because the data is
  per-unit and scope-invariant (its inputs never change between
  `with*` calls). Both collapses are correct; the difference is the
  axis on which the duplicated value lived.

- **A single-site read keeps the parameter-threading option open.**
  `findClassInCurrentScope` is the only `JavaScopeForContext` method
  that needs the resolver, and it has exactly one external caller
  (`JavaResolutionContext.findClassInCurrentScope`). That made
  Option A cheaper than expected — no need to inject
  `unitContext: CompilationUnitContext` into the scope itself; the
  resolver flows in as a method parameter exactly when needed. If
  more scope-side readers had existed, the trade-off would have
  shifted toward giving the scope a `unitContext` field or back to
  Option B.

- **Audit the construction invariant before collapsing.** The
  refactor is safe only because both holders were always handed the
  same `JavaInheritedMemberResolver` instance and no `with*` factory
  let them diverge. The same invariant applies to the previous
  `containingClass` collapse. Documenting *why* the two fields are
  identical-by-construction (and not just by happenstance) is the
  precondition for treating any such duplication as a refactoring
  target rather than as a real semantic distinction.

---

## Drop dead `extraAnnotations` parameter from `createJavaType` and private helpers — 2026-05-26 (same-day later)

### Overview

Code-review follow-up to the *"Is `JavaTypeOverAst.extraAnnotations`
actually used?"* investigation: the analysis showed that the
constructor parameter on the `JavaTypeOverAst` subclasses *is* used
(it carries the type-argument sibling-`ANNOTATION` harvest for
`List<@NotNull Integer>` and was historically attached to the
outermost array dimension), but the user followed up: *"it seems that
it is never passed e.g. to `createJavaType` function, right?"*
Correct. An audit of every `createJavaType(…)` call site — external
(3 ×) and internal recursive (5 ×) — confirmed all eight passed the
default `emptyList()` for `extraAnnotations`. The parameter and its
forwarding through the three private helpers
(`tryCreateArrayOrVarargFromTypeNode`, `createWildcardType`,
`createClassifierOrPrimitive`) was dead code carried across
Iterations 19 → 22 without anyone deleting the entry point. This
iteration removes the parameter from the five function signatures and
the seven forwarding sites; the *constructor parameter* on
`JavaTypeOverAst` subclasses is preserved — it is still consumed by
the local `typeNodeAnnotations` path in
`createClassifierOrPrimitive`'s JAVA_CODE_REFERENCE branch.

### Investigation summary

All `createJavaType(…)` call sites audited:

| # | Caller (file:line) | Passed `extraAnnotations`? |
|---|---|---|
| 1 | `JavaRecordComponentOverAst.kt:27` | no (default) |
| 2 | `JavaMemberOverAst.kt:160` | no (default) |
| 3 | `JavaAnnotationOverAst.kt:310` | no (default) |
| 4 | `JavaTypeOverAst.kt:540` (`createJavaTypeWithAnnotations`) | no (only `memberAnnotations`) |
| 5 | `JavaTypeOverAst.kt:620` (supertype/permits walk) | no (default) |
| 6 | `JavaTypeOverAst.kt:237`, `:247` (`computeTypeArguments`) | no (default) |
| 7 | `JavaTypeOverAst.kt:463` (recursive from `tryCreateArrayOrVararg`) | no (only `memberAnnotations` for vararg component) |
| 8 | `JavaTypeOverAst.kt:489` (recursive from `createWildcardType` for bound) | no (default) |

The only real producer of TYPE-position annotations is the local
`typeNodeAnnotations` computation in `createClassifierOrPrimitive`'s
JAVA_CODE_REFERENCE branch (introduced in Iteration 19 for
`List<@NotNull Integer>`):

```kotlin
val typeNodeAnnotations = tree.getChildrenByType(typeNode, JavaSyntaxElementType.ANNOTATION)
    .map { JavaAnnotationOverAst(it, tree, resolutionContext) }
return JavaClassifierTypeOverAst(referenceNode, tree, resolutionContext,
    extraAnnotations + typeNodeAnnotations, memberAnnotations)
```

Since `extraAnnotations` was empty at every entry point, the sum
degenerates to `typeNodeAnnotations`. The outermost-array-dim sink
(`if (i == dims - 1) extraAnnotations else emptyList()`) was dead for
the same reason.

### Changes

- `JavaTypeOverAst.kt`:
  - `createJavaType`: removed `extraAnnotations: Collection<JavaAnnotation> = emptyList()`
    parameter (5-arity → 4-arity); rewrote the four internal
    calls into the helpers to drop the argument.
  - `tryCreateArrayOrVarargFromTypeNode`: removed `extraAnnotations`
    parameter; the repeat-loop now builds
    `JavaArrayTypeOverAst(typeNode, tree, resolutionContext, result)`
    with both annotation parameters defaulted to empty; refreshed the
    in-body comment to explain why the outer wrapper carries no
    annotations (the outer `TYPE` node never holds TYPE-position
    annotations for arrays in practice; any annotations live inside
    the wrapped `TYPE` child and are picked up by the recursive call).
  - `createWildcardType`: removed `extraAnnotations` parameter; the
    `JavaWildcardTypeOverAst` constructor receives `memberAnnotations`
    by name (the `extraAnnotations` constructor parameter defaults to
    empty).
  - `createClassifierOrPrimitive`: removed `extraAnnotations`
    parameter; the JAVA_CODE_REFERENCE branch now passes
    `typeNodeAnnotations` directly into
    `JavaClassifierTypeOverAst(…, typeNodeAnnotations, memberAnnotations)`;
    the primitive and fallback branches use named
    `memberAnnotations = memberAnnotations` so the
    `extraAnnotations` constructor default kicks in.
  - Preserved the
    `extraAnnotations: Collection<JavaAnnotation> = emptyList()`
    constructor parameter on `JavaTypeOverAst`,
    `JavaClassifierTypeOverAst`, `JavaPrimitiveTypeOverAst`,
    `JavaArrayTypeOverAst`, `JavaWildcardTypeOverAst` — it is still
    read by `JavaTypeOverAst.typePositionAnnotations` and still
    receives the locally computed `typeNodeAnnotations` from
    `createClassifierOrPrimitive`.

### Test Results

- `:compiler:java-direct:compileKotlin` + `compileTestKotlin`:
  exit 0.
- `:compiler:java-direct:test --tests
  "JavaUsingAstPhasedTestGenerated" --tests
  "JavaUsingAstBoxTestGenerated"` → `BUILD SUCCESSFUL`,
  **2793 / 2793 green**; zero `FAILED` / `FAILURE` lines in the saved
  Gradle log (`$JD_TMP/jd_test_2.txt`), per the AGENT_INSTRUCTIONS
  protocol.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/model/JavaTypeOverAst.kt` | Dropped the `extraAnnotations` parameter from `createJavaType`, `tryCreateArrayOrVarargFromTypeNode`, `createWildcardType`, `createClassifierOrPrimitive`; updated the seven forwarding call sites accordingly. Constructor parameter on the type subclasses preserved (still consumed by the local `typeNodeAnnotations` path). |

`git diff --stat`: 1 file changed, 15 insertions(+), 21 deletions(-).

### Key Learnings

- **A parameter being "alive" inside a class doesn't mean its
  function-level entry point is alive.** The constructor parameter
  `JavaTypeOverAst.extraAnnotations` legitimately exists — it is read
  by `typePositionAnnotations` and carries TYPE-position annotations
  for `List<@NotNull Integer>` (Iteration 19). But the *function-level*
  forwarding through `createJavaType(…)` was a separate concern, and
  every caller had been passing the default `emptyList()` since
  Iteration 22 reshuffled responsibilities. The two questions —
  "is the field used?" and "is the function parameter used?" — had
  different answers, even though the parameter and the field share a
  name.

- **Drift between role and shape across iterations.** Iteration 8
  introduced `extraAnnotations` to push `@NotNull` from a member's
  MODIFIER_LIST onto the return type. Iteration 22 split that role
  off into `memberAnnotations` with TYPE_USE pre-filtering. Iteration
  19 then *repurposed* `extraAnnotations` for type-argument
  sibling-`ANNOTATION` annotations, but that fix lives entirely
  inside `createClassifierOrPrimitive` (a sink that *uses* the
  parameter locally and never *sources* it from outside). The
  function signature carried the old role forward without anyone
  noticing that the new role made the function parameter dead.
  Periodic call-site audits catch this kind of drift.

- **Dead outer-dimension annotation channel.** The repeat-loop in
  `tryCreateArrayOrVarargFromTypeNode` had
  `if (i == dims - 1) extraAnnotations else emptyList()` for the
  outermost array dimension. With the parameter gone, both the
  outermost and inner dimensions get empty annotations — which is
  exactly the behavior in production today, because no caller has
  ever supplied a non-empty `extraAnnotations` for an array. The
  array's component type still receives `componentMemberAnnotations`
  (vararg-only) via the recursive `createJavaType` call, matching
  PSI/javac-wrapper's KT-24392 array-head TYPE_USE filter.

---

## Collapse duplicate `containingClass` field between `JavaResolutionContext` and `JavaScopeForContext` — 2026-05-26

### Overview

Code review raised the question: *"Why do we keep the
outerClass/containingClass simultaneously in `JavaClassOverAst`,
`JavaResolutionContext` and `JavaScopeForContext`? Is it the same thing
and if yes, could it be collapsed into one?"* Investigation showed
that on the resolver side the two fields
(`JavaResolutionContext.containingClass` and
`JavaScopeForContext.containingClass`) were kept in lockstep and were
a genuine duplicate; on the model side
(`JavaClassOverAst.outerClass`) the field implements the public
`JavaClass.outerClass` interface contract and is a different concept
that only *happens* to equal the resolver-side value by construction
invariant. The redundant resolver-side copy was removed;
`JavaScopeForContext.containingClass` is the single source of truth
on the resolver side. `JavaClassOverAst.outerClass` was intentionally
left untouched (Non-Negotiable Rule §7 — no changes to the public
Java-model interface surface).

### Investigation summary

| Field | Layer | Conceptual role | Set when… |
|---|---|---|---|
| `JavaClassOverAst.outerClass` | **Model** — implements `JavaClass.outerClass` | Structural parent: drives `fqName`, `isStatic`/`isFinal`/`visibility`, the `outerClass?.isInterface` rules, the same-name positive cache, etc. | Constructor: `outerClass = null` for top-level, `outerClass = this` in `findInnerClassImpl`. |
| `JavaResolutionContext.containingClass` | **Resolution** | "Current resolution frame anchor" — used by `getAggregatedInheritedInnerClasses`'s outer-chain walk, `getContainingClassIds`, and `resolveInheritedInnerClassToClassId`. | `withContainingClass(newContainingClass)`. |
| `JavaScopeForContext.containingClass` | **Scope sub-resolver** | Drives `findClassInCurrentScope` — the five-step lookup (inner / sibling / inherited / outer-chain / top-level same-file). | `JavaResolutionContext.withContainingClass` *always* calls `scopeResolver.withContainingClass(newContainingClass)` with the same argument. |

**Resolver-side fields ≡ duplicate.** `JavaResolutionContext` stored
`containingClass` as its own field, but the only mutator
(`withContainingClass`) updated both copies with the same reference;
`withTypeParameters` / `withInheritedTypeParameters` carried both
through unchanged; the factory `create` never set it (the field
defaulted to `null` on both sides at construction). No code path let
the two diverge.

**Model field vs resolver field — same instance, different role.** In
`findInnerClassImpl`, the inner is built as
`JavaClassOverAst(innerClassNode, tree, contextForInner, outerClass = this)`
with `contextForInner = resolutionContext.withContainingClass(this).…`,
so `outerClass` and `contextForInner.containingClass` are always the
same `JavaClass` reference. However, `outerClass` is part of the
public `JavaClass` interface (read by FIR's `FirJavaFacade`,
`JavaTypeConversion`, etc.) and *must* stay; `containingClass` is
internal to `java-direct`'s resolution machinery. The equality is a
construction invariant, not a definitional identity — collapsing
across the boundary would force `internal` resolver state into a
public model field and violate Rule §7.

### Changes

- `JavaScopeForContext.kt`: dropped `private` on the
  `containingClass` constructor property so it is readable as `val`
  from sibling resolver classes (visibility is still limited by the
  `internal` class).
- `JavaResolutionContext.kt`:
  - removed the
    `private val containingClass: JavaClass? = null`
    constructor parameter / field;
  - rewrote the three readers (the
    `getAggregatedInheritedInnerClasses` outer-chain walk at line 78,
    the `resolveInheritedInnerClassToClassId` pass-through at line
    623, the `getContainingClassIds` walk at line 667) to consume
    `scopeResolver.containingClass`;
  - dropped the now-redundant `containingClass = …` argument from
    the three internal `JavaResolutionContext(...)` constructor calls
    in `withTypeParameters` / `withInheritedTypeParameters` /
    `withContainingClass`. The `create` factory already did not pass
    it, so it was left unchanged.
- `JavaClassOverAst.outerClass`: untouched (public Java-model
  contract; Rule §7).

### Test Results

- `:compiler:java-direct:compileKotlin` + `compileTestKotlin`:
  exit 0.
- `:compiler:java-direct:test --tests
  "JavaUsingAstPhasedTestGenerated" --tests
  "JavaUsingAstBoxTestGenerated"` → `BUILD SUCCESSFUL`,
  **2793 / 2793 green**; zero `FAILED` / `FAILURE` lines in the saved
  Gradle log (`$JD_TMP/jd_test.txt`), per the AGENT_INSTRUCTIONS
  protocol.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/resolution/JavaScopeForContext.kt` | Dropped `private` on `containingClass` constructor property. |
| `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/resolution/JavaResolutionContext.kt` | Removed `containingClass` constructor parameter / field; rewrote three readers to use `scopeResolver.containingClass`; dropped the redundant `containingClass = …` argument from the three internal constructor calls in `withTypeParameters` / `withInheritedTypeParameters` / `withContainingClass`. |

`git diff --stat`: 2 files changed, 4 insertions(+), 8 deletions(-).

### Key Learnings

- **Construction-invariant equality ≠ definitional identity.** Three
  fields can *always* hold the same value in production without being
  the same concept. `JavaClassOverAst.outerClass` and
  `JavaResolutionContext.containingClass` are equal because every
  `JavaClassOverAst` is built by the resolver in lockstep with a
  `withContainingClass(this)` call — but they answer different
  questions ("who lexically declares me?" vs "where is resolution
  currently anchored?"), live on different layers (model vs
  resolution), and serve different consumers (FIR vs java-direct's
  internal scope walks). Collapsing them across the boundary would
  conflate the layers; the equality is documentation, not a
  refactoring opportunity.

- **Lock-stepped sibling fields are the easy collapse case.** When
  two fields are updated *only* together (same argument, same call
  site) and read independently within a single module, one is the
  source of truth and the other is a cached projection that can be
  dropped without behavior change. The KDoc on
  `JavaScopeForContext` already advertised it as the owner of "type
  parameter scoping and current scope class lookup" — the prose
  matched the post-collapse code.

- **Non-Negotiable Rule §7 is a directional constraint.** It forbids
  *adding* members to the public Java-model interfaces. It does not
  forbid removing internal resolver-side copies of values that happen
  to equal a public model field — provided the removal does not push
  internal resolver state into a public field. Here the model side
  (`JavaClassOverAst.outerClass`) stays as the canonical structural
  parent, untouched; only the resolver-internal duplicate was
  removed.

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
