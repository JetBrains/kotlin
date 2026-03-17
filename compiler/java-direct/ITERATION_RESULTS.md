# Java-Direct: Iteration Results Log

**Current status**: See `FIXING_ITERATIONS.md` for test counts and remaining work.

**Last Updated**: 2026-03-17 (iter 39)

---

## Archives

| Archive | Iterations | Result |
|---------|-----------|--------|
| `implDocs/archive/ITERATIONS_1_6_DETAILS.md` | 1–6 | 0 → 90/138 box (65.2%) |
| `implDocs/archive/ITERATIONS_7_16_DETAILS.md` | 7–16 | 90 → 1075/1166 box (92.2%) |
| `implDocs/archive/ITERATIONS_17_23_DETAILS.md` | 17–23 | 1075 → 1150/1167 box, 1374/1442 phased (95.3%) |
| `implDocs/archive/ITERATIONS_24_26_DETAILS.md` | 24–26 | 1150/1167 → same, phased 300 → 1374/1442 |
| `implDocs/archive/ITERATIONS_27_36_DETAILS.md` | 27–36 | 1150/1167 → 1157/1168 box, **79 combined failing** |

---

## Iteration 37: Implicit Java Modifiers Area Audit — 2026-03-16

### Approach
Reference-first area audit: compared `JavaClassOverAst` and `JavaMemberOverAst` against `TreeBasedClass`, `TreeBasedField`, `TreeBasedMethod`, `TreeBasedConstructor` (javac-wrapper).

### Gaps Found and Fixed

| Gap | Reference behavior | java-direct before | Fix |
|-----|-------------------|-------------------|-----|
| `JavaClassOverAst.isFinal` | `isEnum \|\| isFinal` | only `isFinal` | Add `isEnum &&` guard (not final if has abstract methods) |
| `JavaClassOverAst.isAbstract` | `isAbstract \|\| (isAnnotationType\|isEnum) && methods.any { abstract }` | `isAbstract \|\| isInterface` | Add enum/annotation abstract method case |
| `JavaClassOverAst.findAnnotation` | `annotations.find {...}` | **always `null`** | Delegate to `annotations` collection |
| `JavaMethodOverAst.isNative` | `modifiers.isNative` | **always `false`** | `hasModifier("NATIVE_KEYWORD")` |
| `JavaConstructorOverAst.isFinal/isAbstract/isStatic` | `true/false/false` | inherited (wrong) | Override explicitly |
| `isDeprecatedInJavaDoc` (all elements) | check Javadoc comment | **always `false`** | `DOC_COMMENT` child text contains `@deprecated` |

### Unit Tests Added (`JavaParsingTest.kt`)
- `testEnumImplicitFinal` — plain enum is final, enum with abstract methods is not
- `testAnnotationTypeImplicitAbstract` — annotation with methods is abstract
- `testFindAnnotationOnClass` — findAnnotation finds/misses correctly on class
- `testNativeMethod` — isNative true/false
- `testConstructorImplicitFinal` — isFinal/isAbstract/isStatic on constructor
- `testDeprecatedInJavaDoc` — class, method, field with/without Javadoc @deprecated

### Test Results
- **Box tests**: 11 → 8 (+3 fixed: `testJavaMutableListThroughKotlin`, `testJavaAnnotationConstructorTypes`, `testJavaVisibility`)
- **Phased tests**: 68 → 66 (+2 fixed: various deprecated/javadoc tests)
- **Total failures**: 79 → 74 (5 tests fixed)
- PSI regression tests: All passing ✅

### Files Modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt` — `isFinal`, `isAbstract`, `findAnnotation`, `isDeprecatedInJavaDoc`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaMemberOverAst.kt` — `isDeprecatedInJavaDoc`, `isNative`, `hasModifier` protected; `JavaConstructorOverAst` overrides
- `compiler/java-direct/test/org/jetbrains/kotlin/java/direct/JavaParsingTest.kt` — 6 new unit tests

### Key Learning
All 6 gaps shared the same root cause: implicit Java modifiers/behaviors not captured in java-direct. The area audit found all of them in one pass vs. finding them individually through failing tests.

---

## Iteration 37: Reference Audit — Implicit Modifiers + Import Resolution — 2026-03-16

### Approach
Reference-first area audit: compared `JavaClassOverAst`/`JavaMemberOverAst` against `TreeBasedClass`/`TreeBasedField`/`TreeBasedMethod`/`TreeBasedConstructor` (javac-wrapper). Then fixed import resolution bugs in `JavaResolutionContext.kt` and `JavaTypeConversion.kt`.

---

### Part A: Implicit Java Modifiers (area audit)

**Gaps found and fixed:**

| File | Gap | Reference behavior | Fix |
|------|-----|--------------------|-----|
| `JavaClassOverAst.kt` | `isFinal` | `isEnum \|\| isFinal` | Add enum guard (JLS 8.9); not final if has abstract methods |
| `JavaClassOverAst.kt` | `isAbstract` | `isAbstract \|\| (isAnnotationType\|isEnum) && methods.any { abstract }` | Add enum/annotation abstract method case |
| `JavaClassOverAst.kt` | `findAnnotation` | `annotations.find {...}` | Was always `null`; delegate to `annotations` collection |
| `JavaClassOverAst.kt` | `isDeprecatedInJavaDoc` | check Javadoc comment | Check `DOC_COMMENT` child node text for `@deprecated` |
| `JavaMemberOverAst.kt` | `isDeprecatedInJavaDoc` | check Javadoc comment | Same: `DOC_COMMENT` child text contains `@deprecated` |
| `JavaMemberOverAst.kt` | `isNative` | `modifiers.isNative` | Was always `false`; now `hasModifier("NATIVE_KEYWORD")` |
| `JavaConstructorOverAst` | `isFinal/isAbstract/isStatic` | `true/false/false` | Override explicitly (constructors are always final, not abstract, not static) |

**`hasModifier` made `protected`** to allow `JavaMethodOverAst` to call it.

**Unit tests added** (`JavaParsingTest.kt`):
- `testEnumImplicitFinal` — plain enum is final; enum with abstract methods is not
- `testAnnotationTypeImplicitAbstract` — annotation type with methods is abstract
- `testFindAnnotationOnClass` — findAnnotation finds/misses on class
- `testNativeMethod` — isNative true/false
- `testConstructorImplicitFinal` — isFinal/isAbstract/isStatic on constructor
- `testDeprecatedInJavaDoc` — class, method, field with/without `@deprecated`

---

### Part B: Import Resolution Bugs

**Three bugs fixed:**

**1. Explicit imports not checked in `resolveWithCallback`** (`JavaResolutionContext.kt`)
- `resolveSimpleName` checked same-package *before* explicit single-type imports
- JLS 7.5.1: explicit imports shadow same-package classes
- Fix: added step 0 — check `simpleImports[simpleName]` first, then same-package
- Fixed: `testCurrentPackageAndExplicitNestedImport` and cascading import tests

**2. Duplicate star imports caused false ambiguity** (`JavaResolutionContext.kt`)
- `starImports` is a `List`, so `import weatherForecast.*` twice → same package tried twice → false "ambiguous" result
- Fix: use `.distinct()` when iterating star imports for ambiguity check
- Fixed: `testImportTwoTimesStar`

**3. `ClassId.topLevel` used for resolved nested class FQNs** (`JavaTypeConversion.kt`)
- When `isResolved = true` (explicit import found), the code used `ClassId.topLevel(FqName("a.X.Y"))` which splits as package=`a.X`, class=`Y` — WRONG for nested classes
- Correct for `a.X.Y` is package=`a`, class=`X.Y` = `ClassId(FqName("a"), FqName("X.Y"))`
- Fix: replace `ClassId.topLevel(FqName(qualifiedName))` with `findClassId(qualifiedName, session) ?: ClassId.topLevel(...)` which probes all package/class splits
- Fixed: `testCurrentPackageAndExplicitNestedImport` (properly), `testNestedClassClash`, `testNestedAndTopLevelClassClash`, `testImportThriceNestedClass`, `testNested`, `testSimpleCorrect`, `testInnerLightClass`

**Default-package lookup added**: `resolveSimpleName` now also tries the simple name directly for root-package code (was previously skipped when `packageFqName.isRoot`).

---

### Net Test Results

**Tests FIXED (8):**
`testCurrentPackageAndExplicitNestedImport`, `testImportTwoTimesStar`, `testNestedAndTopLevelClassClash`, `testNestedClassClash`, `testImportThriceNestedClass`, `testNested`, `testSimpleCorrect`, `testInnerLightClass`

**New regressions (5)** — `BoxJvm > Ranges > JavaInterop`:
`testJavaCollectionOfExplicitNotNullFailFast`, `testJavaCollectionOfNotNullToTypedArrayFailFast`, `testJavaIteratorOfNotNullFailFast`, `testJavaCollectionOfExplicitNotNullWithIndexFailFast`, `testJavaIteratorOfNotNullWithIndexFailFast`
→ Root cause not yet investigated; likely caused by `findClassId` change in `JavaTypeConversion.kt`

**Net improvement: 74 → 67 failures (−7) after regression fix**

**Regression investigation**: Initial implementation included default-package lookup (`tryResolve(simpleName)` when `packageFqName.isRoot`) to handle same-package types in default-package files. This broke 5 `FailFast` tests (`BoxJvm > Ranges > JavaInterop`). Root cause: for `J.java` (default package), the default-package lookup caused `isTypeUseAnnotation("NotNull")` to be called with the simple name instead of the FQN, which returned false, causing the `@NotNull` type-use annotation to be dropped — nullability assertions not generated — NPE not thrown — test fails. Fix: removed default-package lookup. `testSimpleCorrect` reverts to failing (was already failing before iter 37b).

PSI regression tests (`PhasedJvmDiagnosticLightTreeTestGenerated.*`): All passing ✅

### Files Modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt` — `isFinal`, `isAbstract`, `findAnnotation`, `isDeprecatedInJavaDoc`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaMemberOverAst.kt` — `isDeprecatedInJavaDoc`, `isNative`, `hasModifier` made protected; `JavaConstructorOverAst` overrides
- `compiler/java-direct/test/org/jetbrains/kotlin/java/direct/JavaParsingTest.kt` — 6 new unit tests
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaResolutionContext.kt` — explicit imports before same-package; `.distinct()` for star imports; default-package lookup
- `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt` — `findClassId` instead of `ClassId.topLevel` for resolved nested types

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

---

## Iteration 38: Const Evaluation Correctness + Malformed Constructor Fix — 2026-03-17

### Root Cause Analysis

**Bug A: `hasConstantNotNullInitializer` (java-direct) returned true for non-constant initializers**

PSI's `JavaFieldImpl.hasConstantNotNullInitializer()` evaluates the expression and returns `false` for method calls or unresolvable references. Java-direct's implementation only did a structural check (final + primitive/String type + has initializer), returning `true` even for `System.currentTimeMillis()` or `SOME_WRONG_EXPRESSION`. Since FIR's `FirConstCheckVisitor.visitPropertyAccessExpression` returns `VALID_CONST` for any `FirFieldSymbol` with `hasConstantInitializer=true`, these fields were incorrectly treated as compile-time constants.

**Bug B: `void () {}` treated as constructor in malformed Java class**

In the AST produced by the Java parser for the malformed method `void () {}`, the `void` keyword ends up as `ERROR_ELEMENT` rather than a `TYPE` node (because there's no method name after the return type). `JavaClassOverAst.constructors` filtered for `it.findChildByType("TYPE") == null`, so this malformed node was treated as a constructor with package-private visibility. Since `hasDefaultConstructor() = false` (an "explicit" constructor was found), FIR produced `INVISIBLE_REFERENCE` when calling `Nameless()`.

### Fixes

**Fix A: `isInitializerPotentiallyConstant` in `JavaMemberOverAst.kt`**

Added `isInitializerPotentiallyConstant(node)` helper that returns `false` for:
- Method calls and object creation (never JLS constant expressions)
- Simple (unqualified) name references to non-final same-class fields
- Simple name references with no local resolution and no import

Returns `true` for:
- Literals
- Arithmetic expressions (binary, polyadic, prefix, parenthesized)
- Qualified references (`Foo.BAR`) — might be cross-language constants resolvable via callback

This correctly handles:
- `System.currentTimeMillis()` → false (method call)
- `i3 = i1` (i1 non-final) → false (simple name found in class as non-final field)
- `SOME_WRONG_EXPRESSION` → false (simple name, not in class, not in imports)
- `Foo.FOO + 1` (cross-Kotlin) → true (qualified ref, might be resolvable via callback)
- `i4 = i2` (i2 final) → true (simple name found as final field)

**Important**: The fix was originally attempted in `FirJavaFacade.kt` (making `lazyHasConstantInitializer` depend on `lazyInit.value != null`). This was reverted because it broke `testKotlinJavaCycle` — cycles in cross-language const resolution caused null initializer values even for valid structural constants. The correct fix is at the java-direct model level.

**Fix B: `constructors` filter in `JavaClassOverAst.kt`**

Added `&& it.findChildByType("IDENTIFIER") != null` to the constructor filter. A real constructor declaration always has an IDENTIFIER (the class name). Malformed method nodes with `ERROR_ELEMENT` instead of TYPE but no IDENTIFIER are not valid constructors.

### Unit Tests Added
- `testPublicClassWithMalformedMembers` — verifies public visibility + no spurious constructors for class with `void () {}` and `int ;`

### Test Results
- **Box tests**: 8 → 8 (unchanged)
- **Phased tests**: 59 → 55 (+4 fixed)
- **Total failures**: 67 → 63 (4 tests fixed)
- PSI regression tests (`PhasedJvmDiagnosticLightTreeTestGenerated.*`): All passing ✅

**Tests FIXED (4):**
- `testFromJavaWithNonConstInitializer` — `System.currentTimeMillis()` no longer treated as constant
- `testJavaProperties` — `i3 = i1` (non-final ref) no longer treated as constant
- `testKt57802` — `SOME_WRONG_EXPRESSION` (unresolvable) no longer treated as constant
- `testNamelessInJava` — malformed `void () {}` no longer treated as constructor

### Files Modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaMemberOverAst.kt` — `hasConstantNotNullInitializer` now uses `isInitializerPotentiallyConstant`; added `isSimpleNamePotentiallyConstant`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt` — constructor filter requires IDENTIFIER
- `compiler/java-direct/test/org/jetbrains/kotlin/java/direct/JavaParsingTest.kt` — `testPublicClassWithMalformedMembers` unit test

---

## Iteration 39: Sealed Class Exhaustiveness + Supertype Inner Class Resolution — 2026-03-17

### Root Cause Analysis

**Bug A: Sealed class inheritors lost for cross-file permitted types**

`FirJavaFacade.createFirJavaClass` derives `sealedClassInheritors` via `classifierType.classifier as? JavaClass`. In java-direct, `classifier` is null for any type not defined in the same compilation unit (cross-file resolution requires the class finder). For `Base permits A, B` where A and B are in separate `.java` files, both classifiers were null → empty inheritor list → FIR treated the sealed class as having no known subtypes → all `when` expressions were incorrectly considered exhaustive → `NO_ELSE_IN_WHEN` errors were missing.

**Bug B: Implicit permits (no explicit `permits` clause) yielded empty permitted types**

For `sealed class SameFile { class A extends SameFile {} class B implements SameFile {} }`, there is no `permits` keyword. `JavaClassOverAst.permittedTypes` only looked for a `PERMITS_LIST` node and returned `emptySequence()` when not found. FIR then had no inheritors and again incorrectly treated all `when` as exhaustive.

**Bug C: Dotted supertype names not resolved with package prefix**

In `resolveFromSupertypesRecursive`, when a supertype has `classifierQualifiedName = "x.S"` (partially qualified, without the current package `a`), the code tried `nestedCandidate = "x.S.B"`. FIR's `findClassId("x.S.B", session)` tried ClassId splits that didn't include the package prefix `a`, so it never found `a.x.S.B`. The existing handling for package prefix only covered simple (non-dotted) supertype names.

### Fixes

**Fix A: `FirJavaFacade.kt` — fallback ClassId construction for cross-file permitted types**

When `classifierType.classifier as? JavaClass` is null, build a ClassId from `classifierType.classifierQualifiedName` using the current class's package:
```kotlin
ClassId(classId.packageFqName, FqName(qualifiedName), isLocal = false)
```
This correctly handles: `"A"` → `ClassId(ROOT, "A")`, `"B.C"` → `ClassId(ROOT, "B.C")` (inner class).

**Fix B: `JavaClassOverAst.kt` — `deriveImplicitPermittedTypes()`**

When `isSealed = true` and no `PERMITS_LIST` node exists, scan the class body for inner classes that directly extend/implement the sealed class (by checking their `EXTENDS_LIST`/`IMPLEMENTS_LIST`). Returns `SimpleClassifierType("SameFile.A")` etc., which the FIR fallback then resolves via Fix A.

**Fix C: `JavaResolutionContext.kt` — package prefix for dotted supertype names**

In `resolveFromSupertypesRecursive`, when the package is non-root and the supertype name contains a dot, also try `"${packageFqName}.$supertypeName.$simpleName"`. For `"x.S"` in package `a`: tries `"a.x.S.B"` → `findClassId("a.x.S.B", session)` → `ClassId(FqName("a"), FqName("x.S.B"))` → found.

### Test Results
- **Box tests**: 8 → 8 (unchanged)
- **Phased tests**: 55 → 52 (+3 fixed)
- **Total failures**: 63 → 60 (3 tests fixed)
- PSI regression tests (`PhasedJvmDiagnosticLightTreeTestGenerated.*`): All passing ✅

**Tests FIXED (3):**
- `testJavaSealedClassExhaustiveness` — sealed class with cross-file + in-file permitted types
- `testJavaSealedInterfaceExhaustiveness` — same for sealed interface
- `testInheritedInner2` — `x1.getB()` resolves `B` via supertype `x.S`

### Files Modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt` — `permittedTypes` with `deriveImplicitPermittedTypes()`
- `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/FirJavaFacade.kt` — sealed inheritors fallback for null classifier
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaResolutionContext.kt` — package prefix for dotted supertype names

---

## Known Limitations

- **`testPseudoRawTypes`**: Java compilation error (custom `java.util.Collection`) — pre-existing, not java-direct specific
- **`testRawSupertypeOverride`**: Complex raw supertype inheritance — needs further investigation
- **Static imports**: Not yet supported in annotation argument resolution

---

## Future Iteration Template

```markdown
## Iteration N: [Title] - YYYY-MM-DD

### Root Cause Analysis
[Reference-first: check javac-wrapper / PSI / git show origin/master first]

### Fix
[Files modified, solution description]

### Test Results
- Box: X/1168, Phased: X/1442, Total failing: N

### Key Learnings
```
