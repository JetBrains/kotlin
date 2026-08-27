# Java-Direct: Iteration Results Log

**Current status**: `:compiler:java-direct:test` full suite green, 2839/2839 (100%). No known won't-fix.

**Last archived**: `implDocs/archive/ITERATION_RESULTS_2026_08_26.md` (entries through 2026-08-26).

---

## How to write entries

This log is read into the agent's context every session, so **entries must stay short**.

- **Newest entry on top.** One entry per landed change or per investigated regression.
- **Cap each entry at ~15 lines / ~150 words.** If the rationale, a trace, or a
  measurement table is longer, put it in a dedicated `implDocs/<TOPIC>.md` and link to it
  from the entry — do not inline it here.
- **Use the fixed fields below.** No free-form multi-paragraph narration; if a field needs
  more than ~2 lines, link out instead.
- **No pasted logs, stacktraces, or diffs.** Quote the single line that matters; link the rest.
- **Archive when this file passes ~600 lines** (see `AGENT_INSTRUCTIONS_COMMON.md` →
  "Docs Maintenance"): `git mv` it to
  `implDocs/archive/ITERATION_RESULTS_<last-entry-date>.md`, add an archive banner, and
  reset this file to the template below.

### Entry template

```
### YYYY-MM-DD — <one-line title>
- **Change**: what changed and why (1–3 lines).
- **Files**: key files touched (+N/−M LoC if useful).
- **Tests**: suites run + counts (e.g. box 1178/1178, phased 1513/1513).
- **Result**: green / regression fixed / won't-fix — link to a detail doc if there is one.
```

---

<!-- Add new entries below, newest first. -->

### 2026-08-27 — comment self-check promoted to a non-negotiable rule
- **Change**: three of the four instruction fixes proposed after the repeated comment passes.
  The self-check moved from the closing paragraph of the comment section into the
  `⚠ Non-Negotiable Rules` block as rule 6, with the `git diff -U0 | grep` command that found the
  violations both times; a "Rejected comments and their replacements" table added next to the gate
  (narrative / counterfactual / restatement / fact in two places, real lines from this module); and a
  rule against carrying the register of an analysis or review reply into code comments. The
  root-`AGENTS.md` pointer (proposal C) was left out on request.
- **Files**: `AGENT_INSTRUCTIONS_COMMON.md` (+24 lines).
- **Tests**: not run — docs-only.
- **Result**: green.

### 2026-08-27 — third comment pass: comments restating the code deleted
- **Change**: review find — several comments narrated the lines below them. Deleted the static-outer chain
  note (the loop's own condition), the prose list of the three single-name lookup calls (the javac
  divergence and the two pinning testData files kept), the `finderOver` KDoc, and the recovery half of the
  out-of-scope note; the type-parameter identity trap moved to the return that hands the declaring class's
  own instances over, and `firBackedJavaType`'s KDoc reduced to what `declarationChainRoot` is.
- **Files**: `model/JavaTypeOverAst.kt`, `JavaParsingImplicitOuterTypeArgumentsTest.kt` (−13 comment lines).
- **Tests**: `:compiler:java-direct:test` green.
- **Result**: green — comments only, no behavior change.

### 2026-08-27 — comment pass over `JavaClassifierTypeOverAst.computeClassifier`
- **Change**: reread the pre-existing comments in the body of `computeClassifier` against the comment
  conventions. The numbered walkthrough of the single-name lookup order collapsed to the javac divergence
  and the two testData files that pin it; the justification of the in-scope pass shortened to why it runs
  before the `resolve` fallback; the cross-file comment deleted (`classifierAdapterFor` documents it) and
  the duplicated KT-87797 TODO dropped (it lives on `findInheritedTypeParameter`).
- **Files**: `model/JavaTypeOverAst.kt` (−14 comment lines).
- **Tests**: box + phased + module unit tests green.
- **Result**: green — comments only, no behavior change.

### 2026-08-27 — comment pass over the raw-type change
- **Change**: reread the comment lines of the previous entry's diff against the comment conventions.
  Deleted the KDoc of `unknownArguments` and three narrative block comments in the tests, dropped the
  counterfactual passages (rejected fallback, "instead of degrading to `List<*>`") and the coverage
  meta-commentary in the incremental test, moved the type-parameter identity trap to a single place
  (`firBackedJavaType`), and shortened the two new testData headers.
- **Files**: `model/JavaTypeOverAst.kt`, `resolution/JavaTypeResolver.kt`,
  `JavaParsingImplicitOuterTypeArgumentsTest.kt`, `JavaParsingTypeSystemTest.kt`,
  `IncrementalJavaClassFromPreviousOutputTest.kt`, 2 `testData/diagnostics/tests/j+k/*.kt` (−45 comment lines).
- **Tests**: box + phased + module unit tests green; PSI and light-tree phased gates green.
- **Result**: green — comments only, no behavior change.

### 2026-08-27 — raw-ness derived from `null` type arguments; `firBackedJavaType` gets a declaration-chain root
- **Change**: second review round on PR #7500. `JavaClassifierTypeOverAst.typeArguments` now emits one entry
  per type parameter it has to supply and `null` where nothing is known (PSI's contract and order), and
  `isRaw` is read off it — `computeIsRaw` and `isQualifiedByInheritor` deleted. Fixes the case where a
  simple-name reference to an inner class of a generic outer, from a class that neither encloses nor inherits
  it, produced `ConeErrorType(ConeUnresolvedNameError)` instead of a raw type. `firBackedJavaType` takes a
  `declarationChainRoot` and owns the flexible unwrap plus an explicit `ConeTypeParameterType` arm, so nested
  recovered arguments no longer degrade to `*`; `recoveredOuterTypeArgument` collapsed into one call.
  `IncrementalJavaClassFromPreviousOutputTest` split into the attribute-present and attribute-stripped pair.
- **Files**: `model/JavaTypeOverAst.kt`, `resolution/JavaTypeResolver.kt`,
  `JavaParsingImplicitOuterTypeArgumentsTest.kt`, `JavaParsingTypeSystemTest.kt`,
  `IncrementalJavaClassFromPreviousOutputTest.kt`, 2 new `testData/diagnostics/tests/j+k/*.kt`.
- **Tests**: box + phased green; module unit tests 154/154; PSI and light-tree phased gates green;
  both new testData files verified red without the fix.
- **Result**: green — rationale in `implDocs/RAW_TYPE_ARGUMENT_UNIFICATION_2026_08_27.md`.

### 2026-08-26 — docs pass: log archived, instructions split into common + module parts
- **Change**: archived the iteration log and the fully-landed `MERGED_REFACTORING_PLAN_2026_05_04.md`;
  split `AGENT_INSTRUCTIONS.md` into a module-independent `AGENT_INSTRUCTIONS_COMMON.md`
  (shell/Gradle discipline, comment and writing style, simplification discipline, docs maintenance)
  and the java-direct-specific remainder; rewrote the comment/writing rules from a review of the
  branch's session logs and a comparison against human-written compiler modules; trimmed `ReadMe.md`
  (scenarios now live only in `implDocs/RESOLUTION_SCHEMA.md`).
- **Files**: `AGENT_INSTRUCTIONS.md`, new `AGENT_INSTRUCTIONS_COMMON.md`, `ReadMe.md`,
  `ITERATION_RESULTS.md`, `implDocs/archive/*` (2 moves + banners).
- **Tests**: not run — docs-only.
- **Result**: green.
