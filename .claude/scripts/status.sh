#!/usr/bin/env bash
# Status line for scripting/REPL K2-migration work.
# Outputs a single line: [in-flight step] | open Q-count | last-iter date | $SCRIPTING_TMP
# Wire in settings.json:
#   "statusLine": { "type": "command", "command": "/Users/ich-jb/Work/kotlin/ws/scripting/.claude/scripts/status.sh" }

set -e
AI_DIR="/Users/ich-jb/Work/kotlin/ws/scripting/plugins/scripting/.ai"

# In-flight migration step: first ### N. heading WITHOUT a strikethrough.
step_line=$(grep -E '^### [0-9]+\.' "$AI_DIR/target/50-migration-plan.md" 2>/dev/null | grep -v '~~' | head -1 || true)
if [[ -n "$step_line" ]]; then
  # Extract "N. Title"
  step=$(echo "$step_line" | sed -E 's/^### //; s/ —.*//' | head -c 40)
else
  step="done"
fi

# Open Q count: status: open or in-design.
open_q=$(grep -cE '^- Status: (open|in-design)' "$AI_DIR/target/90-open-questions.md" 2>/dev/null || echo 0)

# Last iteration date: latest index line under "## Iteration index".
last_iter=$(grep -E '^- 20[0-9]{2}-[0-9]{2}-[0-9]{2}' "$AI_DIR/ITERATION_RESULTS.md" 2>/dev/null | head -1 | grep -oE '20[0-9]{2}-[0-9]{2}-[0-9]{2}' | head -1 || true)
[[ -z "$last_iter" ]] && last_iter="—"

# SCRIPTING_TMP
tmp="${SCRIPTING_TMP:-unset}"

echo "🔧 step: $step | open-Q: $open_q | last-iter: $last_iter | tmp: $tmp"
