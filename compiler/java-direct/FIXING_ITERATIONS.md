# Java-Direct: Fixing Iterations

## Document Purpose

This document tracks iteration history and plans for the `java-direct` module.

**Prerequisites**: Read `AGENT_INSTRUCTIONS.md` before starting any iteration  
**Status**: Iteration 24 complete — 1149/1167 box tests (98.5%), ~1318/1442 phased tests (~91.4%)  
**Combined**: 2609 total, 142 failed (94.6% pass)  
**Last Updated**: 2026-03-12

---

## Current State After Iteration 24

### What Was Fixed in Iteration 24
1. **Constant Evaluation** (~5 tests) — Added `JAVA_LANG_TYPES` map for implicit java.lang import
2. **Protected Static Visibility** (7 tests) — Use `ProtectedStaticVisibility` for protected static members
3. **Sibling Inner Classes** (3 tests) — Added sibling lookup in `findLocalClass`

### Remaining Failures (142 tests)
See `TEST_FAILURE_ANALYSIS.md` for detailed breakdown. Summary:

| Category | Est. Tests | Priority | Complexity |
|----------|------------|----------|------------|
| **Inherited Inner Classes** | ~30-40 | HIGH | MEDIUM |
| **Sealed Classes** | ~12-15 | HIGH | LOW-MEDIUM |
| **Java Records** | ~6-8 | HIGH | MEDIUM |
| **Import Edge Cases** | ~8-10 | MEDIUM | MEDIUM |
| **Raw Types** | ~10-15 | LOW | VARIES |
| **Enum Handling** | ~3-5 | LOW | LOW |
| **Baseline Diffs Only** | ~60-70 | LOW | N/A |

---

## Recommended Next Iterations

### Iteration 25: Inherited Inner Class Resolution (RECOMMENDED NEXT)

**Impact**: ~30-40 tests (largest category of actual failures)

**Problem**: When class B extends class A, and A has inner class `Inner`, references to `Inner` from B fail because `findLocalClass` doesn't search supertypes.

**Test to debug first**: `testInheritedInner` in `compiler/testData/diagnostics/tests/javac/inheritance/InheritedInner.kt`

**Fix approach**:
```kotlin
// In JavaResolutionContext.kt, modify findLocalClass:
fun findLocalClass(name: Name): JavaClass? {
    val containingClass = containingClassProvider?.invoke()
    // 1. Inner classes of containing class
    containingClass?.findInnerClass(name)?.let { return it }
    // 2. Sibling inner classes (already implemented in iter 24)
    containingClass?.outerClass?.findInnerClass(name)?.let { return it }
    // 3. NEW: Inner classes of supertypes (JLS 6.5.2)
    containingClass?.let { cls ->
        findInnerClassFromSupertypes(name, cls)?.let { return it }
    }
    // 4. Top-level classes
    return localClassProvider(name)
}

private fun findInnerClassFromSupertypes(name: Name, javaClass: JavaClass): JavaClass? {
    // Walk supertype hierarchy, check each for inner class with given name
    // Be careful about cycles and external classes
}
```

**Challenge**: Supertypes may be external (resolved via FIR), so we need to handle the case where `classifier` is null and only `classifierQualifiedName` is available. May need to use the resolution callback.

---

### Iteration 26: Sealed Classes

**Impact**: ~12-15 tests

**Problem**: `isSealed` returns `false`, `permittedTypes` returns empty.

**Test to debug first**: `testSealedJavaClass` in `compiler/testData/diagnostics/tests/sealed/sealedJavaClass.kt`

**Fix approach** in `JavaClassOverAst.kt`:
```kotlin
override val isSealed: Boolean 
    get() = hasModifier("SEALED_KEYWORD")

override val permittedTypes: Sequence<JavaClassifierType>
    get() {
        val permitsList = node.findChildByType("PERMITS_LIST") ?: return emptySequence()
        return permitsList.getChildrenByType("JAVA_CODE_REFERENCE")
            .map { JavaClassifierTypeOverAst(it, memberResolutionContext) }
            .asSequence()
    }
```

---

### Iteration 27: Java Records

**Impact**: ~6-8 tests

**Problem**: `isRecord` returns `false`, `recordComponents` returns empty.

**Test to debug first**: `testSimpleRecords` in `compiler/testData/diagnostics/tests/testsWithJava17/jvmRecord/simpleRecords.kt`

**Fix approach**:
1. In `JavaClassOverAst.kt`:
   ```kotlin
   override val isRecord: Boolean 
       get() = node.findChildByType("RECORD_KEYWORD") != null
   
   override val recordComponents: Collection<JavaRecordComponent>
       get() {
           val header = node.findChildByType("RECORD_HEADER") ?: return emptyList()
           return header.getChildrenByType("RECORD_COMPONENT")
               .map { JavaRecordComponentOverAst(it, memberResolutionContext) }
       }
   ```

2. Create new `JavaRecordComponentOverAst.kt` implementing `JavaRecordComponent` interface.

---

## Archived Iterations

| Archive | Iterations | Result |
|---------|------------|--------|
| `implDocs/archive/ITERATIONS_1_6_DETAILS.md` | 1-6 | 0 → 90/138 (65.2%) |
| `implDocs/archive/ITERATIONS_7_16_DETAILS.md` | 7-16 | 90 → 532/601 (88.5%) |
| `implDocs/archive/ITERATIONS_17_23_DETAILS.md` | 17-23 | 1075 → 1134/1166 (97.2%) |

**Key patterns established** (see archives and `ITERATION_RESULTS.md`):
- Callback pattern for resolution (types, annotations, enums, constants)
- Two-phase type parameter construction
- PSI/java-direct discrimination in shared FIR code
- Implicit supertypes for Java special class kinds
- java.lang implicit import handling
- Protected static vs protected and package visibility distinction

---

## Recommended Approach

Use the **ad-hoc debugging approach** from iterations 11-16:

1. **Pick category** — Start with highest impact (inherited inner classes)
2. **Debug 2-3 representative tests** — Verify root cause
3. **Implement fix** — Target verified root cause
4. **Run PSI regression tests** if FIR files modified
5. **Document in `ITERATION_RESULTS.md`**

**Key insight from iteration 24**: Always check PSI (`JavaElementUtil`) and javac-wrapper implementations first. They often show the correct pattern.

---

## Future Iteration Template

```markdown
## Iteration N: [Title] - YYYY-MM-DD

### Status
- [ ] In Progress / ✅ Completed

### Root Cause Analysis
[Debug 2-3 representative tests first. Document actual cause.]

### Fix
[Solution description. Files modified.]

### Test Results
- Box: X/1167, Phased: X/1442

### Key Learnings
[What to add to AGENT_INSTRUCTIONS.md?]
```

---

## Document Change Log

- 2026-03-12: Iteration 24 complete, updated remaining work analysis
- 2026-03-12: Consolidated iterations 17-23 to archive
- 2026-03-10: Iterations 20-22 completed
- 2026-03-06: Iterations 17-17b completed
- 2026-03-05: Iterations 11-16 completed
- 2026-03-04: Iterations 7-10 completed
- 2026-02-23: Initial structure with iterations 1-6
