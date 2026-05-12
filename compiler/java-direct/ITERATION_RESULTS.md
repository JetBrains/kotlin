# Java-Direct: Iteration Results Log

**Current status**: 1178/1178 box + 1513/1513 phased (2793/2793, 100%).

**Last Updated**: 2026-05-12 (live log reset; all entries 2026-04-22 → 2026-05-11
archived to `implDocs/archive/ITERATION_RESULTS_2026_05_11.md`).

> **Caveat on historical numbers.** Before 2026-04-28, the `JavaUsingAst*` test
> generators did **not** actually route `// FILE: *.java` blocks through
> `java-direct`'s AST — they fell through to PSI's `JavaClassFinderImpl`. Any
> "1168/1168 box" / "1454/1456 phased" / "feature complete" status claim dated
> before 2026-04-28 was measured against the PSI loader, not `java-direct`. The
> 2026-04-28 framework fix grew the suite to 2793 tests and surfaced fresh
> regression categories, all resolved by 2026-05-11.

## Recent history (one-liners)

- **2026-05-11** — Cat E ASM `Frame.merge` crashes resolved: traced to
  `JavaFieldOverAst.initializerValue` not coercing the evaluated constant to
  the field's declared primitive type. All 11 java-direct-only IJ FP failures
  now pass.
- **2026-05-08 → 2026-05-10** — IJ FP regression delta cleanup (Cat A-E):
  inherited-nested-class lookup over binary supertypes, private interface
  methods, Scala companion-module `$` filter, qualified raw-form nested
  classes, cross-language `ConstantEvaluator`, star-imported binary
  supertypes, `@NotNull T[]` double application, and nested-class
  explicit-import `ClassId` splitting.
- **2026-05-08** — `LazySessionAccess` re-entrance guard (KT-74097 / same-thread
  `PUBLICATION` lazy re-entrance), `extractStaticImports` parser-shape fix,
  nested-record implicit `static` (JLS §8.10.3).
- **2026-05-06 → 2026-05-07** — Step 4.5a-c of
  `implDocs/FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md`: public Java-model
  interface rollback completed (`resolve(...)`, `resolveAnnotation(...)`,
  `resolveEnumClass(...)`, `containingClassIds`, `isResolved` deleted from
  `core/compiler.common.jvm/.../structure/`).
- **2026-05-04 → 2026-05-05** — Merged refactoring plan landed (PSI removal
  × resolver unification, Stages 1-4); `BinaryJavaClassFinder` follow-ups.
- **2026-04-28 → 2026-04-30** — Test framework wiring fix; PSI-removal Phase 1
  (`BinaryJavaClassFinder` behind `kotlin.javaDirect.useBinaryClassFinder`
  flag, default-OFF in production); shared-FIR PSI-path regression gating.

For full root-cause analyses, fixes, and test results, see
`implDocs/archive/ITERATION_RESULTS_2026_05_11.md`.

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

Earlier entries have been moved to dated archives under `implDocs/archive/`:

- `implDocs/archive/ITERATION_RESULTS_2026_05_11.md` — entries 2026-04-22 →
  2026-05-11 (this archive). Covers post-refactoring cleanup, PSI removal
  (Phase 1-2), merged refactoring plan (Stages 1-4 + 4.5a-c public-interface
  rollback), the IJ-FP regression delta (Cat A-E), and the
  `JavaUsingAst*` test framework wiring fix.
- `implDocs/archive/ITERATION_RESULTS_2026_04_22.md` — full log of Phases A-E
  of `REFACTORING_PLAN_2026_04_21.md`: Phase B regression investigation,
  Phase C measurements, Phase D implementation, Phase E cleanup.
- `implDocs/archive/REFACTORING_PLAN_2026_04_21.md` — the 5-phase plan (A-E).
- `implDocs/archive/MEASUREMENTS_2026_04_22.md` — Phase C measurement data
  (8 hypotheses, 3 corpora, corrected classloader-isolation methodology).
- `implDocs/archive/REFACTORING_STEPS_2026_04_17_DETAILS.md` — earlier
  refactoring steps 1.3-3.6.
- `implDocs/archive/LAZY_PACKAGE_INDEXING_PLAN_2026_04_21.md` — lazy
  per-package indexing design (implemented).
- `implDocs/archive/ITERATIONS_52_71_DETAILS.md` — iterations 52-71
  (2026-03-23 → 2026-04-16): wrong-arity type arguments, transitive
  inherited inner class resolution, performance round (61-65), cross-package
  inherited inner classes, multi-field declarations, and the original
  `JavaResolutionContext` split into collaborators.
- `implDocs/archive/ITERATIONS_37_51_DETAILS.md`,
  `implDocs/archive/ITERATIONS_27_36_DETAILS.md`,
  `implDocs/archive/ITERATIONS_24_26_DETAILS.md`,
  `implDocs/archive/ITERATIONS_17_23_DETAILS.md`,
  `implDocs/archive/ITERATIONS_7_16_DETAILS.md`,
  `implDocs/archive/ITERATIONS_1_6_DETAILS.md` — earlier numbered iterations.

### Open items carried forward

- **Context-level `tryResolve` cache** (`PERFORMANCE_REVIEW_2026-04-20.md` §2 #6)
  — deferred with a recorded correctness argument. Only revisit if profiling
  shows `resolve()` as a measurable bottleneck.
- **Variant D of `FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` §12 Q1** — the
  `FirJavaClass.javaClass` visibility flip — preserved as a fallback in the
  proposal but not taken; `directSupertypeClassIds()` (Variant C) is shipped.
- **Build-time enforcement that `LazySessionAccess` is the only `ThreadLocal`
  / re-entrance choke-point in resolution code** — a grep gate or detekt rule
  could forbid `ThreadLocal` in `compiler/java-direct/.../resolution/` to avoid
  reintroducing the old per-thread re-entrance pattern.
