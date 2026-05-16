#!/usr/bin/env bash
# Status line for scripting/REPL K2-migration work.
# Reads Claude Code statusline payload (JSON) from stdin if available.
# Outputs one line with: model, session cost, cache hit %, in-flight subagents,
# in-flight migration step, open Q-count, last-iter date, $SCRIPTING_TMP.
#
# Wire in .claude/settings.json:
#   "statusLine": { "type": "command", "command": "/Users/ich-jb/Work/kotlin/ws/scripting/.claude/scripts/status.sh" }
#
# Manual test (no payload):
#   ./status.sh
# Manual test (with mock payload):
#   echo '{"model":{"display_name":"Sonnet 4.6"},"cost":{"total_cost_usd":0.12,"total_cache_read_input_tokens":15000,"total_cache_creation_input_tokens":5000},"transcript_path":"/path/to/session.jsonl"}' | ./status.sh

set -e
AI_DIR="/Users/ich-jb/Work/kotlin/ws/scripting/plugins/scripting/.ai"

# ---------- 1. Read statusline payload (if any) ----------

payload=""
if [ ! -t 0 ]; then
  # stdin is a pipe — read it (Claude Code sends a single JSON object, no streaming).
  payload=$(cat || true)
fi

jq_get() {
  # Usage: jq_get '.path.to.field'  → echoes value or empty string.
  if [ -z "$payload" ] || ! command -v jq >/dev/null 2>&1; then
    return
  fi
  echo "$payload" | jq -r "$1 // empty" 2>/dev/null || true
}

model=$(jq_get '.model.display_name')
cost_usd=$(jq_get '.cost.total_cost_usd')
cache_read=$(jq_get '.cost.total_cache_read_input_tokens')
cache_creation=$(jq_get '.cost.total_cache_creation_input_tokens')
input_tokens=$(jq_get '.cost.total_input_tokens')
transcript_path=$(jq_get '.transcript_path')

# Cache hit ratio: cache_read / (cache_read + cache_creation + input).
# All three are "input"-side tokens — sum gives the cache-relevant input total.
cache_pct=""
if [ -n "$cache_read" ] && [ -n "$cache_creation" ] && command -v awk >/dev/null 2>&1; then
  # Treat missing input_tokens as 0.
  cache_pct=$(awk -v r="$cache_read" -v c="$cache_creation" -v i="${input_tokens:-0}" \
    'BEGIN { total = r + c + i; if (total > 0) printf "%.0f", (r * 100.0) / total }')
fi

# ---------- 2. In-flight subagent count (parse transcript JSONL) ----------

# Counts Task tool_use blocks whose tool_use_id has no matching tool_result yet.
# Cheap heuristic. If statusline refreshes mid-subagent-call, this goes up by 1.
subagents_inflight=""
if [ -n "$transcript_path" ] && [ -f "$transcript_path" ] && command -v jq >/dev/null 2>&1; then
  # Build set of Task tool_use ids and set of tool_result ids, then diff.
  # NOTE: relies on JSONL where each line is a message with .message.content[].
  task_ids=$(jq -r '. | (.message.content // [])[]? | select(.type=="tool_use" and .name=="Task") | .id' "$transcript_path" 2>/dev/null | sort -u || true)
  result_ids=$(jq -r '. | (.message.content // [])[]? | select(.type=="tool_result") | .tool_use_id' "$transcript_path" 2>/dev/null | sort -u || true)
  if [ -n "$task_ids" ]; then
    subagents_inflight=$(comm -23 <(echo "$task_ids") <(echo "$result_ids") | grep -c . || echo 0)
  else
    subagents_inflight=0
  fi
fi

# ---------- 3. ccusage (optional) — overrides cost if installed ----------

ccusage_line=""
if command -v ccusage >/dev/null 2>&1 && [ -n "$payload" ]; then
  # ccusage statusline reads the same payload; pass it through.
  ccusage_line=$(echo "$payload" | ccusage statusline 2>/dev/null || true)
fi

# ---------- 4. Scripting/REPL context ----------

# In-flight migration step: first ### N. heading WITHOUT a strikethrough.
step="done"
if [ -f "$AI_DIR/target/50-migration-plan.md" ]; then
  step_line=$(grep -E '^### [0-9]+\.' "$AI_DIR/target/50-migration-plan.md" 2>/dev/null | grep -v '~~' | head -1 || true)
  if [ -n "$step_line" ]; then
    step=$(echo "$step_line" | sed -E 's/^### //; s/ —.*//' | head -c 40)
  fi
fi

# Open Q count: status: open or in-design.
open_q=0
if [ -f "$AI_DIR/target/90-open-questions.md" ]; then
  open_q=$(grep -cE '^- Status: (open|in-design)' "$AI_DIR/target/90-open-questions.md" 2>/dev/null || echo 0)
fi

# Last iteration date: latest index line under "## Iteration index".
last_iter="—"
if [ -f "$AI_DIR/ITERATION_RESULTS.md" ]; then
  found=$(grep -E '^- 20[0-9]{2}-[0-9]{2}-[0-9]{2}' "$AI_DIR/ITERATION_RESULTS.md" 2>/dev/null | head -1 | grep -oE '20[0-9]{2}-[0-9]{2}-[0-9]{2}' | head -1 || true)
  [ -n "$found" ] && last_iter="$found"
fi

# SCRIPTING_TMP
tmp="${SCRIPTING_TMP:-unset}"

# ---------- 5. Format output ----------

parts=()

# Cost/model column.
if [ -n "$ccusage_line" ]; then
  parts+=("$ccusage_line")
else
  if [ -n "$model" ]; then
    if [ -n "$cost_usd" ]; then
      parts+=("🤖 $model \$$(printf '%.2f' "$cost_usd" 2>/dev/null || echo "$cost_usd")")
    else
      parts+=("🤖 $model")
    fi
  fi
fi

# Cache hit column.
[ -n "$cache_pct" ] && parts+=("📦 cache ${cache_pct}%")

# Subagents column.
if [ -n "$subagents_inflight" ] && [ "$subagents_inflight" -gt 0 ]; then
  parts+=("🛰 subs: $subagents_inflight")
fi

# Scripting context columns (always shown).
parts+=("🔧 step: $step")
parts+=("❓ open-Q: $open_q")
parts+=("📅 last-iter: $last_iter")
parts+=("📁 tmp: $tmp")

# Join with " | ".
out=""
for p in "${parts[@]}"; do
  if [ -z "$out" ]; then
    out="$p"
  else
    out="$out | $p"
  fi
done
echo "$out"
