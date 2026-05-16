---
name: scripting-iteration
description: "Gate scripting/REPL iteration entries: validate ITERATION_TEMPLATE field completeness, run the migration-step's Gradle suites, capture $SCRIPTING_TMP outputs, run the post-iteration checklist, archive the log when thresholds hit. Invoke when starting OR closing a scripting/REPL iteration."
disable-model-invocation: true
---

# Scripting/REPL Iteration Gate

Invoked manually (or via `/scripting-iter-close`) to keep iteration entries disciplined: full template, real test numbers, post-iteration checklist walked, archive cadence honored.

## When to use

- Closing a scripting/REPL iteration (migration-plan step landed).
- Recovering from a partially-filled `iterations/YYYY-MM-DD_*.md` entry — to backfill test results from `$SCRIPTING_TMP` or run the checklist after the fact.
- Auditing the iteration log for cadence breaches.

Do not use for general doc edits — use `/scripting-doc` instead.

## Required state

- `SCRIPTING_TMP` env var must be set; verify with `echo "$SCRIPTING_TMP"`. If empty, run `/scripting-iter-start` first.
- `plugins/scripting/.ai/ITERATION_TEMPLATE.md` exists (canonical template).
- The user has indicated which migration-plan step (or workstream) just landed.

## Procedure

### 1. Validate template completeness

Read `plugins/scripting/.ai/ITERATION_TEMPLATE.md`. Read the in-flight iteration file under `plugins/scripting/.ai/iterations/<DATE>_<slug>.md` (or create it). Every section must be non-empty:
- Overview
- Workstream / Issue (must reference KT-XXXXX or migration-plan step number)
- Changes (file-per-line with reasons)
- Test Results (table with real before/after numbers)
- Files Modified
- Key Learnings (non-trivial — "no learnings" is acceptable only if explicitly stated)
- **Resources & Cost** (run `.claude/scripts/iter-metrics.sh` and paste output; Loadout-vs-actual block manually filled)
- Post-iteration checklist (checkboxes)

If any section is empty, ask the user to fill it before proceeding. Specifically for Resources & Cost: if the script fails (no jq, no session JSONL accessible from `~/.claude/projects/...`), record "n/a — <reason>" in each table cell rather than leaving them blank.

The Resources & Cost section feeds [`PROCESS_AUDIT.md`](../../../plugins/scripting/.ai/PROCESS_AUDIT.md) — Section 2.9 (cost) and 2.10 (subagent mix) parse these per-iteration entries. Skipping the section silently blinds the periodic audit.

### 2. Run the step's Gradle suites

Read the corresponding `### N.` block from `plugins/scripting/.ai/target/50-migration-plan.md`. Extract the "Done when" criterion and pick the relevant suites from `AGENT_INSTRUCTIONS.md` Test Commands:

- Step 1 (JSR-223 bindings) → jvm-host-test + jsr223-test
- Step 2 (KT-83498) → jvm-host-test + fir2ir codegen + custom-script codegen
- Step 3 (stateless prototype) → jvm-host-test + jsr223-test
- Steps 4/5/6 (daemon REPL / `-Xrepl` / cli-base) → jvm-host-test + scripting-tests + LauncherScriptTest
- Step 7 (jvm-host legacy wrappers) → jvm-host-test
- Step 8 (K1 GenericReplCompiler + K1 registrar) → jvm-host-test + scripting-tests
- Steps 9/10 (ide-services / ide-common) → scripting-tests
- Step 11 (K1 frontend bindings) → fir2ir codegen + jvm-host-test + scripting-tests
- Step 12 (test cleanup) → fir2ir codegen + scripting-tests + LauncherScriptTest

Run each as a separate Bash call (one-token-per-call discipline). Always `tee` to `$SCRIPTING_TMP/<suite-tag>.txt`. Never use `&&`, `||`, `;`.

### 3. Capture results

For each suite, grep `FAILED` in the saved txt. Tabulate pass/fail counts. Update the Test Results table in the iteration entry with real numbers. Never invent numbers — if a suite didn't run, mark "n/a — reason".

### 4. Run the post-iteration checklist

Walk each item from `AGENT_INSTRUCTIONS.md` "Post-iteration checklist". Confirm with the user before each edit. Tick the checkbox in the iteration entry.

### 5. Archive cadence check

After appending the new index line to `ITERATION_RESULTS.md`, check:
- Entry count since last archive (count index lines without an archive pointer)
- File line count: `wc -l ITERATION_RESULTS.md`
- Days since last archive (parse "Archive YYYY-MM-DD" pointers; if none, use file creation date)

If 20+ entries OR 500+ lines OR 30+ days: run archive procedure:
1. `mkdir -p plugins/scripting/.ai/archive/iterations_<TODAY>`
2. Move oldest iteration files (keeping the latest 5) → `archive/iterations_<TODAY>/`
3. Replace their index lines with a single `- (Archive <TODAY>) → archive/iterations_<TODAY>/`

## Non-negotiable rules

- Never run `-Pkotlin.test.update.test.data=true` (Non-Negotiable Rule #9).
- Never create a commit (Non-Negotiable Rule #8).
- Never fabricate test results — empty `$SCRIPTING_TMP` means re-run suites, never guess.
- Never edit `compiler/.../gen/*` generated files.

## Caveats

- If a Gradle suite takes >5 minutes, prefer `run_in_background: true` and check completion via the Monitor tool — do not chain sleeps.
- If the user invoked this skill outside a scripting/REPL workstream, refuse and point to the iteration entry template.
