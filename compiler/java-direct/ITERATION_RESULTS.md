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
| Advanced Features | 17-24 | 1149/1167 (98.5%) | ~300/329 |

**Current Status**: ~18 box test failures remaining

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

*For detailed iteration histories, see `implDocs/archive/`*
