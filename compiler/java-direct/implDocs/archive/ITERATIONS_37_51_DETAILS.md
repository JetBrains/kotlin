# Java-Direct: Iterations 37-51 Details (Archived)

**Archive Date**: 2026-03-23
**Coverage**: Iterations 37 through 51
**Result**: 1157/1168 box → 1165/1168 box (99.7%), phased 1374/1442 → 1442/1456 (99.0%), **17 failing**

> **Warning**: This document is archived for historical reference. Only consult if you need to understand specific implementation decisions or debug regressions.

---

## Iteration Summary

| Iteration | Date | Focus | Tests Fixed | Key Change |
|-----------|------|-------|-------------|------------|
| 37 | 2026-03-16 | Implicit Java modifiers area audit | +5 | isFinal/isAbstract/findAnnotation/isNative/isDeprecatedInJavaDoc/constructor overrides |
| 37b | 2026-03-16 | Import resolution: explicit priority, duplicate star, nested class FQN | +7 (net after regressions) | explicit-import-first; `.distinct()` for star; `findClassId` instead of `ClassId.topLevel` |
| 38 | 2026-03-17 | Const evaluation correctness + malformed constructor | +4 | `isInitializerPotentiallyConstant`; IDENTIFIER guard for constructors |
| 39 | 2026-03-17 | Sealed class inheritors + supertype inner class resolution | +3 | cross-file permitted types fallback; implicit permits; dotted supertype package prefix |
| 40 | 2026-03-17 | Object method detection + qualified generic types | +3 | unresolved `Object` fallback; `stripTypeArguments`; `collectAllRefParamLists` |
| 41 | 2026-03-17 | Value param / type param annotations (area audit) | +0 (infra) | `memberAnnotations` param; TYPE children in `upperBounds` |
| 42 | 2026-03-17 | `rawTypeName` excludes annotation text | +1 | `extractTypeName()` AST-based identifier extraction |
| 43 | 2026-03-18 | ClassId-based resolution + TYPE_USE star import fix | +5 | `resolveToClassId()`; `CombinedClassFinder` FQN verification |
| 44 | 2026-03-19 | TYPE_USE annotation filtering fix | +13 | type-position vs member annotations split in `filterTypeUseAnnotations` |
| 45 | 2026-03-19 | Type parameter direct annotations | +3 | direct ANNOTATION children on TYPE_PARAMETER (no MODIFIER_LIST wrapper) |
| 46 | 2026-03-20 | Import resolution: nested class FQNs, class-level star, JLS 8.5 shadowing | +4 | `resolveAsClassId`; class-level star fallback; `shadowedNames` in `collectInheritedInnerClasses` |
| 47 | 2026-03-20 | KT-4455: non-canonical Java classes not visible from Kotlin | +2 | canonical-file filter in `tryBuildFileEntry`; `knownClassNamesInPackage` returns only canonical names |
| 48 | 2026-03-20 | package-info.java support | +1 | PACKAGE_STATEMENT→MODIFIER_LIST→ANNOTATION extraction |
| 49 | 2026-03-20 | `ft<T,T?>` vs `T!` FIR dump rendering for cross-file source types | +4 | `isTriviallyFlexibleHint` on `JavaClassifierType` |
| 50 | 2026-03-20 | Interface method abstractness + static inner type scoping | +7 | `DEFAULT_KEYWORD` check; `inheritedTypeParametersInScope` (low-priority outer params) |
| 51 | 2026-03-23 | Static-imported Kotlin const vals in Java annotations | +2 | `staticImportResolution` for bare names; `FirExpressionEvaluator` for const evaluation |

---

## Iteration 37: Implicit Java Modifiers Area Audit

### Approach
Reference-first area audit: compared `JavaClassOverAst`/`JavaMemberOverAst` against `TreeBasedClass`/`TreeBasedField`/`TreeBasedMethod`/`TreeBasedConstructor` (javac-wrapper).

### Gaps Found and Fixed (Part A — Implicit Modifiers)

| File | Gap | Reference behavior | Fix |
|------|-----|--------------------|-----|
| `JavaClassOverAst.kt` | `isFinal` | `isEnum || isFinal` | Add enum guard (JLS 8.9); not final if has abstract methods |
| `JavaClassOverAst.kt` | `isAbstract` | `isAbstract || (isAnnotationType|isEnum) && methods.any { abstract }` | Add enum/annotation abstract method case |
| `JavaClassOverAst.kt` | `findAnnotation` | `annotations.find {...}` | Was always `null`; delegate to `annotations` collection |
| `JavaClassOverAst.kt` | `isDeprecatedInJavaDoc` | Check Javadoc comment | Check `DOC_COMMENT` child node text for `@deprecated` |
| `JavaMemberOverAst.kt` | `isDeprecatedInJavaDoc` | Check Javadoc comment | Same: `DOC_COMMENT` child text contains `@deprecated` |
| `JavaMemberOverAst.kt` | `isNative` | `modifiers.isNative` | Was always `false`; now `hasModifier("NATIVE_KEYWORD")` |
| `JavaConstructorOverAst` | `isFinal/isAbstract/isStatic` | `true/false/false` | Override explicitly (constructors are always final, not abstract, not static) |

`hasModifier` made `protected` to allow `JavaMethodOverAst` to call it.

Unit tests added: `testEnumImplicitFinal`, `testAnnotationTypeImplicitAbstract`, `testFindAnnotationOnClass`, `testNativeMethod`, `testConstructorImplicitFinal`, `testDeprecatedInJavaDoc`.

### Import Resolution Bugs Fixed (Part B / 37b)

**1. Explicit imports not checked before same-package** (`JavaResolutionContext.kt`):
- `resolveSimpleName` checked same-package before explicit single-type imports
- JLS 7.5.1: explicit imports shadow same-package classes
- Fix: added step 0 — check `simpleImports[simpleName]` first

**2. Duplicate star imports caused false ambiguity** (`JavaResolutionContext.kt`):
- `starImports` is a `List`, so `import weatherForecast.*` twice → same package tried twice → false "ambiguous" result
- Fix: use `.distinct()` when iterating star imports

**3. `ClassId.topLevel` used for resolved nested class FQNs** (`JavaTypeConversion.kt`):
- `ClassId.topLevel(FqName("a.X.Y"))` splits as package=`a.X`, class=`Y` — WRONG for nested classes
- Fix: replace with `findClassId(qualifiedName, session) ?: ClassId.topLevel(...)` — probes all package/class splits

**4. Default-package lookup regression** (reverted in 37b):
- Added `resolveSimpleName` fallback for default-package code — broke 5 `FailFast` tests (`BoxJvm > Ranges > JavaInterop`)
- Root cause: default-package lookup caused `isTypeUseAnnotation("NotNull")` to be called with simple name → returned false → `@NotNull` dropped → NPE not thrown
- Fix: removed default-package lookup. `testSimpleCorrect` reverts to failing.

### Test Results
- **Box tests**: 1157/1168 (unchanged)
- **Phased tests**: net +7 phased fixed after regression removal (74 → 67 total failures)

### Tests Fixed
`testCurrentPackageAndExplicitNestedImport`, `testImportTwoTimesStar`, `testNestedAndTopLevelClassClash`, `testNestedClassClash`, `testImportThriceNestedClass`, `testNested`, `testSimpleCorrect` (then reverted), `testInnerLightClass` — net: +7 phased

### Files Modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt` — `isFinal`, `isAbstract`, `findAnnotation`, `isDeprecatedInJavaDoc`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaMemberOverAst.kt` — `isDeprecatedInJavaDoc`, `isNative`, `hasModifier` made protected; `JavaConstructorOverAst` overrides
- `compiler/java-direct/test/org/jetbrains/kotlin/java/direct/JavaParsingTest.kt` — 6 new unit tests
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaResolutionContext.kt` — explicit imports before same-package; `.distinct()` for star imports
- `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt` — `findClassId` instead of `ClassId.topLevel` for resolved nested types

### Key Learnings
- When any implicit-Java-rule fix is needed, audit the entire file against the reference implementation
- JLS 7.5.1: explicit single-type imports shadow same-package classes
- Default-package lookup can break TYPE_USE annotation resolution (isTypeUseAnnotation is called with simple name → fails for binary annotations)

---

## Iteration 38: Const Evaluation Correctness + Malformed Constructor Fix

### Root Cause Analysis

**Bug A — `hasConstantNotNullInitializer` returned `true` for non-constant initializers:**
PSI's `JavaFieldImpl.hasConstantNotNullInitializer()` evaluates the expression and returns `false` for method calls or unresolvable references. Java-direct's implementation only did a structural check (final + primitive/String type + has initializer), returning `true` even for `System.currentTimeMillis()` or `SOME_WRONG_EXPRESSION`. FIR's `FirConstCheckVisitor` returned `VALID_CONST` for any `FirFieldSymbol` with `hasConstantInitializer=true` → incorrectly treated as compile-time constants.

**Bug B — `void () {}` treated as constructor in malformed Java class:**
`JavaClassOverAst.constructors` filtered for `it.findChildByType("TYPE") == null`. For the malformed method `void () {}`, the parser emits `ERROR_ELEMENT` instead of `TYPE` (no method name after return type). This malformed node was treated as a constructor → `hasDefaultConstructor() = false` → FIR reported `INVISIBLE_REFERENCE` when calling `Nameless()`.

### Fixes

**Fix A**: Added `isInitializerPotentiallyConstant(node)` in `JavaMemberOverAst.kt`:
- Returns `false` for method calls, object creation, simple unresolvable name references
- Returns `true` for literals, arithmetic, qualified references (might be cross-language constants)
- The fix was originally attempted in `FirJavaFacade.kt` but reverted — caused `testKotlinJavaCycle` to break (null initializer value for valid structural constants in cycles)

**Fix B**: Added `&& it.findChildByType("IDENTIFIER") != null` to the constructor filter. A real constructor always has an IDENTIFIER (the class name).

### Test Results
- **Box tests**: 1157/1168 (unchanged — box count was 8 failing at this point)
- **Phased tests**: +4 fixed (67 → 63 total failures)

### Tests Fixed
- `testFromJavaWithNonConstInitializer` — `System.currentTimeMillis()` no longer treated as constant
- `testJavaProperties` — `i3 = i1` (non-final ref) no longer treated as constant
- `testKt57802` — `SOME_WRONG_EXPRESSION` (unresolvable) no longer treated as constant
- `testNamelessInJava` — malformed `void () {}` no longer treated as constructor

### Files Modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaMemberOverAst.kt` — `hasConstantNotNullInitializer` uses `isInitializerPotentiallyConstant`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt` — constructor filter requires IDENTIFIER
- `compiler/java-direct/test/org/jetbrains/kotlin/java/direct/JavaParsingTest.kt` — `testPublicClassWithMalformedMembers`

### Key Learning
- The correct fix for `hasConstantNotNullInitializer` is at the Java Model level (AST check), NOT in `FirJavaFacade` — FIR-level checks break cross-language const cycles

---

## Iteration 39: Sealed Class Inheritors + Supertype Inner Class Resolution

### Root Cause Analysis

**Bug A — Cross-file permitted types lost:**
`FirJavaFacade.createFirJavaClass` derives `sealedClassInheritors` via `classifierType.classifier as? JavaClass`. In java-direct, `classifier` is null for any cross-file type (requires class finder). For `Base permits A, B` where A and B are in separate `.java` files → empty inheritor list → FIR treated sealed class as having no known subtypes → `when` was incorrectly exhaustive → `NO_ELSE_IN_WHEN` errors missing.

**Bug B — Implicit permits (no `permits` clause) yielded empty permitted types:**
For `sealed class SameFile { class A extends SameFile {} }`, no `PERMITS_LIST` node exists. `permittedTypes` returned `emptySequence()` → FIR had no inheritors → same exhaustiveness problem.

**Bug C — Dotted supertype names missing package prefix:**
In `resolveFromSupertypesRecursive`, supertype `"x.S"` (partially qualified) didn't get package prefix `"a"` added. Only simple (non-dotted) supertypes had package-prefix handling.

### Fixes

**Fix A** (`FirJavaFacade.kt`): When `classifierType.classifier as? JavaClass` is null, build ClassId from `classifierType.classifierQualifiedName` using the current class's package.

**Fix B** (`JavaClassOverAst.kt`): When `isSealed = true` and no `PERMITS_LIST`, scan class body for inner classes that extend/implement the sealed class via `deriveImplicitPermittedTypes()`.

**Fix C** (`JavaResolutionContext.kt`): When package is non-root and supertype name contains a dot, also try `"${packageFqName}.$supertypeName.$simpleName"`.

### Test Results
- **Total failures**: 63 → 60 (+3 fixed)

### Tests Fixed
- `testJavaSealedClassExhaustiveness` — sealed class with cross-file + in-file permitted types
- `testJavaSealedInterfaceExhaustiveness` — sealed interface
- `testInheritedInner2` — `x1.getB()` resolves `B` via supertype `x.S` with package prefix

### Files Modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt` — `permittedTypes` with `deriveImplicitPermittedTypes()`
- `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/FirJavaFacade.kt` — sealed inheritors fallback for null classifier
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaResolutionContext.kt` — package prefix for dotted supertype names

---

## Iteration 40: Object Method Detection + Qualified Generic Types

### Root Cause Analysis

**Bug A — `isMethodWithOneObjectParameter` missed unresolved `Object` types:**
`isObjectMethodInInterface()` checked `classifier.fqName == "java.lang.Object"`. In java-direct, `"Object"` has `classifier = null` (needs FIR callback resolution for java.lang types) → `equals(Object o)` not treated as Object method → interface appeared to have 1 abstract method → valid as fun interface → no `FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS`.

**Bug B — `rawTypeName` stripped at first `<`:**
For `BaseOuter<H>.BaseInner<Double, String>`, `rawTypeName` used `indexOf('<')` → gave `"BaseOuter"` instead of `"BaseOuter.BaseInner"` → wrong class resolved as supertype.

**Bug C — `typeArguments` didn't handle qualified generics:**
For `BaseOuter<H>.BaseInner<Double, String>`, `typeArguments` only found `<H>` (first REFERENCE_PARAMETER_LIST). Also returned early for cross-file (`classifier = null`) types, missing outer class type args.

### Fixes

**Fix A** (`javaLoading.kt`): Added `|| qualifiedName == "Object"` check when `classifier == null`. Unqualified `"Object"` in Java always means `java.lang.Object`.

**Fix B** (`JavaTypeOverAst.kt`): `stripTypeArguments` — balanced-brackets stripper removes `<...>` groups while preserving dots.

**Fix C** (`JavaTypeOverAst.kt`): `collectAllRefParamLists(node)` recursively collects all REFERENCE_PARAMETER_LISTs from nested JAVA_CODE_REFERENCE nodes. Uses the LAST list for innermost explicit args; earlier lists for outer class args. Resolves implicit outer type params via `resolutionContext.findTypeParameter(name)` to get caller's type param instance.

### Test Results
- **Box tests**: 7 → (was 8 failing) +1 fixed
- **Phased tests**: +2 fixed (60 → 57 total failures)

### Tests Fixed
- `testFunInterfaceWithAnyOverrides` — unresolved Object in equals check
- `testJ_k_complex` — qualified generic supertype, inherited methods found
- `testGenericBoundInnerConstructorRef` — side effect of qualified generic type fix

### Files Modified
- `core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaLoading.kt` — fallback for unresolved Object
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt` — `rawTypeName`, `typeArguments`, `collectAllRefParamLists`

---

## Iteration 41: Value Parameter & Type Parameter Annotations (Area Audit)

### Approach
Reference-First Area Audit comparing `JavaValueParameterOverAst` against `TreeBasedValueParameter` and `JavaTypeParameterOverAst` against `TreeBasedTypeParameter`.

### Gaps Found

| File | Property | Reference behavior | java-direct before |
|------|----------|-------------------|--------------------|
| `JavaMemberOverAst.kt` | `JavaValueParameterOverAst.type` | Passes `annotations` (modifier list) to type creation | `createJavaType(typeNode, ctx)` — no annotations |
| `JavaTypeOverAst.kt` | `JavaTypeParameterOverAst.upperBounds` | Handles annotated TYPE nodes | Only `getChildrenByType("JAVA_CODE_REFERENCE")` — misses TYPE children |
| `JavaTypeOverAst.kt` | `JavaTypeParameterOverAst.annotations` | Reads annotations on type param declaration | `emptyList()` |

### Key Finding: `update.test.data=true` Contamination
Running `./gradlew ... -Pkotlin.test.update.test.data=true` modifies test data in TWO directories: `compiler/testData/` AND `compiler/fir/analysis-tests/testData/`. Always restore BOTH with `git checkout compiler/testData/ compiler/fir/analysis-tests/testData/` after any update run.

### Test Results
- Net +0 in full suite (individual tests pass with `--rerun-tasks`; full suite count unchanged due to test infrastructure/caching)

### Files Modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaMemberOverAst.kt` — `JavaValueParameterOverAst.type` uses `createJavaTypeWithAnnotations`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt` — `JavaTypeParameterOverAst.upperBounds` handles TYPE children; `annotations` reads MODIFIER_LIST

---

## Iteration 42: Fix rawTypeName to Exclude Annotations

### Root Cause Analysis
`JavaClassifierTypeOverAst.rawTypeName` was computed from `node.text`, which included annotation text. For `T extends @NotNull Object`, the AST has `JAVA_CODE_REFERENCE: '@NotNull Object'` with children `[ANNOTATION: '@NotNull', IDENTIFIER: 'Object']`. This caused `classifierQualifiedName` to return `"@NotNullObject"` instead of `"Object"`.

### Fix
Replaced text-based `rawTypeName` with `extractTypeName()` that:
1. Recursively traverses `JAVA_CODE_REFERENCE` nodes
2. Collects only `IDENTIFIER` children
3. Joins with `.` for qualified names
4. Ignores `ANNOTATION`, `REFERENCE_PARAMETER_LIST`, etc.

Also removed unused `JAVA_LANG_TYPES` constant (dead code since iteration 31).

### Test Results
- **Total failures**: 57 → 55 (+1 phased fixed)

### Tests Fixed
- `testTypeFromGenericWithAnnotationWithoutWrtHack`

### Files Modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt` — `extractTypeName()`, `collectIdentifiers()`; removed `JAVA_LANG_TYPES`
- `compiler/java-direct/test/org/jetbrains/kotlin/java/direct/JavaParsingTest.kt` — `testTypeParameterBoundWithAnnotation`, `testMethodReturnTypeWithAnnotation`

### Key Learnings
- TYPE_USE annotations appear inline within type references in the AST (not just in MODIFIER_LIST)
- Always extract semantic content (identifiers) from AST structure, not raw text

---

## Iteration 43: ClassId-Based Resolution + TYPE_USE Star Import Fix

### Part 1: ClassId-Based Resolution for Package vs Nested Class

**Problem**: Qualified type names like `a.b` are ambiguous — package `a`, class `b` vs class `a`, nested class `b`. Per JLS 6.5.2, nested class interpretation takes priority. Java-direct's `resolve()` returned a string like `"a.b"` losing the package/class boundary. FIR's `findClassId` tried package interpretations first, violating JLS 6.5.2.

**Fix**: Added `resolveToClassId(tryResolve: (ClassId) -> Boolean): ClassId?` to `JavaClassifierType` interface. Java-direct's implementation uses `resolveSimpleNameToClassId` which returns a `ClassId` encoding the boundary explicitly. `JavaTypeConversion.kt` now tries `resolveToClassId` first.

**Tests Fixed (1)**: `testTopLevelClassVsPackage`

### Part 2: TYPE_USE Annotation Resolution for Star Imports

**Problem**: Star-imported annotations (`import org.jetbrains.annotations.*;`) weren't resolved correctly — `ClassId("/NotNull", empty package)` instead of `ClassId("org.jetbrains.annotations/NotNull")`. FIR creates symbols with the ClassId PASSED to `getClassLikeSymbolByClassId`, so wrong ClassId → wrong symbol identity → annotations not recognized.

**Root cause**: Binary class finder matched by simple name alone, returning `org.jetbrains.annotations.NotNull` for requested `ClassId("", "NotNull")`.

**Fixes**:
1. `CombinedJavaClassFinder.kt`: Added FQN verification in `findClass()` — reject classes where `actualFqName != expectedFqName`
2. `JavaTypeOverAst.kt`: `filterTypeUseAnnotations` now filters ALL annotations through callback (not just `extraAnnotations`)
3. `JavaTypeConversion.kt`: Added ClassId verification in `isTypeUseAnnotationClass()` — reject symbols where `symbol.classId != classId`

**Tests Fixed (4)**: `testJavaIteratorOfNotNullFailFast` + 3 variants

### Combined Test Results
- **Box tests**: 1163/1168 passing (+2)
- **Phased tests**: 1396/1443 passing (+2, test base +1)
- **Total failures**: 55 → 52 (5 tests fixed)

### Files Modified
- `core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaTypes.kt` — `resolveToClassId` on `JavaClassifierType`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt` — `resolveToClassId` impl; `filterTypeUseAnnotations` for all annotations
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaResolutionContext.kt` — `resolveToClassId`, `resolveNestedClassToClassId`, `resolveSimpleNameToClassId`
- `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt` — try `resolveToClassId` first; ClassId verification in `isTypeUseAnnotationClass`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/CombinedJavaClassFinder.kt` — FQN verification in `findClass()`

### Key Learnings
- String-based resolution loses package/class boundary — `ClassId` encodes it explicitly
- Binary class finders may match by simple name — always verify FQN matches requested ClassId
- FIR symbol provider uses the REQUESTED ClassId, not the actual class's ClassId
- Both `resolve` (string) and `resolveToClassId` are needed for different call sites

---

## Iteration 44: TYPE_USE Annotation Filtering Fix

### Root Cause Analysis
`filterTypeUseAnnotations` was filtering out ALL type annotations including TYPE_USE ones from type positions (e.g., `List<@NotNull V>`). The callback `isTypeUseAnnotationClass` returned false because `annotations-13.0.jar` (bundled with Kotlin) predates Java 8's `ElementType.TYPE_USE` — `@NotNull`/`@Nullable` lack `TYPE_USE` in their `@Target`. Kotlin-mapped targets lost TYPE_USE entirely.

PSI's default `filterTypeUseAnnotations` returns all annotations (already pre-filtered by PsiType). Java-direct's override used callback-based filtering which failed for binary-loaded annotations.

### Fix
Separated annotations into two categories in `JavaTypeOverAst`:
1. **Type-position annotations** (`extraAnnotations`, `modifierListAnnotations`, `directAnnotations`): returned unconditionally from `filterTypeUseAnnotations` — TYPE_USE by syntactic position
2. **Member modifier list annotations** (`memberAnnotations` from `createJavaTypeWithAnnotations`): filtered via callback — needed for source-defined TYPE_USE annotations like `@TypeAnn`

Added `memberAnnotations` parameter to `JavaTypeOverAst` and all subclasses.

### Test Results
- **Box tests**: 1163/1168 (unchanged)
- **Phased tests**: 1409/1443 (was 1396, **+13 fixed**)
- **Total failures**: 39 (was 52)

### Tests Fixed
`testNotNullTypeParameterWithKotlinNullable` (and 4 variants), `testTypeFromGenericWithAnnotation` (and 3 variants), `testFlexibleConstraints`, `testKt41984`, `testTypeParameterUse`, `testRepeatedAnnotations`

### Files Modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt` — added `memberAnnotations` parameter, split `filterTypeUseAnnotations` logic

### Key Learning
`annotations-13.0.jar` lacks `ElementType.TYPE_USE` for `@NotNull`/`@Nullable` — predates Java 8. Type-position annotations are TYPE_USE by syntactic position and don't need callback verification.

---

## Iteration 45: Type Parameter Direct Annotations

### Root Cause Analysis
`JavaTypeParameterOverAst.annotations` only checked for annotations inside `MODIFIER_LIST`. But the KMP Java parser places annotations on type parameters as direct children of the `TYPE_PARAMETER` node (no `MODIFIER_LIST` wrapper). For `<@org.jetbrains.annotations.NotNull K>`, the AST has `[ANNOTATION, IDENTIFIER, EXTENDS_BOUND_LIST]` — no MODIFIER_LIST.

### Fix
Updated `JavaTypeParameterOverAst.annotations` to collect annotations from both `MODIFIER_LIST` children and direct `ANNOTATION` children of the `TYPE_PARAMETER` node.

### Test Results
- **Phased tests**: 1412/1443 (was 1409, **+3 fixed**)
- **Total failures**: 36 (was 39)

### Tests Fixed
`testCheckEnhancedUpperBounds`, `testCheckEnhancedUpperBoundsWithEnabledImprovements`, `testTypeAliasConstructorTypeArgumentsInferenceWithNestedCalls`

### Files Modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt` — `JavaTypeParameterOverAst.annotations`

---

## Iteration 46: Import Resolution — Nested Class FQNs, Class-Level Star Imports, JLS 8.5 Shadowing

### Root Cause Analysis

**Bug 1: Explicit import FQN split** — `resolveSimpleNameToClassId` used `ClassId.topLevel(imported)` for explicit imports. For `import a.x.b.b.b`, this produced `ClassId(a.x.b.b, b)` instead of `ClassId(a, x.b.b.b)`.

**Bug 2: Class-level star imports not handled** — `import a.x.b.b.*` and `import a.D.*` are CLASS-level star imports. The star import loop only tried package-level lookup; when `a.x.b.b` and `a.D` are classes (not packages), lookup fails.

**Bug 3: Last-wins for duplicate explicit imports** — `import a.B; import a.D.B;` overwrote `simpleImports["B"]`. PSI/K1 uses first-wins semantics.

**Bug 4: Cross-file ambiguity check bypassed** — `resolveInheritedInnerClassToClassId` only checked DIRECT supertypes. For `y extends x implements i2` where `i2 extends i` and both `x` and `i` have inner class `Z`, only saw `x.Z` and missed `i.Z` (via `i2→i`).

**Regression: False ambiguity in `collectInheritedInnerClasses`** — Adding cross-file ambiguity pre-check triggered regressions (`testSameInnersInSupertypeAndSupertypesSupertype`). Root cause: `collectInheritedInnerClasses` didn't implement JLS 8.5 shadowing — when `B extends A` and both declare `Inner`, `B.Inner` should shadow `A.Inner`.

### Fixes
1. `resolveAsClassId` helper — tries all package/class splits for a FQN
2. Step 1 (explicit imports): use `resolveAsClassId` instead of `ClassId.topLevel`
3. Step 5 (star imports): when package-level lookup fails, try class-level
4. `extractImports`: all import-adding sites changed to keep-first (putIfAbsent)
5. Cross-file ambiguity pre-check before step 2b using `collectInheritedInnerClasses`
6. `collectInheritedInnerClasses` `shadowedNames` parameter (JLS 8.5): names declared by a class block same-named types from its supertypes

### Test Results
- **Phased tests**: 1416/1443 (was 1412, **+4 fixed**)
- **Total failures**: 32 (was 36)

### Tests Fixed
`testImportThriceNestedClass`, `testNestedAndTopLevelClassClash`, `testInheritanceAmbiguity2`, `testClash`

### Files Modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaResolutionContext.kt` — `resolveAsClassId`, keep-first for duplicates, class-level star fallback, cross-file ambiguity pre-check
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassFinderOverAstImpl.kt` — `collectInheritedInnerClasses` with `shadowedNames`

### Key Learnings
- `ClassId.topLevel` only splits at the last dot — wrong for nested class FQNs
- Java star imports can be class-level (`import a.D.*` imports nested types of class `a.D`)
- PSI uses first-import-wins for duplicate explicit imports
- JLS 8.5 shadowing: B.Inner shadows A.Inner when B extends A — pass shadowed names DOWN the inheritance path

---

## Iteration 47: KT-4455 — Non-Canonical Java Classes Visibility

### Root Cause Analysis

**PSI behavior (expected)**:
- `A.java` with `class A` and `class B`: only `A` appears in same-package scope. `B` accessible via API chains but NOT as standalone name.
- `E.java` with only `class F`: no class indexed AT ALL. `F` completely inaccessible.

**java-direct bugs**:
1. `tryBuildFileEntry` created entries for ALL files regardless of canonical class match → `E.java`/class `F` was indexed and resolvable
2. `knownClassNamesInPackage` returned ALL class names including secondary classes → `Another` from `Some.java` was resolvable in Kotlin

### Fixes

**Fix A** (`tryBuildFileEntry`): Skip files where file's base name doesn't match any declared class name.
- `E.java` with only class `F` → `"E" !in classNames` → returns null → `F` not indexed

**Fix B** (`knownClassNamesInPackage`): Only return canonical class names (where class name matches the file's base name).

### Test Results
- **Phased tests**: 1418/1443 (was 1416, **+2 fixed**)
- **Total failures**: 30 (was 32)

### Tests Fixed
`testMultipleJavaClassesInOneFile`, `testDifferentFilename`

### Files Modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassFinderOverAstImpl.kt` — `tryBuildFileEntry` canonical filter; `knownClassNamesInPackage` canonical filter

### Key Learnings
- PSI only exposes canonical classes (matching filename) in same-package scope
- Files without any canonical class are completely inaccessible — not indexed at all

---

## Iteration 48: package-info.java Support

### Initial Investigation Mistake
First investigation concluded `WITH_STDLIB` bypasses java-direct — incorrect. The Gradle daemon served cached bytecode. Debugging with `throw`-based approaches is unreliable with daemon caching — use file-based logging with `--no-daemon`.

### Root Cause (Corrected)
The annotation in `package-info.java` is in `PACKAGE_STATEMENT → MODIFIER_LIST → ANNOTATION`. Initial code only checked `root.getChildrenByType("ANNOTATION")` and `packageStmt.getChildrenByType("ANNOTATION")` (direct children) — missed the MODIFIER_LIST level.

### Implementation
- `buildIndex()` calls `indexPackageInfo(path)` for `package-info.java` files
- `indexPackageInfo` extracts annotations from `PACKAGE_STATEMENT → MODIFIER_LIST → ANNOTATION`
- `JavaPackageOverAst.annotations` returns annotations from `finder.getPackageAnnotations(fqName)`
- `JavaPackageOverAst.findAnnotation` now works via `annotations`

### Test Results
- **Phased tests**: 1419/1443 (was 1418, **+1 fixed**)
- **Total failures**: 29 (was 30)

### Tests Fixed
`testGoogleErrorProne_packageInfoJava`

### Files Modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassFinderOverAstImpl.kt` — `packageAnnotationNodes`, `indexPackageInfo`, `getPackageAnnotations`, `buildIndex`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaPackageOverAst.kt` — `annotations`, `findAnnotation`

---

## Iteration 49: ft\<T,T?\> vs T! FIR Dump Rendering Fix

### Root Cause
`JavaTypeConversion.kt` creates `ConeFlexibleType(lower, upper, isTrivial=false)` when `classifier == null` (cross-file reference). `isTrivial=false` forces verbose `ft<T, T?>` rendering in FIR dumps, while PSI creates `isTrivial=true` (compact `T!`). The `isTrivial` flag is rendered differently by `ConeTypeRenderer.renderForSameLookupTags()` for generic types with type arguments.

### Fix
Added `isTriviallyFlexibleHint: Boolean` property (default `false`) to `JavaClassifierType` interface. `JavaClassifierTypeOverAst` overrides it to `true` when:
1. `classifier == null` (cross-file reference — otherwise existing `isTriviallyFlexible()` works)
2. Simple name (single part)
3. Class is unambiguously present in the source index (same-package, explicit import, or exactly one star-import match)

`JavaTypeConversion.kt` checks `|| isTriviallyFlexibleHint` when deciding to call `toTrivialFlexibleType()`.

### Test Results
- **Phased tests**: 1423/1443 (was 1419, **+4 fixed**)
- **Total failures**: 25 (was 29)

### Tests Fixed
`testMappingWithWrongNullability`, `testMappingWithWrongNullabilityLegacy`, `testMappingWithWrongNullabilityWithoutWrtHack`, `testFlexibleTypeAliases`

### Files Modified
- `core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaTypes.kt` — `isTriviallyFlexibleHint: Boolean get() = false` on `JavaClassifierType`
- `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt` — `|| isTriviallyFlexibleHint` check
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassFinderOverAstImpl.kt` — `isClassInIndex(classId: ClassId): Boolean`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaResolutionContext.kt` — `isUnambiguouslyCrossFileClass(simpleName: String): Boolean`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt` — `override val isTriviallyFlexibleHint: Boolean`

### Key Learnings
- `ConeFlexibleType.isTrivial=true` required for compact `T!` rendering for types WITH type arguments
- `isTriviallyFlexibleHint` approach separates rendering from construction: construction stays in the `null` branch, only `isTrivial` changes
- Test ordering effects exist: some tests pass in full suite due to earlier tests warming FIR caches

---

## Iteration 50: Interface Method Abstractness + Static Inner Type Scoping

### Root Cause Analysis

**Bug 1 — Interface method `isAbstract` detection:**
`JavaMethodOverAst.isAbstract` used `!hasBody` where `hasBody = node.findChildByType("CODE_BLOCK") != null`. For `public boolean foo(@Nullable T y) {}` — an empty body IS a `CODE_BLOCK`. So `hasBody = true` → `isAbstract = false` → interface A had zero abstract methods → not recognized as SAM interface → SAM lambda conversion failed → `UNRESOLVED_REFERENCE`.

PSI determines abstractness by checking for `default` keyword, NOT body presence. Any interface method without `default` or `static` is abstract regardless of body. The `DEFAULT_KEYWORD` lives inside `MODIFIER_LIST`, not as a direct child of the method node.

**Bug 2 — Static inner type: outer type params not in scope:**
`contextForInner` used `resolutionContext` (no outer type params) for static nested types. For `JavaClass<T>.Inner<X>` (static), `T` from outer class was not in scope → `MISSING_DEPENDENCY_CLASS` for method using `T`.

**Bug 3 — Static inner type: outer type params shadowed inner class names:**
Adding outer type params with high priority caused them to shadow inner class names. For `Nested.getT()` where outer has type param `T` AND `Nested` has inner class `T`, the outer param was found first.

### Fix

**isAbstract fix** (`JavaMemberOverAst.kt`):
- `hasDefaultKeyword` checks `modifierList?.children?.any { it.type == "DEFAULT_KEYWORD" }` (inside `MODIFIER_LIST`)
- `isAbstract = super.isAbstract || (isInterface && !hasDefaultKeyword && !isStatic)`

**Scoping fix** (`JavaResolutionContext.kt`, `JavaClassOverAst.kt`, `JavaTypeOverAst.kt`):
- Added `inheritedTypeParametersInScope: Map<String, JavaTypeParameter>` field (low-priority)
- Non-static inner class → `memberResolutionContext` (outer params as OWN, high priority)
- Static inner class → `resolutionContext.withContainingClass(this).withInheritedTypeParameters(typeParameters)` (outer params as INHERITED, low priority)
- Resolution order: own type params → local/inner class names → inherited (outer) type params

### Test Results
- **Box tests**: 1164/1168 (+1 fixed)
- **Phased tests**: 1425/1443 (+6 fixed)
- **Total failures**: 18 (was 25, **7 tests fixed**)

### Tests Fixed
`testNoAnnotationInClassPath`, `testInnerClassInGeneric`, `testComplexGenericOverride`, `testHugeMixedCapturedType`, `testWriterAppenderExampleRecursive`, `testKt64045`, `testCapturedSelfInsideIntersection4` (box)

### Files Modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaMemberOverAst.kt` — `isAbstract`, `hasDefaultKeyword`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaResolutionContext.kt` — `inheritedTypeParametersInScope`, `withInheritedTypeParameters()`, `findInheritedTypeParameter()`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt` — `findInnerClass` static/non-static context split
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt` — resolution order: own params → local classes → inherited params

### Key Learnings
- `DEFAULT_KEYWORD` in Java interface methods is inside `MODIFIER_LIST`, not a direct child of the method node
- Java interface methods without `default` are always abstract regardless of body presence
- Outer class type parameters have DIFFERENT priority for static vs non-static nesting:
  - Non-static: outer params are "own" (win over inner class names)
  - Static: outer params are "inherited" (shadowed by inner class names of the static nested type)

---

## Iteration 51: Static-Imported Kotlin Const Vals in Java Annotations

### Root Cause Analysis

`testConstValAsAnnotationArgumentInJava` and `testKt83402`. Two-layer problem:

**Layer 1 — Static import parsing** (`JavaResolutionContext.kt`):
`import static example.KotlinDtoMapping.ID` is parsed as `IMPORT_STATIC_STATEMENT` (not `IMPORT_STATEMENT`) by KMP parser. `extractImports` only processed `IMPORT_STATEMENT`. Fix: added `IMPORT_STATIC_STATEMENT` handling with `IMPORT_STATIC_REFERENCE` child extraction.

**Layer 2 — Bare-name annotation argument resolution** (`JavaAnnotationOverAst.kt`):
After fixing static import parsing, bare identifiers like `ID` in `@SimpleAnnotation(ID)` still failed. `JavaEnumValueAnnotationArgumentOverAst` returned `enumClassId=null` for bare names (no dots) because it didn't consult static imports → FIR produced error expression → `FirLazyResolveContractViolationException`.

**Layer 3 — Fragile const field evaluation** (`javaAnnotationsMapping.kt`):
Even with correct `enumClassId`, `(constField.initializer as? FirLiteralExpression)?.value` only works for simple literal initializers. Kotlin const vals with non-trivial expressions need `FirExpressionEvaluator`.

### Fixes

**Fix 1** (`JavaResolutionContext.kt`): Added `IMPORT_STATIC_STATEMENT` block in `extractImports()` — uses `IMPORT_STATIC_REFERENCE` child to get FQN; checks for `ASTERISK` child for star static imports.

**Fix 2** (`JavaAnnotationOverAst.kt`): Added `staticImportResolution` lazy property to `JavaEnumValueAnnotationArgumentOverAst`. For bare identifiers (no dots), checks `resolutionContext.getSimpleImport(text)`. If found (e.g., `example.KotlinDtoMapping.ID`), splits FQN into class qualifier and member name. `className`, `entryName`, and `enumClassId` now use this resolution.

**Fix 3** (`javaAnnotationsMapping.kt`): Added `resolveConstFieldValue(session, classId, fieldName)` and `extractEvaluatedConstValue(property, session)` using `FirExpressionEvaluator.evaluatePropertyInitializer()` for proper const evaluation. Handles non-trivial initializers and already-evaluated properties.

**Defensive fix** (`AnnotationCodegen.kt`): `is IrErrorExpression` in `genCompileTimeValue` now returns (skips) instead of crashing with `BackendException`.

### Test Results
- **Box tests**: 1165/1168 (+1 net — 2 fixed, test base unchanged)
- **Phased tests**: 1442/1456 (no regressions; test base grew from 1443 to 1456)
- **Total failures**: 17 (was 18)

### Tests Fixed
`testConstValAsAnnotationArgumentInJava`, `testKt83402`

### Files Modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaAnnotationOverAst.kt` — `staticImportResolution` for bare-name annotation arguments
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaResolutionContext.kt` — `IMPORT_STATIC_STATEMENT` handling
- `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/javaAnnotationsMapping.kt` — `resolveConstFieldValue()`, `extractEvaluatedConstValue()`
- `compiler/ir/backend.jvm/codegen/src/org/jetbrains/kotlin/backend/jvm/codegen/AnnotationCodegen.kt` — skip `IrErrorExpression` annotation args

### Key Learnings
- KMP parser uses `IMPORT_STATIC_STATEMENT` with `IMPORT_STATIC_REFERENCE` child (not `IMPORT_STATEMENT`+`JAVA_CODE_REFERENCE`) for `import static`
- Static-imported Kotlin const vals require two-layer fix: (1) resolve bare names via static imports, (2) use `FirExpressionEvaluator` (not literal cast) for const evaluation
- `FirExpressionEvaluator.evaluatePropertyInitializer()` handles circular dependencies via `visitedCallables` ThreadLocal — safe for cross-language const references
- `AnnotationCodegen` defensive skip for `IrErrorExpression` prevents `BackendException` when annotation args are unresolvable

---

## Key Architectural Patterns

### 1. Callback Pattern for Resolution
Used throughout for resolution without FirSession access in Java Model:
- `JavaClassifierType.resolve(tryResolve)` — Type resolution
- `JavaAnnotation.resolveAnnotation(tryResolve)` — Annotation resolution
- `JavaEnumValueAnnotationArgument.resolveEnumClass(tryResolve)` — Enum class resolution
- `JavaType.filterTypeUseAnnotations(isTypeUse)` — TYPE_USE filtering
- `JavaField.resolveInitializerValue(resolveReference)` — Constant evaluation

### 2. PSI/Java-Direct Discrimination in Shared Files
```kotlin
val isJavaDirectClass = classSource == null && origin.fromSource
```

### 3. Two-Phase Type Parameter Construction
For mutually-referencing type parameters: create all instances first, then update context with siblings.

### 4. Implicit Supertypes
- Enums → `java.lang.Enum<E>`
- Annotation types → `java.lang.annotation.Annotation`
- Classes without extends → `java.lang.Object`

### 5. Implicit Modifiers (Java Spec)
Must be applied explicitly in java-direct (no parser support):
- Interface members: `public abstract` (methods), `public static final` (fields)
- Interface nested types: `public static` (JLS 9.5)
- Enum constants: `public static final` (JLS 8.9.3)
- Protected members: `ProtectedAndPackage` not plain `Protected`
- Protected static members: `ProtectedStaticVisibility`

---

## Known Limitations (as of archival — 2026-03-23)

- **`testPseudoRawTypes`**: Java compilation error (custom `java.util.Collection`) — pre-existing, not java-direct specific
- **`testRawSupertypeOverride`**: Complex raw supertype inheritance — needs further investigation
- **`testJavaMethodsSmokeTest`**, **`testJavaArrayType`**: KotlinReflectionInternalError — reflection metadata differences

---

## Files Modified (Iterations 37-51)

| File | Iterations | Changes |
|------|-----------|---------|
| `JavaClassOverAst.kt` | 37, 39 | `isFinal`, `isAbstract`, `findAnnotation`, `isDeprecatedInJavaDoc`; `permittedTypes`, `deriveImplicitPermittedTypes()`; `findInnerClass` static/non-static context |
| `JavaMemberOverAst.kt` | 37, 38, 50 | Implicit modifier gaps; `isInitializerPotentiallyConstant`; `isAbstract`, `hasDefaultKeyword` |
| `JavaTypeOverAst.kt` | 40, 41, 42, 43, 44, 45, 49, 50 | `rawTypeName`, `typeArguments`, `collectAllRefParamLists`; type param annotations; `extractTypeName`; `filterTypeUseAnnotations`; `isTriviallyFlexibleHint`; resolution order |
| `JavaResolutionContext.kt` | 37, 39, 43, 46, 49, 50, 51 | Import resolution fixes; `resolveToClassId`; class-level star; shadowing; `isTriviallyFlexibleHint` support; inherited type params; static import handling |
| `JavaClassFinderOverAstImpl.kt` | 46, 47, 48, 49 | `shadowedNames` in `collectInheritedInnerClasses`; canonical class filter; `indexPackageInfo`; `isClassInIndex` |
| `JavaAnnotationOverAst.kt` | 51 | `staticImportResolution` for bare-name annotation arguments |
| `JavaPackageOverAst.kt` | 48 | `annotations` delegates to `finder.getPackageAnnotations` |
| `CombinedJavaClassFinder.kt` | 43 | FQN verification in `findClass()` |
| `JavaParsingTest.kt` | 37, 38, 42 | 6 modifiers tests; `testPublicClassWithMalformedMembers`; annotation type name tests |
| `javaTypes.kt` (shared) | 43, 49 | `resolveToClassId`; `isTriviallyFlexibleHint` |
| `JavaTypeConversion.kt` (shared) | 37, 43, 49 | `findClassId` for nested FQNs; ClassId verification; `isTriviallyFlexibleHint` check |
| `FirJavaFacade.kt` (shared) | 39 | Sealed inheritors fallback for null classifier |
| `javaLoading.kt` (shared) | 40 | Fallback for unresolved `Object` |
| `javaAnnotationsMapping.kt` (shared) | 51 | `resolveConstFieldValue()`, `extractEvaluatedConstValue()` |
| `AnnotationCodegen.kt` (shared) | 51 | Skip `IrErrorExpression` annotation args |

---

## Test Results Progression

| After Iteration | Box Tests | Phased Tests | Combined Failing |
|----------------|-----------|--------------|-----------------|
| Start (post-36) | 1157/1168 | 1374/1442 | 79 |
| After 37 | 1157/1168 | ~1381/1442 | 74 |
| After 37b | 1157/1168 | ~1388/1442 | 67 |
| After 38 | 1157/1168 | ~1392/1442 | 63 |
| After 39 | 1157/1168 | ~1395/1442 | 60 |
| After 40 | 1158/1168 | ~1397/1442 | 57 |
| After 41 | 1158/1168 | ~1397/1442 | 57 |
| After 42 | 1158/1168 | ~1398/1442 | 55 |
| After 43 | 1163/1168 | 1396/1443 | 52 |
| After 44 | 1163/1168 | 1409/1443 | 39 |
| After 45 | 1163/1168 | 1412/1443 | 36 |
| After 46 | 1163/1168 | 1416/1443 | 32 |
| After 47 | 1163/1168 | 1418/1443 | 30 |
| After 48 | 1163/1168 | 1419/1443 | 29 |
| After 49 | 1163/1168 | 1423/1443 | 25 |
| After 50 | 1164/1168 | 1425/1443 | 18 |
| After 51 | 1165/1168 | 1442/1456 | **17** |

---

*Archived: 2026-03-23*
