# Test Failure Analysis - Post Iteration 24

**Date**: 2026-03-12  
**Context**: Comprehensive analysis after iteration 24 fixes  
**Current Results**: 
- Box: 1167 total, 18 failed (98.5% pass)
- Phased: 1442 total, 124 failed (91.4% pass)
- **Combined: 2609 total, 142 failed (94.6% pass)**

---

## Iteration 24 Fixed (15 tests)

The following regressions were fixed in iteration 24:

| Category | Tests Fixed | Fix Applied |
|----------|-------------|-------------|
| Constant Evaluation | ~5 | Added `JAVA_LANG_TYPES` map in `JavaTypeOverAst.kt` |
| Protected Static Visibility | 7 | Changed to `ProtectedStaticVisibility` in `JavaMemberOverAst.kt` |
| Sibling Inner Classes | 3 | Added sibling lookup in `JavaResolutionContext.findLocalClass()` |

---

## Remaining Failures Analysis (142 tests)

### Category 1: Inherited Inner Class Resolution (~30-40 tests) — HIGH PRIORITY

**Error Pattern**: `MISSING_DEPENDENCY_CLASS: Cannot access class 'y'`

**Root Cause**: When class B extends class A, and A has inner class `Inner`, references to `Inner` from B's context fail. The current `findLocalClass` only checks:
1. Inner classes of the containing class
2. Sibling inner classes (fixed in iter 24)
3. Top-level classes

It does NOT search supertypes' inner classes per JLS 6.5.2.

**Affected Tests** (representative):
- `testInheritedInner`, `testInheritedInner2`
- `testInheritedInnerAndNested`
- `testInheritedInnerUsageInInner`
- `testNestedFromJava`, `testNestedFromJavaAfterKotlin`
- `testSuperTypeWithSameInner`
- `testInheritedKotlinInner`
- Multiple `testNested*` and `testInheritance*` tests

**Fix Required**: Extend `findLocalClass` in `JavaResolutionContext.kt` to walk up the supertype hierarchy and search each supertype's inner classes. This requires:
1. Getting the containing class's supertypes
2. For each supertype, check `findInnerClass(name)`
3. Recursively check supertypes of supertypes

**Note**: The callback-based `resolveFromSupertypes` already exists but only runs during FIR resolution. The issue is that `findLocalClass` is called during Java model construction, before FIR callbacks are available.

---

### Category 2: Sealed Classes (~12-15 tests) — HIGH PRIORITY

**Error Pattern**: Various - wrong diagnostics, missing permitted types

**Root Cause**: `isSealed` returns `false` always, `permittedTypes` returns empty.

**Affected Tests**:
- `testSealedJavaClass`, `testSealedJavaClassEnabled`
- `testJavaSealedClassExhaustiveness`, `testJavaSealedInterfaceExhaustiveness`
- `testActualJavaSealedClass`
- `testDirectJavaActualization_sealedClass`
- `testKotlinInheritsJavaClass`, `testKotlinInheritsJavaInterface`
- `testFlexibleSealedInSubject`

**Fix Required**: In `JavaClassOverAst.kt`:
1. Implement `isSealed` - check for `SEALED_KEYWORD` in modifier list
2. Implement `permittedTypes` - parse `PERMITS_LIST` node for permitted class references

---

### Category 3: Java Records (~6-8 tests) — HIGH PRIORITY

**Error Pattern**: `UNRESOLVED_REFERENCE` for record component accessors

**Root Cause**: `isRecord` returns `false`, `recordComponents` returns empty.

**Affected Tests**:
- `testSimpleRecords`, `testSimpleRecordsDefaultConstructor`
- `testSimpleRecordsWithSecondaryConstructor`
- `testJavaRecordWithCanonicalConstructor`
- `testJavaRecordWithExplicitComponent`
- `testJavaRecordWithGeneric`

**Fix Required**: In `JavaClassOverAst.kt`:
1. Implement `isRecord` - check for `RECORD_KEYWORD`
2. Implement `recordComponents` - parse `RECORD_HEADER` or `RECORD_COMPONENT` nodes
3. Create `JavaRecordComponentOverAst` class implementing `JavaRecordComponent`

---

### Category 4: Import/Package Edge Cases (~8-10 tests) — MEDIUM PRIORITY

**Error Pattern**: `UNRESOLVED_REFERENCE: Unresolved reference '_ab'`

**Root Cause**: Complex scenarios where package names clash with class names.

**Affected Tests**:
- `testTopLevelClassVsPackage`, `testTopLevelClassVsPackage2`
- `testPackageVsClass`, `testPackageVsRootClass`
- `testNestedAndTopLevelClassClash`, `testNestedClassClash`
- `testCurrentPackageAndExplicitNestedImport`

**Fix Required**: Review import resolution in `JavaResolutionContext.kt` for edge cases where:
- A class name matches a package name
- Nested imports clash with top-level classes

---

### Category 5: Raw Types (~10-15 tests) — LOW PRIORITY

**Error Pattern**: Mostly baseline differences, some `MISSING_DEPENDENCY`

**Affected Tests**:
- `testRecursiveBound`, `testIntermediateRecursion`
- `testRawSupertypeOverride`, `testRawTypeSyntheticExtensions`
- `testNonTrivialErasure`, `testInterdependentTypeParameters`
- `testArrays`, `testRawTypes`

**Status**: Many are baseline differences only. Some may be related to inherited inner class issue.

---

### Category 6: Enum Handling (~3-5 tests) — LOW PRIORITY

**Affected Tests**:
- `testEnumEntriesFromJava`
- `testStaticImportFromEnumJava`
- `testJavaEnum`
- `testDirectJavaActualization_enumStatics`

**Status**: May require investigation of enum entries generation for java-direct.

---

### Category 7: Baseline Differences Only (~60-70 tests) — LOW PRIORITY

**Error Pattern**: `Actual data differs from file content` with no compilation error

These tests compile successfully but produce slightly different IR/diagnostic output.

**Status**: May be acceptable differences or require baseline updates. Lower priority.

---

## Box Test Failures Detail (18 tests)

| Test | Category | Error |
|------|----------|-------|
| testAnnotationWithKotlinProperty | Annotation | Baseline diff |
| testAnnotationWithKotlinPropertyFromInterfaceCompanion | Annotation | Baseline diff |
| testNotErasedMapGetMap_inherited | Collections | Baseline diff |
| testEnumEntriesFromJava | Enum | Runtime/entries |
| testStaticImportFromEnumJava | Enum | Baseline diff |
| testCapturedSelfInsideIntersection4 | Inference | Complex generics |
| testInheritedInnerAndNested | Inner | MISSING_DEPENDENCY |
| testGenericBoundInnerConstructorRef | Inner/Generics | UNRESOLVED |
| testConstValAsAnnotationArgumentInJava | Const | Baseline diff |
| testAnnotationsViaActualTypeAliasFromBinary | Multiplatform | Baseline diff |
| testKt47785 | Platform types | Baseline diff |
| testJavaAnnotationConstructorTypes | Reflection | Baseline diff |
| testJavaMethodsSmokeTest | Reflection | Baseline diff |
| testJavaVisibility | Reflection | Baseline diff |
| testJavaArrayType | Reflection | Baseline diff |
| testRawRecursiveType | Reflection | Baseline diff |
| testApproximationForDefinitelyNotNull | Regression | Baseline diff |
| testJavaMapWithCustomEntries | Special builtins | Baseline diff |

---

## Priority Order for Next Iterations

### Iteration 25 (Recommended): Inherited Inner Classes
- **Impact**: ~30-40 tests
- **Complexity**: MEDIUM
- **Files**: `JavaResolutionContext.kt`
- **Approach**: Add supertype inner class lookup to `findLocalClass`

### Iteration 26: Sealed Classes  
- **Impact**: ~12-15 tests
- **Complexity**: LOW-MEDIUM
- **Files**: `JavaClassOverAst.kt`
- **Approach**: Implement `isSealed` and `permittedTypes`

### Iteration 27: Java Records
- **Impact**: ~6-8 tests
- **Complexity**: MEDIUM
- **Files**: `JavaClassOverAst.kt`, new `JavaRecordComponentOverAst.kt`
- **Approach**: Implement `isRecord` and `recordComponents`

### Later: Import edge cases, baseline updates

---

## Key Learnings from Iteration 24

1. **Check PSI/javac-wrapper first** — The `ProtectedStaticVisibility` pattern was directly from PSI's `JavaElementUtil.getVisibility()`
2. **java.lang implicit import** — Java always has `java.lang.*` implicitly imported; handle common types locally
3. **Sibling vs Inherited** — Inner class resolution has two distinct cases that need separate handling
