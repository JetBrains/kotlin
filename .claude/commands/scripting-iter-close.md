---
description: Close a scripting/REPL iteration — create entry from template, run the post-iteration checklist. Usage `/scripting-iter-close <slug>`.
allowed-tools: Read, Write, Edit, Bash, AskUserQuestion
argument-hint: <short-slug>
---

# Close iteration — slug: $ARGUMENTS

## Procedure

1. Read `/Users/ich-jb/Work/kotlin/ws/scripting/plugins/scripting/.ai/ITERATION_TEMPLATE.md`.

2. Ask the user (via AskUserQuestion if multiple needed; otherwise inline) for:
   - Title (1 line)
   - Workstream / KT-id
   - Slug (if `$ARGUMENTS` empty)

3. Compute today's date: `$(date +%Y-%m-%d)`. Write filled iteration file to `/Users/ich-jb/Work/kotlin/ws/scripting/plugins/scripting/.ai/iterations/<DATE>_<slug>.md`. Required sections from the template — leave none empty. Test Results table must have actual numbers from `$SCRIPTING_TMP/*.txt` (read those files; do not invent numbers).

3a. **Resources & Cost section**: run `/Users/ich-jb/Work/kotlin/ws/scripting/.claude/scripts/iter-metrics.sh` (no args — auto-picks the most recent session). Paste the script's output as the "Resources & Cost" section of the iteration entry. Then fill the Loadout-vs-actual sub-block manually:
   - Which row from `AGENT_INSTRUCTIONS.md` Per-Task Agent Loadout matched this iteration?
   - Did actual model match the row's recommended model?
   - Did actual cost hit the row's budget (within ~30%) — over / hit / under?
   - Was subagent dispatch followed (cavecrew-investigator before cross-module work, etc.)?
   - If "no" / "over": one-line cause + intervention. Carries forward into PROCESS_AUDIT.

   If `iter-metrics.sh` fails (jq missing, no session JSONL accessible): record "n/a — <reason>" in the metrics table rather than leaving it empty.

4. Append one-line index entry to `/Users/ich-jb/Work/kotlin/ws/scripting/plugins/scripting/.ai/ITERATION_RESULTS.md` under the "## Iteration index" heading. Format:
   ```
   - <DATE> — [<Title>](iterations/<DATE>_<slug>.md) — <workstream / KT-id> — <one-line summary>
   ```
   Most recent on top. Do not rewrite earlier lines.

5. Walk the post-iteration checklist from `AGENT_INSTRUCTIONS.md` — for each item, confirm with the user before edit:
   - Strike landed migration-plan step (`### N. ~~Title~~ — landed <DATE>`)
   - Update Active Workstreams list
   - Update `current/90-legacy-inventory.md` disposition rows
   - Update `current/40-embedding-cli.md` / `current/45-embedding-daemon-legacy.md` / `current/70-tests.md` if surface changed
   - Flip resolved Q* in `target/90-open-questions.md` and link this iteration
   - Bump "Last verified" date in materially-changed docs

6. Check archive thresholds (20 entries OR 500 lines OR 30 days since last archive) — if hit, run the archive procedure from `ITERATION_RESULTS.md`.

## Caveats

- Do not auto-commit. The user always reviews diffs before any commit (Non-Negotiable Rule #8).
- If `$SCRIPTING_TMP` is empty or unset, prompt the user — do not fabricate test results.
- Do not run `-Pkotlin.test.update.test.data=true` (Non-Negotiable Rule #9).
