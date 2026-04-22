# Java-Direct: Iteration Results Log

**Current status**: 1168/1168 box + 1454/1456 phased (2679/2681, 99.9%), 2 known won't-fix.

**Last Updated**: 2026-04-22 (Phases A-E complete, archive reset)

### Entry Template

```markdown
## [Title] — [Date]

### Overview
[1-2 sentence summary of what was done and why.]

### Changes
[Bullet list of concrete changes: file, what changed, why.]

### Test Results
[Suite name, pass/fail count, any regressions.]

### Files Modified
| File | Change |
|------|--------|
| `file.kt` | description |

### Key Learnings
[Bullet list of non-obvious findings useful for future work.]
```

> **Add new entries below this line.** Most recent first. Separate with `---`.

---

## Archived Iteration History

All entries from the 2026-04-17 through 2026-04-22 refactoring work (Phases A-E of
`REFACTORING_PLAN_2026_04_21.md`) have been archived to:

- `implDocs/archive/ITERATION_RESULTS_2026_04_22.md` — full log with Phase B regression
  investigation, Phase C measurements, Phase D implementation, Phase E cleanup
- `implDocs/archive/REFACTORING_PLAN_2026_04_21.md` — the 5-phase plan (A-E)
- `implDocs/archive/MEASUREMENTS_2026_04_22.md` — Phase C measurement data (8 hypotheses,
  3 corpora, corrected classloader-isolation methodology)
- `implDocs/archive/REFACTORING_STEPS_2026_04_17_DETAILS.md` — earlier refactoring steps 1.3-3.6
- `implDocs/archive/LAZY_PACKAGE_INDEXING_PLAN_2026_04_21.md` — lazy per-package indexing design (implemented)

### Open items carried forward

- **Context-level `tryResolve` cache** (`PERFORMANCE_REVIEW_2026-04-20.md` §2 #6) — deferred
  with a recorded correctness argument. Only revisit if profiling shows `resolve()` as a
  measurable bottleneck.
