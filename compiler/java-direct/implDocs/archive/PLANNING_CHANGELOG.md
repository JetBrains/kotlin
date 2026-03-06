# Java-Direct: Planning Changelog (Archived)

**Archive Date**: 2026-03-06

> This document tracked iteration plan changes during active development. Now archived as iterations 1-16 are complete.

---

## 2026-03-06: Test Infrastructure Changes

- Changed test extraction method from common compiler testdata
- New test classes:
  - `JavaUsingAstBoxTestGenerated` (1166 tests, replaces old box tests)
  - `JavaUsingAstPhasedTestGenerated` (327 tests, replaces old diagnostic tests)
- Updated all documentation to reference new test classes
- Added `JavaParsingTest` mention for custom unit tests
- Current status: 1317/1493 passing (88.2%)

## 2026-03-06: Documentation Cleanup

- Archived iterations 7-16 details to `implDocs/archive/ITERATIONS_7_16_DETAILS.md`
- Condensed FIXING_ITERATIONS.md, ITERATION_RESULTS.md
- Extracted generic learnings to AGENT_INSTRUCTIONS.md
- Moved obsolete design docs to archive

## 2026-03-05: Ad-Hoc Approach for 11-16

Iterations 11-16 followed ad-hoc error analysis rather than detailed upfront planning, which proved more effective for interconnected issues.

## 2026-03-04: Post-Iteration 7c Restructuring

After Iteration 7c (101/138 = 73.2%), restructured remaining iterations:
- Deleted old Iterations 8, 10 (completed in 7c)
- Merged old Iterations 11+13 into Iteration 8 (Annotations)
- Added Iteration 9 (SAM/Interface Fields)
- Renumbered to 8-14

---

*Archived: 2026-03-06*
