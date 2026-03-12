# Java-Direct: Iteration Results Log

## Document Purpose

This file captures key findings, decisions, and learnings from each iteration.

**Last Updated**: 2026-03-12

---

## Progress Summary

| Phase | Iterations | Box Tests | Phased Tests |
|-------|------------|-----------|--------------|
| Foundation | 1-6 | 90/138 (65.2%) | — |
| Core Implementation | 7-16 | 139/142 → 1075/1166 (92.2%) | 242/327 (74.0%) |
| Advanced Features | 17-25c | 1150/1167 (98.5%) | 1326/1442 (92.0%) |

**Current Status**: ~134 total test failures (combined box + phased: 2649 tests, 2515 passing, 94.9%)

---

## Key Architectural Patterns

### 1. Callback Pattern for Resolution
Used throughout for resolution without FirSession access in Java Model:
- `JavaClassifierType.resolve(tryResolve)` — Type resolution
- `JavaAnnotation.resolveAnnotation(tryResolve)` — Annotation resolution
- `JavaEnumValueAnnotationArgument.resolveEnumClass(tryResolve)` — Enum class resolution
- `JavaType.filterTypeUseAnnotations(isTypeUse)` — TYPE_USE filtering
- `JavaField.resolveInitializerValue(resolveReference)` — Constant evaluation

### 2. PSI/Java-Direct Discrimination
When shared FIR code needs different behavior:
```kotlin
val isJavaDirectClass = classSource == null && origin.fromSource
```

### 3. Two-Phase Type Parameter Construction
For mutually-referencing type parameters:
1. Create all instances first
2. Update context with all siblings

### 4. Implicit Supertypes
Java classes have implicit inheritance:
- Enums → `java.lang.Enum<E>`
- Annotation types → `java.lang.annotation.Annotation`
- Classes without extends → `java.lang.Object`

---

## Iteration History

### Iterations 1-6: Foundation
**Archive**: `implDocs/archive/ITERATIONS_1_6_DETAILS.md`

Established: Hybrid class finder, import handling, type resolution architecture.

### Iterations 7-16: Core Implementation
**Archive**: `implDocs/archive/ITERATIONS_7_16_DETAILS.md`

Established: Array parsing, type parameter scope, wildcards, annotations, interface modifiers, external type handling, raw types.

### Iterations 17-23: Advanced Features
**Archive**: `implDocs/archive/ITERATIONS_17_23_DETAILS.md`

| Iteration | Focus | Impact |
|-----------|-------|--------|
| 17 | Annotation argument subinterfaces | +4 tests |
| 17b | Annotation method defaults | +16 tests |
| 17c | enumEntriesOrigin fix | No regression |
| 18 | Nested class resolution | +8 tests |
| 19 | TYPE_USE on type arguments | +5 tests |
| 20 | Wildcard parsing, inner class type args | +7 tests |
| 21 | Implicit supertypes | +15 tests |
| 22 | TYPE_USE annotation filtering via callback | +1 test |
| 23 | Cross-language constant evaluation | +4 tests |
| 24 | Three regression fixes | +13 tests |
| 25 | Inherited inner class resolution | +2 tests |
| 25c | Interface nested class static flag | +8 tests |

---

## Key Learnings

### What Worked
- **Ad-hoc debugging approach** (iterations 11-16) more effective than detailed upfront planning
- **Callback pattern** for resolution cleanly separates concerns
- **Reference implementation comparison** (javac-wrapper, PSI) often reveals correct approach

### What Didn't Work
- **Detailed upfront plans** overestimated fix counts (same symptom ≠ same cause)
- **Hardcoded lists** for filtering/resolution (use callbacks instead)
- **Modifying shared FIR files** without running PSI regression tests

### Process Improvements
- Debug 2-3 representative tests BEFORE estimating fix count
- Run PSI tests after ANY FIR file modification
- Check javac-wrapper implementation BEFORE implementing new features

---

## Future Iteration Template

```markdown
## Iteration N: [Title] - YYYY-MM-DD

### Status
- [ ] In Progress / ✅ Completed

### Root Cause Analysis
[Debug 2-3 representative tests first. Document actual cause.]

### Fix
[Solution description and files modified.]

### Test Results
- Box: X/1166, Phased: X/329

### Key Learnings
[What to add to AGENT_INSTRUCTIONS.md?]
```

---

## Iteration 24: Three Regression Fixes - 2026-03-12

### Status
✅ Completed

### Overview
Fixed three regression categories that were blocking tests after iteration 23's test suite restoration.

### Fix 1: Constant Evaluation - java.lang Types
**Problem**: `hasConstantNotNullInitializer` checked `classifierQualifiedName == "java.lang.String"`, but java-direct returned just `"String"` for unresolved types.

**Root Cause**: `classifierQualifiedName` in `JavaTypeOverAst.kt` didn't handle the implicit `java.lang.*` import that Java has by default.

**Solution**: Added `JAVA_LANG_TYPES` map in `JavaClassifierTypeOverAst` companion object to resolve common `java.lang` types (String, Object, Integer, etc.) before falling back to unresolved names.

**Files Modified**: `JavaTypeOverAst.kt`

**Tests Fixed**: ~5 (constant evaluation tests like `testAccessComplexConst`, `testDifferentTypes`, `testKt29833`)

### Fix 2: Protected Static Visibility
**Problem**: Protected static members returned `Visibilities.Protected` instead of `JavaVisibilities.ProtectedStaticVisibility`, causing FIR to reject access from subclasses in different packages.

**Root Cause**: Java has special semantics for protected static members - they're accessible from subclasses even in different packages. PSI and javac-wrapper correctly distinguish between `ProtectedStaticVisibility` and `ProtectedAndPackage`.

**Solution**: Modified `visibility` property in `JavaMemberOverAst.kt`:
```kotlin
hasModifier("PROTECTED_KEYWORD") -> if (isStatic) JavaVisibilities.ProtectedStaticVisibility else JavaVisibilities.ProtectedAndPackage
```

**Files Modified**: `JavaMemberOverAst.kt`

**Tests Fixed**: 7 (`testProtectedStatic`, `testProtectedStatic2`, `testActualizeExpectProtectedToJavaProtected`, `testJavaVisibility`, etc.)

### Fix 3: Sibling Inner Class Resolution
**Problem**: When an inner class extends a sibling inner class (e.g., `class J { class AImpl {} class A extends AImpl {} }`), the type resolver couldn't find `AImpl`.

**Root Cause**: `findLocalClass` only looked for inner classes of the current class and top-level classes, not siblings in the outer class.

**Solution**: Modified `findLocalClass` in `JavaResolutionContext.kt` to also check `containingClass.outerClass.findInnerClass(name)`.

**Files Modified**: `JavaResolutionContext.kt`

**Tests Fixed**: 3 (`testIrrelevantImplCharSequence`, `testIrrelevantImplCharSequenceWithExtraSupertype`, `testIrrelevantImplMutableListSubstitution`)

### Test Results
- Box: 1149/1167 (98.5%) — 18 failures remaining
- Started at 33 failures, fixed 15 total (some overlap with constant eval)

### Remaining Failures (18)
Categories:
- **IR dump differences** (BASELINE_DIFF_ONLY): `testNotErasedMapGetMap_inherited`
- **Enum entries**: `testEnumEntriesFromJava`, `testStaticImportFromEnumJava`
- **Reflection**: `testJavaMethodsSmokeTest`, `testJavaAnnotationConstructorTypes`, `testRawRecursiveType`, `testJavaArrayType`, `testJavaVisibility`
- **Annotations**: `testAnnotationWithKotlinProperty`, `testAnnotationWithKotlinPropertyFromInterfaceCompanion`, `testConstValAsAnnotationArgumentInJava`
- **Other**: `testGenericBoundInnerConstructorRef`, `testInheritedInnerAndNested`, `testCapturedSelfInsideIntersection4`, `testApproximationForDefinitelyNotNull`, `testKt47785`, `testJavaMapWithCustomEntries`, `testAnnotationsViaActualTypeAliasFromBinary`

### Key Learnings
1. **Check reference implementations first** — PSI's `JavaElementUtil.getVisibility()` showed the correct visibility handling pattern
2. **java.lang implicit import** — Java always has `java.lang.*` implicitly imported; handling well-known types locally avoids resolution round-trips
3. **Inner class scoping** — Java allows referencing sibling inner classes by simple name; resolution must walk up the containment hierarchy

---

## Iteration 24b: Cyclic Type Bounds StackOverflowError Fix - 2026-03-12

### Status
✅ Completed

### Overview
Fixed StackOverflowError occurring with cyclic type parameter bounds like `class JavaA<T extends JavaB>` and `class JavaB<T extends JavaA>`.

### Root Cause Analysis
The test `testSignatureEnhancementCycleTypeBound` was failing with StackOverflowError. The cycle occurred because:

1. During type parameter bounds resolution (`TYPE_PARAMETER_BOUND_FIRST_ROUND` mode), FIR converts bound types
2. For java-direct classes with `classifier == null`, the code accessed `typeParameterSymbols` to detect raw types
3. This triggered loading of the referenced class's type parameters
4. Which triggered bounds resolution on that class, which referenced the original class
5. Infinite recursion → StackOverflowError

The issue was in `JavaTypeConversion.kt` where raw type detection was not guarded during first-round type parameter bounds resolution. The existing `JavaClass` branch had this safety check, but the `classifier == null` branch (used by java-direct) did not.

### Fix
Modified `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt` to skip raw type detection during `TYPE_PARAMETER_BOUND_FIRST_ROUND` in two places:

1. **In `toConeTypeProjection`** (lines 150-159): Added early return for `TYPE_PARAMETER_BOUND_FIRST_ROUND` mode before accessing `typeParameterSymbols`
2. **In `toConeKotlinTypeForFlexibleBound`** (line 318): Added `takeIf { mode != FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_FIRST_ROUND }` guard before accessing `typeParameterSymbols`

This matches the existing safety pattern in the `JavaClass` branch (line 264).

### Test Results
- Box: 1150/1167 (98.5%) — 17 failures (was 18)
- Phased: 1323/1442 (91.7%) — 119 failures (was ~124)
- **Tests fixed**: ~6 (including `testSignatureEnhancementCycleTypeBound`)
- PSI regression tests: Pass

### Key Learnings
1. **Always check both branches** — When there's a safety guard in one code branch (e.g., `JavaClass`), verify the parallel branch (`classifier == null`) has equivalent protection
2. **Cyclic type bounds are tricky** — Java allows mutually-referencing type parameters across classes; FIR handles this with multi-round resolution, but we must not trigger class loading during first round

---

## Iteration 25: Inherited Inner Class Resolution - 2026-03-12

### Status
✅ Completed

### Overview
Fixed resolution of inherited inner classes per JLS 6.5.2. When class B extends class A, and A has inner class `Inner`, references to `Inner` from B (or B's inner classes) now resolve correctly.

### Root Cause Analysis
Test `testInheritedInner` failed with `MISSING_DEPENDENCY_CLASS: Cannot access class 'y'`:
```java
// a/x.java
public class x { public class y {} }

// a/b.java  
public class b extends x { public y getY() { return null; } }
```

The type `y` in `b.getY()` return type couldn't be resolved because:
1. `findLocalClass` only searched within the same compilation unit
2. `resolveFromSupertypesRecursive` returned unqualified names for same-package supertypes

### Fix
Modified `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaResolutionContext.kt`:

1. **Added `findInnerClassFromSupertypes`** to `findLocalClass` — Searches inner classes of local supertypes (same compilation unit)

2. **Added outer class supertype search** — For nested inner classes, also search supertypes of outer classes. This handles cases like:
   ```java
   class Y extends X { class D { z ref; } }  // z is inner class of X
   ```

3. **Fixed same-package supertype resolution** — In `resolveFromSupertypesRecursive`, if supertype name is unqualified (no dots), try with package prefix first:
   ```kotlin
   if (!supertypeName.contains('.') && !packageFqName.isRoot) {
       val packageQualified = "${packageFqName.asString()}.$supertypeName"
       // Try package-qualified version...
   }
   ```

4. **Walk outer class hierarchy in `resolveSimpleName`** — For nested inner classes, check supertypes of all enclosing classes, not just the immediate containing class.

### Test Results
- Box: 1150/1167 (17 failures, unchanged)
- Phased: 1326/1442 (116 failures, was 118)
- **Tests fixed**: 2 phased tests (`testInheritedInner`, `testInheritedInnerUsageInInner`)
- 2 remaining failures are baseline diffs only (no actual errors)

### Key Learnings
1. **Avoid cycles in lazy resolution** — Accessing `classifier` or `classifierQualifiedName` during supertype search can cause infinite recursion. Use `localClassProvider` directly or `presentableText` for raw names.
2. **Same-package types need qualification** — When resolving via callback, unqualified type names like `"x"` won't match; need to try `"pkg.x"` for same-package classes.
3. **JLS 6.5.2 scope rules** — Inner classes of supertypes AND outer classes' supertypes are all in scope for nested inner classes.

---

## Iteration 25c: Interface Nested Class Static Flag - 2026-03-12

### Status
✅ Completed

### Overview
Fixed incorrect `isStatic` reporting for classes nested inside Java interfaces. Per JLS 9.5, all classes declared as members of an interface are implicitly static, but java-direct was not accounting for this.

### Root Cause Analysis
Test `testInheritedInnerAndNested` failed because java-direct incorrectly reported `isStatic = false` for classes nested in interfaces:

```java
public interface BaseInterface {
    class Inner {  // This is implicitly static per JLS 9.5
        public String box() { return "BaseInterface"; }
    }
}
```

The `isStatic` property in `JavaClassOverAst` only checked:
```kotlin
hasModifier("STATIC_KEYWORD") || (outerClass != null && (isInterface || isEnum))
```

This checks if the **inner class itself** is an interface or enum, but doesn't check if the **outer class** is an interface. In Java, any class nested inside an interface is implicitly static, regardless of whether it has the `static` keyword.

### Fix
Modified `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt` line 62:

```kotlin
// Before:
override val isStatic: Boolean get() = hasModifier("STATIC_KEYWORD") || (outerClass != null && (isInterface || isEnum))

// After:
override val isStatic: Boolean get() = hasModifier("STATIC_KEYWORD") || (outerClass != null && (isInterface || isEnum)) || (outerClass?.isInterface == true)
```

This matches the javac-wrapper implementation in `TreeBasedClass.kt:55`:
```kotlin
override val isStatic: Boolean
    get() = isEnum || isInterface || (outerClass?.isInterface ?: false) || tree.modifiers.isStatic
```

### Test Results
- Total: 2649 tests
- Before: 142 failures
- After: 134 failures
- **Tests fixed**: 8 (including `testInheritedInnerAndNested` phased and box tests)

### Key Learnings
1. **JLS 9.5 interface member rules** — All member classes of interfaces are implicitly `public static`, even without explicit modifiers
2. **Check reference implementations** — The javac-wrapper (`TreeBasedClass.kt`) correctly implements this rule and serves as a good reference
3. **Implicit modifiers matter** — Java has several cases of implicit modifiers (interface members, enum constructors, etc.) that must be correctly reported to FIR

---

*For detailed iteration histories, see `implDocs/archive/`*
