# Java-Direct: Iteration Results Log

**Current status**: `:compiler:java-direct:test` full suite green, 2838/2838 (100%). No known won't-fix.

**Last archived**: `implDocs/archive/ITERATION_RESULTS_2026_07_13.md` (entries through 2026-07-13).

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
- **Archive when this file passes ~600 lines** (see `AGENT_INSTRUCTIONS.md` →
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

### 2026-07-13 — Memoize `JavaClassOverAst.supertypes` (IJ-FP regression)
- **Change**: `supertypes` was a recomputing `get()` that returned fresh `JavaClassifierTypeOverAst`
  instances on every read; FIR forces it from two lazy slots per class (`FirJavaClass.superTypeRefs`
  enhancement and `directSupertypeClassIdsCache`), so each supertype's `classifier` (a per-instance
  lazy hitting the symbol provider) resolved from cold twice. Made it `by lazy(PUBLICATION)` so both
  reads share instances and each supertype resolves once. List build allocates wrappers only → still
  resolution-safe.
- **Files**: `model/JavaClassOverAst.kt` (supertypes get()→by lazy).
- **Tests**: box 1178/1178, phased 1513/1513 (0 FAILED).
- **Result**: regression fixed. IntelliJ `testIntellij_platform_ide_impl` warm frontend (isolated bench,
  4 iters, same build): java-direct 21.5s wall / 20.8s CPU vs legacy 25.3s / 23.6s — was ~+8% before.
