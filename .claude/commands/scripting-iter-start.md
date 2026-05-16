---
description: Bootstrap a scripting/REPL iteration session. Exports SCRIPTING_TMP and loads stable prefix + recent iterations.
allowed-tools: Read, Bash
---

# Bootstrap scripting/REPL iteration session

## Procedure

1. Run (single Bash call):
   ```
   export SCRIPTING_TMP="/tmp/scr_$(date +%Y%m%d_%H%M%S)" && mkdir -p "$SCRIPTING_TMP" && echo "SCRIPTING_TMP=$SCRIPTING_TMP"
   ```
   Note the value — every Gradle invocation this session must `tee` to this dir per AGENT_INSTRUCTIONS Shell Discipline.

2. Read `/Users/ich-jb/Work/kotlin/ws/scripting/plugins/scripting/.ai/AGENT_INSTRUCTIONS.md` (stable cache prefix).

3. Read `/Users/ich-jb/Work/kotlin/ws/scripting/plugins/scripting/.ai/ITERATION_RESULTS.md` for the workstream state table + iteration index.

4. List `/Users/ich-jb/Work/kotlin/ws/scripting/plugins/scripting/.ai/iterations/` — read the latest 3 entries (if any) for context continuity.

5. Print:
   - Current `SCRIPTING_TMP` path.
   - Workstream state summary.
   - Last iteration date + slug (if any).
   - Recommended next loadout: ask the user which task type from the Per-Task Agent Loadout matrix they're starting; load matching docs only.

## Caveats

- Do not load every doc — defer task-specific docs until the user names the task.
- Do not run any Gradle suite at bootstrap.
