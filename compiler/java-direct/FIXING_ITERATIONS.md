# Java-Direct: Fixing Iterations

## Current Status (Single Source of Truth)

| Metric | Value |
|--------|-------|
| **Last Iteration** | 29 (2026-03-13) |
| **Box Tests** | 1152/1167 passing (98.7%) |
| **Phased Tests** | 1349/1442 passing (93.5%) |
| **Combined** | ~2501/2609 passing, **~108 failing** |

**Prerequisites**: Read `AGENT_INSTRUCTIONS.md` before starting any iteration.

---

## Estimation Rules

Estimates have been consistently wrong (5-60% accuracy). Follow these rules:

1. **Debug 2-3 representative tests** before estimating — run with exception debugging, trace actual code path
2. **Categorize by code path**, not test name or error message — different symptoms often have different root causes
3. **Apply 50% discount** for unfamiliar code areas
4. **Maximum estimate**: 15 tests per category unless proven otherwise
5. **Do NOT trust code snippets** in this document — always verify AST node names via debugging first

---

## Remaining Failures (125 tests)

### Completed Features (iterations 26-29)

| Category | Status                  | Tests Fixed           |
|----------|-------------------------|-----------------------|
| **Sealed Classes** | Done (iter 26)          | 9 tests fixed         |
| **Java Records** | Done (iter 27, iter 28) | all record tests pass |
| **Ambiguity Detection** | Done (iter 29)          | 2 tests fixed (same-file only) |

### Next Priorities

#### Ambiguity Detection — ✅ PARTIALLY DONE (iter 29)

**Status**: Fixed 2/4 tests. Remaining failure (`testInheritanceAmbiguity2`) cannot be fixed due to architectural limitation (cross-file ambiguity detection requires parsing all files together, but java-direct uses on-demand per-file parsing).

**Tests fixed**:
- ✅ `testInheritanceAmbiguity` (same-file)
- ✅ `testInheritanceAmbiguity3` (same-file)
- ❌ `testInheritanceAmbiguity2` (cross-file, architectural limitation)

#### Type Parameter Scoping — ~6-10 tests — LOW CONFIDENCE

**Problem**: Type parameters from outer classes not visible in inner classes in some scenarios.

**Tests to debug first**: `testInnerWithTypeParameter`, `testSeveralInnersWithTypeParameters`

**Status**: Needs investigation — may be FIR integration issue, not java-direct.

#### Import/Package Edge Cases — ~8-10 tests — LOW CONFIDENCE

**Problem**: Complex scenarios where package names clash with class names, or nested imports clash with top-level classes.

**Tests to debug first**: `testTopLevelClassVsPackage`, `testNestedClassClash`, `testCurrentPackageAndExplicitNestedImport`

**Approach**: Review import resolution in `JavaResolutionContext.kt`.

#### Records FIR Integration — **DONE** (iter 28)

All 6 record tests pass. See iteration 28 in `ITERATION_RESULTS.md` for details.

#### Enum Handling — ~3-5 tests — LOW CONFIDENCE

**Tests**: `testEnumEntriesFromJava`, `testStaticImportFromEnumJava`, `testJavaEnum`

**Status**: Needs investigation of enum entries generation for java-direct.

#### Baseline Differences — ~50-60 tests — NEEDS TRIAGE

**IMPORTANT**: Many "baseline diffs" are real java-direct bugs, not cosmetic differences.

**Triage process** (for each test):
1. Run with PSI-based FIR: `./gradlew :compiler:fir:fir-jvm:test --tests "FirLightTreeBlackBoxCodegenTestGenerated.*testName*" -q`
2. If PSI passes but java-direct fails -> real java-direct bug, recategorize by actual error
3. If both fail -> general FIR issue, exclude from java-direct scope

**Known real bugs in this category** (from iteration 25c investigation):
- `testInheritedInnerAndNested` was a real `isStatic` bug, not a cosmetic diff

---

## Archived Iterations

| Archive | Iterations | Result |
|---------|------------|--------|
| `implDocs/archive/ITERATIONS_1_6_DETAILS.md` | 1-6 | 0 -> 90/138 (65.2%) |
| `implDocs/archive/ITERATIONS_7_16_DETAILS.md` | 7-16 | 90 -> 532/601 (88.5%) |
| `implDocs/archive/ITERATIONS_17_23_DETAILS.md` | 17-23 | 1075 -> 1134/1166 (97.2%) |
| `implDocs/archive/ITERATIONS_24_26_DETAILS.md` | 24-26 | 1134/1166 -> 1150/1167 (98.5%) |

**Key patterns established** (see archives and `ITERATION_RESULTS.md`):
- Callback pattern for resolution (types, annotations, enums, constants)
- Two-phase type parameter construction
- PSI/java-direct discrimination in shared FIR code
- Implicit supertypes for Java special class kinds
- java.lang implicit import handling
- Protected static vs protected and package visibility distinction

### Recent Iterations (24-27)

| Iteration | Category | Tests Fixed |
|-----------|----------|-------------|
| 24 | Constant evaluation, protected static, sibling inner classes | +15 |
| 24b | Cyclic type bounds StackOverflowError | +6 |
| 25 | Inherited inner class resolution | +2 |
| 25c | Interface nested class static flag | +8 |
| 26 | Sealed classes (`isSealed`, `permittedTypes`) | +9 |
| 27 | Java records (Java Model only, FIR integration pending) | +0 |
| 28 | Java records FIR integration (isRecord token fix, isVararg fix, canonical ctor detection) | +6 phased, +2 box |
| 29 | Ambiguity detection for inner classes (same-file only) | +2 phased |

---

## Recommended Approach

Use the **ad-hoc debugging approach** from iterations 11-16:

1. **Pick category** — start with highest impact
2. **Debug 2-3 representative tests** — verify root cause (run pre-implementation checklist from AGENT_INSTRUCTIONS.md)
3. **Check reference implementation** — javac-wrapper or PSI (see `implDocs/ARCHITECTURE.md`)
4. **Implement fix** — target verified root cause only
5. **Run PSI regression tests** if shared FIR files modified
6. **Document in `ITERATION_RESULTS.md`**

**Key insight**: Always check PSI (`JavaElementUtil`) and javac-wrapper implementations first. They often show the correct pattern.

---

## Future Iteration Template

```markdown
## Iteration N: [Title] - YYYY-MM-DD

### Status
- [ ] In Progress / Completed

### Root Cause Analysis
[Debug 2-3 representative tests first. Document actual cause.]

### Fix
[Solution description. Files modified.]

### Test Results
- Box: X/1167, Phased: X/1442

### Key Learnings
[What to add to AGENT_INSTRUCTIONS.md or implDocs/?]
```

---

## Document Change Log

- 2026-03-13: Restructured — merged TEST_FAILURE_ANALYSIS content, removed code snippets, updated metrics to post-iter-27
- 2026-03-12: Iteration 24 complete, updated remaining work analysis
- 2026-03-12: Consolidated iterations 17-23 to archive
