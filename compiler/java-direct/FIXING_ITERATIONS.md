# Java-Direct: Fixing Iterations

## Document Purpose

This document tracks iteration history and plans for the `java-direct` module.

**Prerequisites**: Read `AGENT_INSTRUCTIONS.md` before starting any iteration  
**Status**: Iteration 23 complete — 1134/1166 box tests (97.2%), 300/329 phased tests (91.2%)  
**Last Updated**: 2026-03-12

---

## Recommended Approach for Remaining Work

For remaining ~62 failing tests, use the **ad-hoc debugging approach** that proved effective in iterations 11-16:

1. **Run tests and categorize** — Group by error message pattern
2. **Debug 2-3 representative tests** — Verify root cause (same symptom ≠ same cause)
3. **Implement fix** — Target the verified root cause
4. **Run PSI regression tests** if FIR files modified
5. **Document findings** in `ITERATION_RESULTS.md`

**Why not detailed upfront plans**: Iterations 17-21 used detailed plans but consistently overestimated fix counts because same error message often has multiple distinct causes.

---

## Archived Iterations

| Archive | Iterations | Result |
|---------|------------|--------|
| `implDocs/archive/ITERATIONS_1_6_DETAILS.md` | 1-6 | 0 → 90/138 (65.2%) |
| `implDocs/archive/ITERATIONS_7_16_DETAILS.md` | 7-16 | 90 → 532/601 (88.5%) |
| `implDocs/archive/ITERATIONS_17_23_DETAILS.md` | 17-23 | 1075 → 1134/1166 (97.2%) |

**Key patterns established** (see archives for implementation details):
- Callback pattern for resolution (types, annotations, enums, constants)
- Two-phase type parameter construction
- PSI/java-direct discrimination in shared FIR code
- Implicit supertypes for Java special class kinds

---

## Remaining Test Categories (~62 failing)

Based on analysis of current failures:

| Category | Est. Count | Complexity | Notes |
|----------|-----------|------------|-------|
| Baseline diffs | ~20 | LOW | May need individual triage or baseline updates |
| Annotation edge cases | ~10 | MEDIUM | Const val refs, annotation instantiation |
| Visibility/access | ~5 | MEDIUM | Protected field access patterns |
| Reflection/metadata | ~5 | HIGH | May require FIR-level changes |
| Modern Java features | ~5 | MEDIUM | Records, sealed classes |
| Other edge cases | ~17 | VARIES | Need individual analysis |

---

## Future Iteration Template

When starting a new iteration, use this template:

```markdown
## Iteration N: [Title] - YYYY-MM-DD

### Status
- [ ] In Progress / ✅ Completed

### Root Cause Analysis
[Debug 2-3 representative tests first. Document actual cause, not just symptom.]

### Fix
[Describe the solution. Reference files modified.]

### Test Results
- Box tests: X/1166 passing
- Phased tests: X/329 passing

### Key Learnings
[What should be added to AGENT_INSTRUCTIONS.md?]
```

---

## Document Change Log

- 2026-03-12: Consolidated iterations 17-23 to archive, simplified document
- 2026-03-10: Iterations 20-22 completed
- 2026-03-06: Iterations 17-17b completed
- 2026-03-05: Iterations 11-16 completed (ad-hoc approach)
- 2026-03-04: Iterations 7-10 completed
- 2026-02-23: Initial structure with iterations 1-6
