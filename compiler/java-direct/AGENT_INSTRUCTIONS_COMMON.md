# Agent Instructions — Common Part

Module-independent working rules for AI-agent development in this repository. A module that
uses them keeps its own `AGENT_INSTRUCTIONS.md` with module status, key files, test commands
and module-specific patterns, and references this file instead of repeating it
(first adopter: `compiler/java-direct/`).

---

## ⚠ Non-Negotiable Rules (stop immediately if violated)

1. **No command chaining** — NEVER use `&&`, `||`, or `;`. Each command = one tool call.
   Why: the permission system only checks the first token. `|` (piping) is fine.

2. **Always pipe Gradle output to `tee "$SESSION_TMP/..."`** — no exceptions.
   If you forgot `tee`: do NOT rerun Gradle. Grep whatever output you have, or ask the user.

3. **Only the main agent runs Gradle** — subagents MUST NOT invoke `./gradlew`.
   Why: parallel builds corrupt each other's test results and cause excessive CPU and disk load.

4. **NEVER create git commits** — all changes must be reviewed by the user first.

5. **NEVER regenerate shared golden test data** (e.g. `-Pkotlin.test.update.test.data=true`)
   and **NEVER modify shared test data to make a module's tests pass** — fix the
   implementation, or document the difference in the module's iteration log with evidence.

---

## Shell Discipline

### Session temp directory

At the start of each session, export a session temp directory and create it (the module's
instructions define the variable name, e.g. `$JD_TMP`):
```bash
export <VAR>="/tmp/<prefix>_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$<VAR>"
```
**NEVER write directly to `/tmp/`** — always use the session directory.

### Gradle runs: save output, run once

Every Gradle invocation MUST `tee` its output into the session directory. Include
`--stacktrace` for suite and single-test runs. Do NOT use `--info`/`--debug` unless
specifically needed. After a run, **grep the saved file** — never rerun Gradle just to see a
different slice of the same results. Do not pass `--rerun-tasks` / `--no-build-cache` on
routine runs; use `--rerun` (test-task-only) to re-execute tests whose inputs did not change.

---

## Ground Rules

- **Use JetBrains MCP tools** for all project file operations (see `.ai/guidelines.md`).
- **Search before reading**: prefer `search_in_files_by_text` / `search_in_files_by_regex`
  over `get_file_text_by_path` for large files — search tools return only matching lines.
- **Oversized MCP results**: when a call exceeds the token limit, the result is auto-saved
  to `~/.claude/projects/.../tool-results/<tool>-<timestamp>.txt`. Filter it with
  `grep`/`jq` via Bash rather than loading the full file into context.
- **Check `git diff` for unintended changes** after every test run.
- **Run `get_file_problems`** (errorsOnly=false) after edits; fix warnings related to your
  changes.

---

## Source Comment Conventions

These rules apply to **every** source comment or KDoc you add or edit — with extra
strictness in shared compiler modules. Comments are reviewed alongside the code; write them
for a future reader of the **merged** code (an experienced compiler developer), not as a
development journal.

**The default is no comment.** Human-maintained compiler code has roughly half the comment
density of unedited LLM output (~10% of lines vs the ~25% this repo's LLM-authored branches
have reached before cleanup passes). Before writing a comment, pass this gate — a comment is
justified only when it:

1. explains **why** a non-obvious decision was made, or how a genuinely difficult piece
   works when words do it better or shorter than the code itself; or
2. briefly states an **API contract** that saves the reader a detour into the
   implementation; or
3. records a **real trap** (a regression guard, a cycle hazard), ideally with a
   KT-issue or testData reference.

Everything else — delete. When in doubt, delete.

### Write facts, not narratives

The single most repeated review complaint is narrative, justificatory tone. Human comments
in this codebase are terse declarative fragments; match them.

- State what a thing **is**, not the story of why it is shaped that way:
  `// Mapping for classes with separate read-only and mutable equivalents.` — not
  `// This data class acts as a holder for the mapping because we need to handle
  read-only and mutable cases separately.`
- Drop justification clauses (`so that`, `because otherwise`, `required because`,
  `note that`, `it is worth mentioning`) unless the justification *is* the real trap
  being recorded.
- Reference specs briefly: `// JLS 6.5.5.2 member type lookup.` — not a prose
  restatement of the JLS paragraph.
- Prefer minimal punctuation: few em-dashes, few parenthetical asides.
- **Use the codebase's own terminology.** Before naming a concept in a comment, grep for
  how surrounding code already names it (e.g. "previous snippets", not "priors";
  `simpleImports`/`starImports`, not singleType/onDemand). Non-standard vocabulary reads
  as foreign and triggers review questions.

### Specific prohibitions

- **Don't comment the obvious.** If the code says it, or an experienced compiler developer
  sees it at a glance, no comment. This includes restating a function's body in prose and
  `@param` entries that paraphrase the parameter name/type.
- **No counterfactuals.** Do not describe hypothetical alternatives, rejected designs, or
  the previous implementation ("rather than X", "unlike the old Y", "the legacy path
  returned…"). Nobody reading merged code cares what was *not* implemented. Exception:
  the alternative is a real trap a maintainer is likely to fall into — then one terse
  sentence.
- **No caller inventories.** Don't enumerate call sites, users, or anything a one-level
  usages search reveals.
- **One fact, one place.** State a fact at the declaration site only; a use site gets at
  most a short cross-reference, never a repeat of the explanation.
- **No references to transient planning docs** (`implDocs/`-style folders) — not by
  filename, not by section number (`§6.x`), not by stage/phase label. Put the (brief)
  explanation itself in the comment.
- **Describe the current state only.** No narration of past or superseded attempts
  ("used to live behind…", "before the … cleanup", "now deleted…", dated history).
- **Keep it short.** 1–3 lines is the norm. KDoc on internal declarations is the
  exception, not the rule — human modules leave most internal functions undocumented.
  No numbered algorithm walkthroughs or "Scenario A/B/C" breakdowns in KDoc; if the
  algorithm needs that, it belongs in a design doc.

### Keep comments in sync with the code

Stale comments are a recurring review find. Whenever you delete or rename a symbol, or
change behavior, **grep for comments mentioning it** (in the whole module, not just the
edited file) and fix or delete them in the same change. A comment justifying complexity
must be re-verified against the *current* code, not carried forward from an earlier round.

Self-check before finishing any change: reread the diff's comment lines alone. If a
comment would survive neither the gate above nor a reviewer asking "what does this tell
me that the code doesn't?", remove it.

---

## Explanation & Writing Style (logs, docs, review responses)

Reviewers repeatedly had to ask "what does this mean / when is this reached?" about
explanations that were formally correct but too compressed or too abstract. When writing
an iteration-log entry, a design-doc section, or a review reply:

- **Lead with the current behavior** in one plain sentence; put the rationale after it,
  not interleaved with it.
- **Ground every guard/fallback/special case in a concrete trigger**: name the input,
  code path, or test that reaches it (`Sub.Inner where class Sub extends Outer<String>`),
  not just the abstract condition. If you cannot name one, that is a signal the code may
  be unnecessary — see Simplification Discipline.
- **Prefer short declarative sentences** over long noun phrases and nested subordinate
  clauses. One idea per sentence.
- **No contrast with the unimplemented**: describe what the code does, not what it does
  instead of some alternative.
- Module ReadMe files follow the repo's human baseline: a few dozen lines saying what the
  module is and where the details live — not a full technical specification.

---

## Simplification & Review Discipline

Distilled from repeated push-collapse-push review cycles: complexity was defended, the
user pushed back two or three times, and the simplification landed anyway. The goal is to
reach the simplified end state in one pass, and to catch small gaps before review does.

- **Default to one generic path.** When the same operation is implemented separately per
  representation/origin (source vs. binary vs. Kotlin, same-file vs. cross-file), treat
  that split as a hypothesis to disprove: look for the one existing generic mechanism the
  specialized arms could route through.
- **Search for an existing helper before writing one.** Several review iterations were
  spent replacing hand-rolled code with a utility the same module already used
  (e.g. javac invocation → `JvmCompilationUtils.compileJavaFiles`). Grep for the
  operation's key ingredient first.
- **Check the peer implementations at every decision point.** When a reference
  implementation of the same behavior exists (e.g. PSI, javac, an upstream file), compare
  before and after designing — most "missed small details" found in review were places
  where a peer already handled the case (staticness breaks, case sensitivity, constant
  coercion).
- **"A unit test injects a fake here" is not a production justification.** If a
  parameter, overload, or lambda exists only for a test's convenience, change the test:
  write an end-to-end test against the real wiring, or add a narrow test double at the
  correct architectural boundary.
- **Parallel caches/lists/maps need a demonstrated distinct answer.** Two structures that
  differ only in a filter are one structure until a test shows the answers diverge.
- **Re-derive, don't recite.** Any claim that complexity is "necessary" must be
  re-verified against the current code on every review pass — trace the actual call sites
  and data flow again; earlier reasoning may be stale after intervening refactors.
- **Answer capability questions with evidence, not assumption.** "Is this tested?",
  "is this reachable?", "is this parameter used?" must be answered by grepping call sites
  or running the scenario. If no test demonstrates a claimed hazard, add one in the same
  pass or prefer the simpler design.
- **Treat a repeated "are we sure?" from the user as a cue to re-investigate from
  scratch**, with a concrete test or reachable call path as the outcome — not to restate
  the previous answer.
- **Healthy resistance is still expected — but only backed by evidence**: a concrete
  failing regression test or a specific reachable call path, produced within the same
  investigation pass. Otherwise implement the simplification.
- **Prefer collapsing multi-parameter overloads with exactly one production caller** into
  a single context-bound function, replacing the lost test flexibility with an
  integration test against the real path.

---

## Docs Maintenance

Keep the working doc set small — the module's instruction files and iteration log are read
into context every session.

- **The iteration log is append-only and short.** New entry on top, fixed fields
  (`Change` / `Files` / `Tests` / `Result`), ~15 lines / ~150 words per entry. Long
  rationale, traces, or measurement tables go into a dedicated `implDocs/<TOPIC>.md` and
  are linked, never inlined. No pasted logs/diffs.
- **Archive the log when it passes ~600 lines.** `git mv` it to
  `implDocs/archive/ITERATION_RESULTS_<last-entry-date>.md`, add an archive banner
  (Archive Date / Coverage / Result / staleness warning), then reset the live log to its
  template.
- **Archive an `implDocs/` doc once its work has fully landed or been superseded.**
  Move it to `implDocs/archive/`, add a banner, and repoint any references. Keep only
  living references and docs for *active* work outside the archive.
- **One fact, one doc.** When the same information (status, rule, file map) is needed in
  two documents, one owns it and the other links to it.

---

*Last updated: 2026-08-26 (extracted from `compiler/java-direct/AGENT_INSTRUCTIONS.md`;
comment and writing-style rules rewritten from a review of the branch's session logs —
recurring complaints: narrative/justificatory tone, counterfactual phrasing, stale comments
after refactors, non-standard terminology — and from a density/style comparison against
human-written compiler modules.)*
