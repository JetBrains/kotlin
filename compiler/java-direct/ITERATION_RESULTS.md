# Java-Direct: Iteration Results Log

## Document Purpose

Append-only log of iteration findings, decisions, and learnings.

**Current status**: See `FIXING_ITERATIONS.md` for up-to-date test counts and remaining work.

**Last Updated**: 2026-03-13

---

## Progress Summary

| Phase | Iterations | Box Tests | Phased Tests |
|-------|------------|-----------|--------------|
| Foundation | 1-6 | 90/138 (65.2%) | -- |
| Core Implementation | 7-16 | 139/142 -> 1075/1166 (92.2%) | 242/327 (74.0%) |
| Advanced Features | 17-27 | 1150/1167 (98.5%) | 1374/1442 (95.3%) |

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

### Iterations 24-26: Regression Fixes, Sealed Classes
**Archive**: `implDocs/archive/ITERATIONS_24_26_DETAILS.md`

| Iteration | Focus | Impact |
|-----------|-------|--------|
| 24 | Constant eval, protected static, sibling inner classes | +15 tests |
| 24b | Cyclic type bounds StackOverflowError | +6 tests |
| 25 | Inherited inner class resolution | +2 tests |
| 25c | Interface nested class static flag | +8 tests |
| 26 | Sealed classes (`isSealed`, `permittedTypes`) | +9 tests |

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
- [ ] In Progress / Completed

### Root Cause Analysis
[Debug 2-3 representative tests first. Document actual cause.]

### Fix
[Solution description and files modified.]

### Test Results
- Box: X/1167, Phased: X/1442

### Key Learnings
[What to add to AGENT_INSTRUCTIONS.md or implDocs/?]

### Investigation State (if stopping mid-work)
- **Current test**: [test being investigated]
- **Hypothesis**: [what you think is causing the failure]
- **Tried**: [approaches attempted and why they failed]
- **Next step**: [what to try next when resuming]
```

---

---

*For detailed iteration histories, see `implDocs/archive/`*
