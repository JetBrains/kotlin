---
description: Load only the relevant Q-block from target/90-open-questions.md (Q1..Q12, sub-questions Q5a..Q10f). Usage `/scripting-q 10` or `/scripting-q 5a`.
allowed-tools: Read, Grep
argument-hint: <q-id>
---

# Load open-question $ARGUMENTS

## Procedure

1. Read `/Users/ich-jb/Work/kotlin/ws/scripting/plugins/scripting/.ai/target/90-open-questions.md`.
2. Locate the `## Q$ARGUMENTS.` heading (or sub-row `| Q5a |` / `| Q10a |` for sub-questions).
3. Print:
   - The triage fields (Status / Owner / YT / Target doc / Last touched).
   - The body.
   - Cross-reference: the target-doc link from the Q's field block. Suggest loading that doc next if the user wants design context.
4. If the Q is `resolved`, note it and point to the landing iteration link (if present).

## Caveats

- Do not auto-load the target-doc — only suggest. Cache discipline (per AGENT_INSTRUCTIONS section "Caching strategy"): keep the loaded set minimal.
- If `$ARGUMENTS` does not match a Q-id, print the list of Q-ids and statuses from the doc; do not guess.
