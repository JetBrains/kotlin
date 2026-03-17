# Java-Direct: Iteration Results Log

**Current status**: See `FIXING_ITERATIONS.md` for test counts and remaining work.

**Last Updated**: 2026-03-16 (iter 37/37b)

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
