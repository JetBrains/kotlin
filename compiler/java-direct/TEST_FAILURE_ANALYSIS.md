# Test Failure Analysis - Post Iteration 25c

**Date**: 2026-03-12  
**Context**: Post-mortem analysis after iteration 25/25c underwhelming results  
**Current Results**: 
- Box: 1167 total, 17 failed (98.5% pass)
- Phased: 1442 total, 110 failed (92.4% pass)
- **Combined: 2609 total, 127 failed (95.1% pass)**

---

## Critical Lesson: Estimation Accuracy

### Iteration 25 Estimation vs Reality

| Category | Estimated | Actual Fixed | Notes |
|----------|-----------|--------------|-------|
| Inherited Inner Classes | 30-40 | 2 | Only 2 tests were actually this root cause |
| Interface Static Flag | (grouped above) | 8 | Different root cause, discovered during iteration |
| **Total** | **30-40** | **10** | **75% overestimate** |

### Why the Discrepancy?

**Root Cause of Bad Estimation**: Tests were categorized by **symptom** (test name containing "Inherited", "Inner", "Nested") rather than by **actual root cause**. Many tests that appear to be about "inherited inner classes" have completely different underlying issues:

1. **Ambiguity Detection Tests** (`InheritanceAmbiguity`, `InheritanceAmbiguity2`, `InheritanceAmbiguity3`)
   - Expected: Report `MISSING_DEPENDENCY_CLASS` when multiple inner classes named `Z` exist from different supertypes
   - java-direct: **Resolves one** (wrong behavior) instead of detecting ambiguity
   - This is NOT about finding inherited inner classes - it's about detecting **ambiguous** inner classes

2. **Type Parameter Scoping Tests** (`InnerWithTypeParameter`, `SeveralInnersWithTypeParameters`, `Clash`)
   - About type parameters being properly scoped in nested generic classes
   - Not about inherited inner class lookup at all

3. **Import/Package Edge Cases** (`TopLevelClassVsPackage`, `NestedClassClash`)
   - About complex import resolution with package/class name conflicts
   - Not about inheritance

### Improved Estimation Methodology

**BEFORE estimating fix counts:**
1. **Debug 2-3 representative tests** from each category to confirm the actual root cause
2. **Check if tests share the EXACT same code path** where the fix would be applied
3. **Categorize by code path**, not by test name or superficial error message
4. **Apply 50% discount** to initial estimates for unfamiliar code areas

---

## Corrected Remaining Failures Analysis (127 tests)

### Category 1: Inherited Inner Class Resolution (TRUE) — 2-5 tests remaining

**Confirmed Tests**: Only tests where the actual issue is finding an inner class from a supertype.
- Most were already fixed in iteration 25

**Status**: Mostly resolved

---

### Category 2: Ambiguity Detection — ~5-8 tests — MEDIUM PRIORITY

**Error Pattern**: java-direct resolves a type when it should report ambiguity/MISSING_DEPENDENCY_CLASS

**Root Cause**: When a class inherits from multiple sources (class + interface) that both define inner class `Z`, Java should report ambiguity. java-direct picks one instead.

**Affected Tests**:
- `testInheritanceAmbiguity`, `testInheritanceAmbiguity2`, `testInheritanceAmbiguity3`
- `testInheritedInner2` (different: type param shadows inherited inner)
- `testClash` (type parameter `O` clashes with inherited inner `O`)
- `testSupertypeInnerAndTypeParameterWithSameNames`

**Fix Required**: Modify inner class resolution to:
1. Collect ALL matching inner classes from supertypes
2. If multiple found, return null (let FIR report MISSING_DEPENDENCY)
3. Only return if exactly one match

---

### Category 3: Type Parameter Scoping in Nested Classes — ~6-10 tests — MEDIUM PRIORITY

**Error Pattern**: Type parameters from outer classes not visible in inner classes

**Affected Tests**:
- `testInnerWithTypeParameter`
- `testSeveralInnersWithTypeParameters`
- `testJ_k`, `testJ_k_complex`

**Status**: Needs investigation - may be FIR integration issue, not java-direct

---

### Category 4: Sealed Classes — ~12-15 tests — HIGH PRIORITY

(unchanged from previous analysis)

---

### Category 5: Java Records — ~6-8 tests — HIGH PRIORITY

(unchanged from previous analysis)

---

### Category 6: Import/Package Edge Cases — ~8-10 tests — MEDIUM PRIORITY

(unchanged from previous analysis)

---

### Category 7: Raw Types — ~10-15 tests — LOW PRIORITY

(unchanged from previous analysis)

---

### Category 8: Baseline Differences Only — ~50-60 tests — LOW PRIORITY

(unchanged from previous analysis)

---

## Updated Priority Order

### Iteration 26: Sealed Classes  
- **Impact**: 12-15 tests (HIGH CONFIDENCE - distinct feature)
- **Complexity**: LOW-MEDIUM
- **Files**: `JavaClassOverAst.kt`

### Iteration 27: Java Records
- **Impact**: 6-8 tests (HIGH CONFIDENCE - distinct feature)
- **Complexity**: MEDIUM
- **Files**: `JavaClassOverAst.kt`, new `JavaRecordComponentOverAst.kt`

### Iteration 28: Ambiguity Detection
- **Impact**: 5-8 tests (MEDIUM CONFIDENCE)
- **Complexity**: MEDIUM
- **Files**: `JavaResolutionContext.kt`

### Later: Type parameter scoping, Import edge cases, baseline updates

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
