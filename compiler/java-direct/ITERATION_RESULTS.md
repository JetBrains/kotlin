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
