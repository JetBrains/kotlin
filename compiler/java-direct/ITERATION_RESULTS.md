# Java-Direct: Iteration Results Log

**Current status**: 1178/1178 box + 1513/1513 phased (2793/2793, 100%). No known won't-fix.

**Last archived**: `implDocs/archive/ITERATION_RESULTS_2026_06_01.md` (entries through 2026-06-01).

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

### 2026-06-09 — Minify supertype cycle breaker to a session-keyed guard
- **Change**: Replaced the per-file `JavaSupertypeCycleChecker` (thread-local deque + dead
  `recordCycleEdge`/`consumeCycleEdges` diagnostic machinery, never wired to a diagnostic) with a
  session-registered `JavaModelSupertypeWalkGuard` + `cycleGuardedSupertypeWalk`, co-located with
  `cycleSafeClassLikeSymbol`/`JavaModelInFlightResolutions` and mirroring its shape (concurrent
  per-session set, no thread-local). Behaviour is unchanged: re-entry on an in-flight `ClassId`
  returns the caller default, bounding `A→B→A` Java inheritance cycles.
- **Comments**: `cycleSafeClassLikeSymbol` KDoc now states the *hypothetical* re-entrance trigger
  (no IntelliJ-test mention); `JavaCycleBreakerTest` documents the real `testIntellij_vcs_git` /
  KT-74097 scenario (`GitSimpleEventDetector.Event.@Deprecated`, refs to
  `implDocs/archive/ITERATION_RESULTS_2026_05_11.md`) for the in-flight guard and the hypothetical
  malformed-cyclic-Java pattern for the supertype guard.
- **Files**: `JavaModelSessionAccess.kt` (+guard), `JavaTypeResolver.kt`, `JavaFileContext.kt`
  (−`cycleChecker`), `JavaClassFinderOverAstImpl.kt` (+register), `JavaCycleBreakerTest.kt`;
  deleted `JavaSupertypeCycleChecker.kt`.
- **Tests**: `:compiler:java-direct:test` 2816/2816 (455 files, 0 failures); `JavaCycleBreakerTest`
  4/4 (each breaker proven load-bearing — `StackOverflowError` when the guard component is absent).
- **Result**: green; valid-code paths unaffected, both breakers stay out of the way.
