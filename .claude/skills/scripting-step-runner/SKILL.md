---
name: scripting-step-runner
description: "Drive a scripting migration-plan step end-to-end: investigator → builder (per file) → reviewer → iteration entry. Stops on first test failure. Invoke manually with a step number `/scripting-step-runner 2` or after `/scripting-step <N>`."
disable-model-invocation: true
---

# Scripting Migration Step Runner

Drives one migration-plan step from `plugins/scripting/.ai/target/50-migration-plan.md` through the cavecrew pipeline. Use only when the step is fully specified (Touch list, Done-when clear).

## When to use

- A step from `target/50-migration-plan.md` is ready to execute (no blocking sequencing constraint).
- The user has reviewed the step's Touch list and approved scope.
- Tests are green at HEAD (baseline established).

Do not use for:
- Design tasks (use `/scripting-doc` or `Plan` agent).
- Steps blocked on a Q* not yet resolved in `target/90-open-questions.md`.
- Cross-step refactors (each step is independently mergeable — keep them so).

## Required state

- `SCRIPTING_TMP` set.
- `git status` clean OR all dirty files belong to this step's Touch list.
- The step's sequencing constraints (read tail of `target/50-migration-plan.md`) are satisfied.

## Procedure

### 1. Load step context

Run `/scripting-step <N>` first (or replicate inline): read `target/50-migration-plan.md`, extract the `### N.` block — Goal, Touch list, Done when, Design notes, sequencing constraints.

### 2. Baseline tests

For the suites tied to this step (per `scripting-iteration` skill's "Run the step's Gradle suites" mapping), run each once at HEAD, capture pass/fail counts to `$SCRIPTING_TMP/baseline_<suite>.txt`. Record as "Before" column in the iteration entry's Test Results table.

### 3. Investigator pass

Always spawn `caveman:cavecrew-investigator` with the step's Goal + Touch list verbatim. Ask it to locate every call-site of every file/symbol in Touch. Capture its file:line table to `$SCRIPTING_TMP/investigation.txt`. Read the table.

If the investigation surfaces files outside the Touch list, stop and ask the user — scope creep is not allowed without explicit re-scoping.

### 4. Builder passes (one per file)

For each file in the Touch list, spawn `caveman:cavecrew-builder` with:
- The step's Goal.
- The exact edit to perform (extracted from the step's body — be specific; do not hand-wave).
- The investigator's findings for cross-file impact.

Run builders **sequentially**, not in parallel — one file at a time. After each, run JetBrains MCP `get_file_problems` on the edited file with `errorsOnly=false`. Stop if any related error appears.

For deletions (steps 4/6/7/8/9/10/11): use `Bash` with `git rm` after the file is no longer referenced (per investigator).

### 5. Reviewer pass

Spawn `caveman:cavecrew-reviewer` on the full diff (`git diff`). Capture findings to `$SCRIPTING_TMP/review.txt`. Address every severity ≥ medium before proceeding. Low-severity findings can be deferred to the iteration's Key Learnings.

### 6. Run tests

Run the step's suites (per `scripting-iteration` skill mapping). Always `tee` to `$SCRIPTING_TMP/<suite>.txt`. One Bash call per suite.

If any suite FAILED that wasn't failing at baseline: **stop**. Do not attempt to fix in the same step — the step is too large. Open a sub-issue or surface to the user.

If all suites pass: record "After" column in the iteration entry's Test Results table.

### 7. Iteration entry

Invoke the `scripting-iteration` skill (step 1 onward) to gate the entry and walk the post-iteration checklist.

## Non-negotiable rules

- One step = one iteration entry. Do not bundle two steps into one entry — they are independently mergeable for a reason.
- Never run `-Pkotlin.test.update.test.data=true`.
- Never create a commit — the user reviews diffs first (Non-Negotiable Rule #8).
- Stop on first test regression; do not "fix while at it".
- Respect sequencing constraints. Step 6 requires steps 4+5 landed; step 8 requires no remaining K1 REPL callers (i.e. steps 5+7); etc.

## Caveats

- If the Touch list is empty or generic (e.g. "audit X"), this is a discovery step, not an execution step — use `Plan` agent instead.
- For step 13 (KT-82551 classpath discovery decision), refuse — that step is a design decision, not an execution.
- For step 11 (K1 frontend bindings), confirm whole-compiler K1 retirement is complete before starting — that's not scripting's call.
