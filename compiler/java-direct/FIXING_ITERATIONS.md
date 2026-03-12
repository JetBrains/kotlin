# Java-Direct: Fixing Iterations

## Document Purpose

This document tracks iteration history and plans for the `java-direct` module.

**Prerequisites**: Read `AGENT_INSTRUCTIONS.md` before starting any iteration  
**Status**: Iteration 25c complete — 1150/1167 box tests (98.5%), ~1326/1442 phased tests (~92.0%)  
**Combined**: 2609 total, 127 failed (95.1% pass)  
**Last Updated**: 2026-03-12

---

## CRITICAL: Estimation Accuracy Lesson

### Iteration 25 Post-Mortem

| Category | Estimated | Actual Fixed | Accuracy |
|----------|-----------|--------------|----------|
| Inherited Inner Classes | 30-40 | 2 | **5-7%** |
| Interface Static Flag | (grouped above) | 8 | (discovered during work) |
| **Total** | **30-40** | **10** | **25-33%** |

### Root Cause of Bad Estimation

Tests were categorized by **symptom/test name** rather than **actual root cause**:

1. **"InheritanceAmbiguity" tests** - NOT about finding inherited inner classes, but about **detecting ambiguity** when multiple inner classes with same name exist from different supertypes
2. **"InnerWithTypeParameter" tests** - About **type parameter scoping**, not inheritance
3. **"NestedClassClash" tests** - About **import resolution**, not inheritance

### MANDATORY Estimation Process

**BEFORE estimating fix counts for ANY category:**

1. **Debug 2-3 representative tests** — Run with debugger, trace actual code path
2. **Verify tests share SAME code path** — Different symptoms ≠ same root cause
3. **Categorize by code path**, not test name
4. **Apply 50% discount** for unfamiliar code areas
5. **Maximum confidence estimate**: 15 tests per category unless proven otherwise

---

## Current State After Iteration 25c

### What Was Fixed in Iterations 24-25c
| Iteration | Category | Tests Fixed |
|-----------|----------|-------------|
| 24 | Constant Evaluation | ~5 |
| 24 | Protected Static Visibility | 7 |
| 24 | Sibling Inner Classes | 3 |
| 25 | Inherited Inner from Supertypes | 2 |
| 25c | Interface Nested Class Static | 8 |

### Remaining Failures (127 tests) — CORRECTED ESTIMATES
See `TEST_FAILURE_ANALYSIS.md` for details.

| Category | Est. Tests | Confidence | Priority |
|----------|------------|------------|----------|
| **Sealed Classes** | 12-15 | HIGH | HIGH |
| **Java Records** | 6-8 | HIGH | HIGH |
| **Ambiguity Detection** | 5-8 | MEDIUM | MEDIUM |
| **Type Param Scoping** | 6-10 | LOW | MEDIUM |
| **Import Edge Cases** | 8-10 | LOW | MEDIUM |
| **Baseline Diffs** | 50-60 | HIGH | LOW |

**Note**: Sealed Classes and Records have HIGH confidence because they're **distinct features** with clear implementation gaps (isSealed=false, isRecord=false), not subtle resolution bugs.

---

## Recommended Next Iterations

### Iteration 26: Sealed Classes (RECOMMENDED NEXT)

**Impact**: 12-15 tests (HIGH CONFIDENCE — distinct unimplemented feature)

**Why High Confidence**: `isSealed` literally returns `false` always. Every sealed class test will fail until this is implemented. No ambiguity about root cause.

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

**Impact**: 6-8 tests (HIGH CONFIDENCE — distinct unimplemented feature)

**Why High Confidence**: `isRecord` returns `false` always. Same reasoning as sealed classes.

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

### Iteration 28: Ambiguity Detection (LOWER CONFIDENCE)

**Impact**: 5-8 tests (MEDIUM CONFIDENCE — needs debugging to confirm)

**Problem**: When multiple inner classes with same name exist from different supertypes, java-direct resolves one instead of returning null (which would trigger MISSING_DEPENDENCY_CLASS).

**Tests**: `testInheritanceAmbiguity*`, `testClash`

**MUST DEBUG FIRST** to confirm these share the same root cause.

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
