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

## Post-refactoring review: readability cosmetics — 2026-04-22

### Overview

Independent code review (`implDocs/reviews/r1.md`) cross-checked against the completed
Phases A-E and Phase B regression reversals. Six low-risk readability items from the
review's suggestions 2-7 were applied.

### Changes

- **Trim `rawTypeNameParts` KDoc** (`JavaTypeOverAst.kt`) — replaced the inline "83%"
  measurement detail with a concise one-liner; the data lives in
  `archive/MEASUREMENTS_2026_04_22.md` §7.4.
- **Trim `CacheHelpers.kt` file KDoc** — consolidated the "why not `by lazy(PUBLICATION)`"
  and "why not explicit backing fields" rationale from 31 lines to 19, preserving the key
  insight (24B+8B per delegate × 200K instances).
- **Rename `findInPhase1JavaModel` → `walkJavaSourceSupertypes`** and
  **`findInPhase2ClassIdWalk` → `walkBinarySupertypes`** (`JavaInheritedMemberResolver.kt`)
  — the old "Phase 1 / Phase 2" names read as compilation phases; the new names describe
  the data source (Java model vs binary/Kotlin supertypes). Updated all KDoc references.
- **Rename `AggregatedInheritedInnerClassesHolder` → `InheritedInnerCache`** and
  **`aggregatedInheritedInnerClassesHolder` → `inheritedInnerCache`**
  (`JavaResolutionContext.kt`) — shorter name for the `@Volatile`-wrapped cache holder.
- **Add comment on `JavaAnnotationOverAst.resolve()`** — one-liner explaining that
  resolution is callback-based via `resolveAnnotation()`.
- **Trim static-inner-class context comment** (`JavaClassOverAst.kt:162-167`) — removed the
  3-line mirror explanation of the `else` branch; the first two lines already explain the
  non-static case and the code is self-evident.

### Test Results

No behavioral changes — renames and comment edits only. Compilation verified via IDE.

### Files Modified

| File | Change | Lines |
|------|--------|-------|
| `JavaTypeOverAst.kt` | Trim `rawTypeNameParts` KDoc | −1 |
| `CacheHelpers.kt` | Trim file-level KDoc | −12 |
| `JavaInheritedMemberResolver.kt` | Rename two methods + update KDoc | ~0 (rename) |
| `JavaResolutionContext.kt` | Rename class + field | ~0 (rename) |
| `JavaAnnotationOverAst.kt` | Add one-liner on `resolve()` | +1 |
| `JavaClassOverAst.kt` | Trim inner-class context comment | −3 |
| **Net** | | **−15 lines** |

### Key Learnings

- **Review after refactoring catches different things than review before.** The original
  review (r1.md) independently flagged `filterTypeUseAnnotations` caching and
  `resolveSimpleNameToClassIdImpl` extraction — both of which had already been tried and
  reverted (P1 and R12+O10). Cross-checking against the refactoring history before acting
  avoided re-introducing known regressions.
- **Method names that describe mechanism ("Phase 1/2") age worse than names that describe
  data source ("JavaSource/Binary").** The Phase 1/Phase 2 naming was clear when the two
  methods were freshly extracted in B.3 but confusing to a fresh reader.

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
