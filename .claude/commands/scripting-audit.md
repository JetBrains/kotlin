---
description: Walk the scripting/REPL process-audit playbook. Run periodically (~10 iterations / 4 weeks / on trigger). Produces a dated audit entry under iterations/.
allowed-tools: Read, Bash, Write, Edit, Grep
---

# Process audit — scripting/REPL agentic workflow

## Procedure

1. Read `/Users/ich-jb/Work/kotlin/ws/scripting/plugins/scripting/.ai/PROCESS_AUDIT.md` fully.
2. Walk Section 0 (snapshot) — set `AUDIT_TMP`, record git SHA.
3. Walk Section 1 (reading list) — load only the docs listed there; do NOT auto-load current/* or target/{00,10,20,30,40}-*.md unless a Section 2 metric flags them.
4. Walk Section 2 quantitative recipes — one Bash call per recipe. Save outputs under `$AUDIT_TMP/`. Do NOT skip recipes; if a recipe yields no output, note it as "0" or "n/a" in the audit entry.
5. Walk Section 3 qualitative review — pick 3 recent iterations as instructed. Answer each sub-question literally.
6. Walk Section 4 (interventions) — for every symptom found in Sections 2/3, choose an intervention. Surface to the user for approval before any edit.
7. Walk Section 5 — write the audit entry to `iterations/audit_$(date +%Y-%m-%d).md` using the provided template. Fill every field.
8. Walk Section 6 (after-audit) — apply each approved decision as a separate edit. Append the audit index line to `ITERATION_RESULTS.md`.

## Non-negotiable rules

- Do not skip a recipe to save time. Audit-driven decisions need full data.
- Do not invent metrics — if a recipe fails (e.g. ccusage missing), record "n/a, reason".
- Do not auto-commit (Rule #8). Surface every decision for user review.
- Do not delete prior `iterations/audit_*.md` entries — they're the historical record.

## Caveats

- Audit takes ~30–60 minutes. Avoid running unless a trigger from PROCESS_AUDIT.md Section "When to run" actually fired.
- macOS-specific `date -j -f` syntax is in some recipes; switch to `date -d` on Linux.
- If `~/.claude/projects/.../sessions/*.jsonl` paths don't resolve, the subagent-mix recipe (Section 2.10) skips silently — note in entry.
