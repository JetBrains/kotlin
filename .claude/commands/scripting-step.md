---
description: Load a scripting migration-plan step (N) and its Touch files. Usage `/scripting-step 2`.
allowed-tools: Read, Bash, Grep
argument-hint: <step-number>
---

# Load scripting migration-plan step $1

Step number from `plugins/scripting/.ai/target/50-migration-plan.md`. Steps 1–14 are valid.

## Procedure

1. Read `/Users/ich-jb/Work/kotlin/ws/scripting/plugins/scripting/.ai/AGENT_INSTRUCTIONS.md` (stable prefix).
2. Read `/Users/ich-jb/Work/kotlin/ws/scripting/plugins/scripting/.ai/target/50-migration-plan.md` and locate `### $1.` heading. Extract:
   - Goal block
   - Touch list (file paths)
   - Done-when criterion
   - Design notes (if present)
   - Sequencing constraints affecting this step (read tail "## Sequencing constraints").
3. For each path in the Touch list, if it points to an existing repo file, read it. If it's a glob/pattern, skip — note it for the operator.
4. Print the loadout-matrix row for **Migration-step execution** (from `AGENT_INSTRUCTIONS.md` "Per-Task Agent Loadout") — budget, model, subagent guidance.
5. State the cavecrew dispatch decision: if Touch list >1 module, recommend `cavecrew-investigator` first; otherwise `cavecrew-builder` per file.

## Caveats

- If `$1` is not provided or out of range 1–14, print available step titles only — do not guess.
- Do not modify anything; this command only loads context.
