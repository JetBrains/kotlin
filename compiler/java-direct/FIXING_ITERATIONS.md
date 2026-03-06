# Java-Direct: Fixing Iterations

## Document Purpose

This document contains iteration plans for the `java-direct` module.

**Prerequisites**: Read `AGENT_INSTRUCTIONS.md` before starting any iteration  
**Status**: Iteration 16 complete — 532/601 tests passing (88.5%)  
**Last Updated**: 2026-03-06

---

## How to Use This Document

1. **Before starting**: Read `AGENT_INSTRUCTIONS.md` thoroughly
2. **For each iteration**: Follow the 4-phase template (Analysis → Reproduction → Implementation → Validation)
3. **After completing**: Update `ITERATION_RESULTS.md` with findings

---

## Iterations 1-6: Foundation (Archived)

**Status**: ✅ Completed  
**Result**: 0 → 90/138 (65.2%) box tests passing  
**Archive**: `implDocs/archive/ITERATIONS_1_6_DETAILS.md`

| Iteration | Focus | Key Result |
|-----------|-------|------------|
| 1 | Initial Analysis | Fixed `hasDefaultConstructor()` |
| 2 | Type Resolution Architecture | Verified classifierQualifiedName approach |
| 3 | Import Handling | Implemented JavaImports |
| 4 | Star Import Resolution | Callback pattern + parameter parsing |
| 5 | Type Arguments | Generic type arguments + visibility |
| 6 | Hybrid JavaClassFinder | Combined source+binary class finding |

**Key Architectural Decisions**:
1. Type resolution in FIR layer (not Java Model)
2. Callback pattern for star imports: `resolve(tryResolve: (String) -> Boolean)`
3. Hybrid finder: source-first, binary-fallback

---

## Iterations 7-16: Core Implementation (Archived)

**Status**: ✅ Completed  
**Result**: 90/138 → 532/601 (88.5%) tests passing  
**Archive**: `implDocs/archive/ITERATIONS_7_16_DETAILS.md`

| Iteration | Focus | Key Result |
|-----------|-------|------------|
| 7a-c | Arrays, Imports, Type Params | Resolution context pattern, wildcards |
| 8 | Annotations/Nullability | TYPE_USE annotation handling |
| 9 | Interface Fields/Methods | Implicit modifiers (static/final/abstract) |
| 10 | Nested Interfaces/Enums | Implicit static for nested types |
| 11 | External Type Arguments | FIR null classifier branch fix |
| 12 | Fragmented Star Imports | Parser edge case handling |
| 13 | Annotation Callback | Unified resolution pattern |
| 14 | External Raw Types | ConeRawType in FIR |
| 15 | TYPE_USE Filtering | Filter non-TYPE_USE annotations |
| 16 | Raw Type Bounds | Type param scope in bounds |

> ⚠️ **Deep Context Recovery**: Only consult the archive documents if you need to understand specific implementation decisions or debug regressions. The `AGENT_INSTRUCTIONS.md` contains extracted learnings.

---

## Note on Iterations 11-16

Iterations 11-16 followed an **ad-hoc error analysis approach** rather than detailed pre-planned prompts. The workflow was:

1. Run tests and categorize failures
2. Pick the most impactful error pattern
3. Debug using exception-based inspection
4. Implement fix and verify
5. Document in ITERATION_RESULTS.md

This proved more effective than detailed upfront planning for the later iterations, as the remaining issues were interconnected and often revealed themselves during debugging.

---

## Remaining Work

### Current Status: 532/601 (88.5%)

| Category | Count | Notes |
|----------|-------|-------|
| Box tests | 3 failing | Annotation args, wildcards, overloads |
| Diagnostic tests | 66 failing | Various edge cases |

### Remaining Box Test Failures

| Test | Issue |
|------|-------|
| `testConstValAsAnnotationArgumentInJava` | Annotation argument handling |
| `testInheritanceWithWildcard` | NoSuchMethodError - IR fake override |
| `testKt48590` | NONE_APPLICABLE - overload resolution |

### Potential Future Iterations

**Iteration 17: Annotation Arguments** (if needed)
- Handle complex annotation arguments (`@Foo(value = "x")`)
- Support array arguments, nested annotations

**Iteration 18: Wildcard Edge Cases** (if needed)
- IR fake override generation issues
- Complex wildcard inheritance

**Iteration 19: Overload Resolution** (if needed)
- Investigate `NONE_APPLICABLE` errors
- May be FIR-level issues, not java-direct

---

## Document Change Log

- 2026-03-06: **Major cleanup** - Archived iterations 7-16, condensed document
- 2026-03-05: Iterations 11-16 completed (ad-hoc approach)
- 2026-03-04: Iterations 7-10 completed
- 2026-02-23: Initial structure with iterations 1-6
