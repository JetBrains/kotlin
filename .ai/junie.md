# Junie Setup Guide

> **Note:** This guide is for developers using JetBrains' Junie agent in this repo. AI agents should ignore this file.

## What Junie reads automatically

- `.junie/guidelines.md` — repo-level Junie entry point. Points at the canonical guidelines.
- `.junie/memory/*.md` — pre-loaded on session start. `.junie/memory/` is gitignored, so files there are local-only and opt-in per contributor. Recommended for scripting work: drop a `scripting.md` pre-load index (template content in `plugins/scripting/.ai/JUNIE_NOTES.md` and `.junie/guidelines.md`).
- The `<issue_description>` plus the user's selected files.

## Relationship to `.claude/`

The `.claude/` tree (settings, slash commands, status line, hooks, skills) is **Claude Code-specific**.
None of it executes under Junie:

- `SessionStart` / `PreToolUse` / `UserPromptSubmit` hooks — Junie has no hook system.
- `statusLine` / `iter-metrics.sh` — Junie emits no session JSONL in `~/.claude/projects/`.
- `cavecrew-investigator / -builder / -reviewer` subagents — Junie has no `Task` subagent surface.
- `mcp__jetbrains__*` tools — Junie uses its own native tool family.
- `/scripting-*` slash commands — invoke by plain prompt instead (procedural bodies are tool-agnostic and still apply).

The procedural markdown bodies in `.claude/commands/scripting-*.md` are portable — when a user says
"run the `scripting-iter-start` procedure" Junie can read the same body and execute the listed steps
using its native tools.

## How Junie maps to the scripting workstream

For tasks under `plugins/scripting/**`, the canonical source of truth is
`plugins/scripting/.ai/AGENT_INSTRUCTIONS.md`, with Junie-specific deltas in
`plugins/scripting/.ai/JUNIE_NOTES.md`. Read both before starting work.

Quick mapping:

| Claude primitive | Junie equivalent |
|---|---|
| `mcp__jetbrains__search_in_files_by_text` / `_by_regex` / `search_symbol` | `search_project` |
| `mcp__jetbrains__get_file_text_by_path` | `open` / `get_file_structure` |
| `mcp__jetbrains__replace_text_in_file` | `search_replace` / `multi_edit` |
| `mcp__jetbrains__rename_refactoring` | `rename_element` |
| `mcp__jetbrains__get_file_problems` | `build` / `run_test` (post-edit verification) |
| `cavecrew-investigator` role | `[ADVANCED_CHAT]` or read-only start of `[CODE]` mode |
| `cavecrew-builder` role | `[CODE]` / `[FAST_CODE]` mode with `search_replace` / `multi_edit` |
| `cavecrew-reviewer` role | Final self-review pass + `run_test` before `submit` |
| `$SCRIPTING_TMP` (env, persisted by `SessionStart` hook) | Deterministic in-tree path: `plugins/scripting/.ai/tmp/junie/$(date +%Y-%m-%d)` recomputed in every `bash` call |
| `/scripting-iter-start` etc. | Plain prompt: "execute the `scripting-iter-start` procedure" |

## Self-enforced rules (no hook backstop under Junie)

The PreToolUse hook in `.claude/settings.json` does NOT fire under Junie. The corresponding
rules from `AGENT_INSTRUCTIONS.md` must be self-enforced:

- **Never** run `git add` / `git commit` / `git push` (Non-Negotiable Rule #8).
- **Never** run `-Pkotlin.test.update.test.data=true` (Non-Negotiable Rule #9).
- **Always** `tee` Gradle output to a tmp file under the Junie tmp path above (Shell Discipline).

## Local Preferences

`.junie/local.md` (untracked) can be used to store local preferences if needed.
