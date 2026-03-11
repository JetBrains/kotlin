# Java-Direct Failure Analysis: Post-Iteration 22

**Date**: 2026-03-11 (Updated)
**Tests Run**: Box (1167), Phased (327)
**Current Status**:
- Box: 1131/1167 passing (96.9%) - 36 failing
- Phased: 299/327 passing (91.4%) - 28 failing

**Iteration 22 Progress**:
- Phased: 31 → 28 failing (3 tests fixed)
- Box: 37 → 36 failing (1 test fixed)

**Note**: Box and Phased tests share most failing test cases. Box tests have additional unique failures.

---

## Executive Summary

The remaining failures fall into these major categories:

| Category | Count | Fixable? | Effort | Status |
|----------|-------|----------|--------|--------|
| TYPE_USE Annotation Filtering | 4 | YES | MEDIUM | ✅ FIXED |
| Static Field Nullability Enhancement | ~3 | YES | MEDIUM | Needs investigation |
| Nested Class Resolution in Supertypes | 5 | YES | MEDIUM | Planned (Iteration 23) |
| StackOverflow in Cyclic Type Bounds | 1 | YES | HIGH | Planned (Iteration 24) |
| Raw Types with Inner Classes | 2 | YES | MEDIUM | Needs investigation |
| External Dependencies/Test Setup | 2 | INVESTIGATE | VARIES | |
| Other Edge Cases | ~12 | VARIES | VARIES | |

---

## Category 1: TYPE_USE Annotation Filtering ✅ FIXED

**Count**: 4 tests fixed (3 phased + 1 box)
**Status**: ✅ FIXED in Iteration 22

### What Was Fixed

Implemented a **callback-based** TYPE_USE annotation filtering system that allows java-direct to use FIR's
annotation class resolution without affecting other Java class finders.

### Architecture

1. Added `filterTypeUseAnnotations(isTypeUseAnnotation: (String) -> Boolean)` method to `JavaType` interface
2. Default implementation returns `annotations` unchanged (javac-wrapper already filters at Java structure level)
3. Java-direct overrides this method to use the callback for filtering
4. FIR provides the callback that resolves annotation classes and checks their `@Target`

### Implementation

**In `JavaType` interface (javaTypes.kt):**
```kotlin
fun filterTypeUseAnnotations(isTypeUseAnnotation: (String) -> Boolean): Collection<JavaAnnotation> {
    // Default: return annotations as-is (javac-wrapper already filters)
    return annotations
}
```

**In `JavaTypeOverAst` (java-direct):**
```kotlin
override fun filterTypeUseAnnotations(isTypeUseAnnotation: (String) -> Boolean): Collection<JavaAnnotation> {
    // Filter extra annotations (from method modifier list) using the callback
    val filteredExtraAnnotations = extraAnnotations.filter { annotation ->
        val fqName = annotation.classId?.asSingleFqName()?.asString() ?: return@filter false
        isTypeUseAnnotation(fqName)
    }
    // Direct annotations on the type are TYPE_USE by definition
    return filteredExtraAnnotations + modifierListAnnotations + directAnnotations
}
```

**In `JavaTypeConversion.kt` (FIR):**
```kotlin
private fun isTypeUseAnnotationClass(fqName: String, session: FirSession): Boolean {
    val classId = findClassId(fqName, session) ?: ClassId.topLevel(FqName(fqName))
    val annotationClass = session.symbolProvider.getClassLikeSymbolByClassId(classId)?.fir
        as? FirRegularClass ?: return false

    val targetAnnotation = annotationClass.annotations.find { firAnnotation ->
        val targetClassId = firAnnotation.annotationTypeRef.coneType.classId
        targetClassId == JvmStandardClassIds.Annotations.Java.Target ||
                targetClassId == StandardClassIds.Annotations.Target
    } ?: return false

    return hasTypeUseTarget(targetAnnotation)
}
```

### Why Callback-Based Approach?

**Failed approach #1 (blocklist)**: Hardcoding annotation names in java-direct was rejected as not maintainable.

**Failed approach #2 (FIR-level filtering)**: Adding filtering directly in shared FIR code (`JavaTypeConversion.kt`)
caused regressions in PSI-based tests because:
- Javac-wrapper already filters at Java structure level
- Adding FIR-level filtering double-filtered and broke expected behavior

**Successful approach**: Callback pattern allows each implementation to control its own filtering:
- Javac-wrapper: returns annotations as-is (already filtered at Java structure level)
- Java-direct: uses callback to resolve annotation classes via FIR

### Tests Fixed

- `testSyntheticSmartCast` - `@Nullable` annotation was incorrectly appearing on return type
- `testFlexibleTypeAliases`
- `testGenericDescriptor`
- 1 additional box test

### Results

- Phased: 31 → 28 failing (3 tests fixed)
- Box: 37 → 36 failing (1 test fixed)
- No regressions in PSI-based tests ✓

---

## Category 2: Static Field Nullability Enhancement

**Count**: ~3 tests
**Root Cause**: Static final fields should be enhanced to non-nullable in certain cases

### Symptoms

```diff
- lval project: R|kotlin/String| = Q|PlatformDataKeys|.R|/CommonDataKeys.PROJECT*s|
+ lval project: R|kotlin/String!| = Q|PlatformDataKeys|.R|/CommonDataKeys.PROJECT*s|
```

### Affected Tests

- `testStaticFromBaseClass` - `static final String PROJECT` should enhance to `String`, not `String!`

### Effort: MEDIUM

---

## Category 3: Nested Class Resolution in Supertypes

**Count**: 5 tests
**Status**: Planned for Iteration 23

### Symptoms

```
MISSING_DEPENDENCY_SUPERCLASS: Cannot access 'Base' which is a supertype of 'Test.Derived'
```

### Effort: MEDIUM

---

## Category 4: StackOverflow in Cyclic Type Bounds

**Count**: 1 test
**Status**: Planned for Iteration 24

### Effort: HIGH

---

## Category 5: Raw Types with Inner Classes

**Count**: 2 tests
**Root Cause**: Type argument handling for inner classes of raw types

### Effort: MEDIUM

---

## Category 6: External Dependencies / Test Setup Issues

**Count**: 2 tests

### Effort: INVESTIGATE

---

## Recommended Fix Order

### ✅ Phase 1: Callback-Based TYPE_USE Annotation Filtering - COMPLETED
**Result**: 31 → 28 failing phased tests, 37 → 36 failing box tests

### Phase 2: Static Field Nullability Enhancement (MEDIUM effort, ~3 tests)

### Phase 3: Sibling Nested Class Resolution (MEDIUM effort, ~5 tests)

### Phase 4: Raw Type Inner Class Handling (MEDIUM effort, ~2 tests)

### Phase 5: Cyclic Type Bound Detection (HIGH effort, 1 test)

### Phase 6: Remaining Edge Cases (VARIES)

---

## Updated Projections

| Phase | Tests Fixed | Cumulative Pass Rate |
|-------|-------------|---------------------|
| After Phase 1 | 4 | 91.4% (299/327 phased), 96.9% (1131/1167 box) |
| Phase 2 (Static fields) | ~3 | 92.4% (~302/327) |
| Phase 3 (Nested classes) | ~5 | 93.9% (~307/327) |
| Phase 4 (Raw types) | ~2 | 94.5% (~309/327) |
| Phase 5 (Cyclic bounds) | 1 | 94.8% (~310/327) |

**Realistic Target**: 95%+ with Phases 2-4

---

*Generated: 2026-03-10*
*Updated: 2026-03-11 (Iteration 22 - Callback-based filtering)*
