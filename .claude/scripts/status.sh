#!/usr/bin/env bash
# Status line for scripting/REPL K2-migration work.
# Reads Claude Code statusline payload (JSON) from stdin if available.
# Outputs one line with: model + session cost/effort, cache hit % + token breakdown + context fill,
# in-flight subagents, in-flight migration step, open Q-count, last-iter date, $SCRIPTING_TMP.
#
# Wire in .claude/settings.json:
#   "statusLine": { "type": "command", "command": "/Users/ich-jb/Work/kotlin/ws/scripting/.claude/scripts/status.sh" }
#
# Manual test (no payload):
#   ./status.sh
# Manual test (with mock payload):
#   echo '{"model":{"id":"claude-sonnet-4-6","display_name":"Sonnet 4.6"},"transcript_path":"/path/to/session.jsonl","context_window":{"total_input_tokens":55000,"context_window_size":200000,"current_usage":{"input_tokens":3000,"cache_creation_input_tokens":5000,"cache_read_input_tokens":40000},"used_percentage":27,"remaining_percentage":73}}' | ./status.sh

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
model_id=$(jq_get '.model.id')
output_tokens=$(jq_get '.context_window.current_usage.output_tokens')
cache_read=$(jq_get '.context_window.current_usage.cache_read_input_tokens')
cache_creation=$(jq_get '.context_window.current_usage.cache_creation_input_tokens')
input_tokens=$(jq_get '.context_window.current_usage.input_tokens')
ctx_window_size=$(jq_get '.context_window.context_window_size')
ctx_used_pct_prebuilt=$(jq_get '.context_window.used_percentage')
transcript_path=$(jq_get '.transcript_path')

# Cache hit ratio: cache_read / (cache_read + cache_creation + input).
# All three are "input"-side tokens — sum gives the cache-relevant input total.
cache_pct=""
cache_read_k=""
cache_creation_k=""
ctx_total_k=""
ctx_pct=""
if [ -n "$cache_read" ] && [ -n "$cache_creation" ] && command -v awk >/dev/null 2>&1; then
  # Token breakdown in K (thousands).
  cache_read_k=$(awk -v r="$cache_read" 'BEGIN { printf "%.0f", r / 1000 }')
  cache_creation_k=$(awk -v c="$cache_creation" 'BEGIN { printf "%.0f", c / 1000 }')

  # Cache hit %: read / all input-side tokens (treat missing input_tokens as 0).
  cache_pct=$(awk -v r="$cache_read" -v c="$cache_creation" -v i="${input_tokens:-0}" \
    'BEGIN { total = r + c + i; if (total > 0) printf "%.0f", (r * 100.0) / total }')

  # Context window fill — use pre-calculated field when present.
  if [ -n "$ctx_used_pct_prebuilt" ]; then
    ctx_pct=$(printf "%.0f" "$ctx_used_pct_prebuilt" 2>/dev/null || echo "$ctx_used_pct_prebuilt")
  else
    win="${ctx_window_size:-200000}"
    ctx_pct=$(awk -v r="$cache_read" -v c="$cache_creation" -v i="${input_tokens:-0}" -v w="$win" \
      'BEGIN { if (w > 0) printf "%.0f", ((r + c + i) / w) * 100 }')
  fi

  # Total context in K: cached prefix + non-cached input.
  ctx_total_k=$(awk -v r="$cache_read" -v c="$cache_creation" -v i="${input_tokens:-0}" \
    'BEGIN { printf "%.0f", (r + c + i) / 1000 }')
fi

# ---------- 2a. Cost calculation from tokens ----------

cost_line=""
if [ -n "$model_id" ] && command -v awk >/dev/null 2>&1; then
  # Pricing per 1M tokens (in/out) + cache read discount (10% of input rate).
  case "$model_id" in
    *haiku*) in_rate=0.80; out_rate=4.00; cache_disc_rate=0.08 ;;
    *sonnet*) in_rate=3.00; out_rate=15.00; cache_disc_rate=0.30 ;;
    *opus*) in_rate=5.00; out_rate=25.00; cache_disc_rate=0.50 ;;
    *) in_rate=0; out_rate=0; cache_disc_rate=0 ;;
  esac

  if [ "$in_rate" != "0" ]; then
    # Cost calculation: (cache_read @ 10% discount) + (cache_creation @ full rate) + (input @ full rate) + (output @ full rate).
    cost_usd=$(awk -v cr="$cache_read" -v cc="$cache_creation" -v i="${input_tokens:-0}" -v o="${output_tokens:-0}" \
      -v in_r="$in_rate" -v out_r="$out_rate" -v cache_r="$cache_disc_rate" \
      'BEGIN {
        cache_cost = (cr * cache_r) / 1000000
        creation_cost = (cc * in_r) / 1000000
        input_cost = (i * in_r) / 1000000
        output_cost = (o * out_r) / 1000000
        total = cache_cost + creation_cost + input_cost + output_cost
        printf "%.2f", total
      }')

    # Format cost (hide if <$0.01).
    if [ -n "$cost_usd" ] && [ "${cost_usd%.*}" != "0" ] || [ "${cost_usd#*.}" != "00" ]; then
      cost_line="💰 \$$cost_usd"
    fi
  fi
fi

# ---------- 2b. Wall-clock time from transcript timestamps ----------

effort_line=""
if [ -n "$transcript_path" ] && [ -f "$transcript_path" ] && command -v jq >/dev/null 2>&1; then
  # Extract first and last message timestamps (ISO 8601 format).
  first_ts=$(jq -r '. as $arr | $arr[0]?.ts? // $arr[0]?.timestamp? // empty' "$transcript_path" 2>/dev/null | head -1 || true)
  last_ts=$(jq -r '. as $arr | $arr[-1]?.ts? // $arr[-1]?.timestamp? // empty' "$transcript_path" 2>/dev/null | tail -1 || true)

  if [ -n "$first_ts" ] && [ -n "$last_ts" ] && command -v date >/dev/null 2>&1; then
    # Convert ISO 8601 to Unix timestamp (handle both 'Z' and '+00:00' formats).
    first_unix=$(date -f "%Y-%m-%dT%H:%M:%S" -j "$first_ts" 2>/dev/null || date -d "$first_ts" +%s 2>/dev/null || echo 0)
    last_unix=$(date -f "%Y-%m-%dT%H:%M:%S" -j "$last_ts" 2>/dev/null || date -d "$last_ts" +%s 2>/dev/null || echo 0)

    if [ "$first_unix" != "0" ] && [ "$last_unix" != "0" ] && [ "$last_unix" -gt "$first_unix" ]; then
      elapsed=$((last_unix - first_unix))
      if [ "$elapsed" -ge 3600 ]; then
        hours=$((elapsed / 3600))
        mins=$(( (elapsed % 3600) / 60 ))
        effort_line="⏱️ ${hours}h ${mins}m"
      elif [ "$elapsed" -ge 60 ]; then
        mins=$((elapsed / 60))
        secs=$((elapsed % 60))
        effort_line="⏱️ ${mins}m ${secs}s"
      elif [ "$elapsed" -ge 1 ]; then
        effort_line="⏱️ ${elapsed}s"
      fi
    fi
  fi

  # Extended thinking flag (Opus only; detect thinking blocks in transcript).
  if echo "$model_id" | grep -q "opus"; then
    has_thinking=$(jq -r '. | (.message.content // [])[]? | select(.type == "thinking") | .thinking' "$transcript_path" 2>/dev/null | head -1 || true)
    if [ -n "$has_thinking" ]; then
      effort_line="${effort_line:+$effort_line | }🧠 thinking"
    fi
  fi
fi

# ---------- 3. In-flight subagent count + completed subagent model breakdown ----------

# In-flight: Agent tool_use blocks with no matching tool_result yet.
# Completed models: map subagent_type to h/s/o (haiku/sonnet/opus), count by model.
subagents_inflight=0
subagent_models=""
if [ -n "$transcript_path" ] && [ -f "$transcript_path" ] && command -v jq >/dev/null 2>&1; then
  # Build sets of Agent tool_use ids and completed result ids.
  agent_ids=$(jq -r '. | (.message.content // [])[]? | select(.type=="tool_use" and .name=="Agent") | .id' "$transcript_path" 2>/dev/null | sort -u || true)
  result_ids=$(jq -r '. | (.message.content // [])[]? | select(.type=="tool_result") | .tool_use_id' "$transcript_path" 2>/dev/null | sort -u || true)

  if [ -n "$agent_ids" ]; then
    subagents_inflight=$(comm -23 <(echo "$agent_ids") <(echo "$result_ids") | grep -c . || echo 0)
  fi

  # Extract subagent_type from completed Agent tool_uses; map to model shorthand.
  completed_types=$(jq -s -r '
    map((.message.content // [])[]? | select(.type == "tool_use" and .name == "Agent")) as $agents |
    map((.message.content // [])[]? | select(.type == "tool_result") | .tool_use_id) as $completed_ids |
    $agents[] | select(.id as $id | $completed_ids | any(. == $id)) | (.input.subagent_type // "unknown")
  ' "$transcript_path" 2>/dev/null | sort || echo "")

  if [ -n "$completed_types" ]; then
    h_count=0; s_count=0; o_count=0
    while IFS= read -r atype; do
      if [ "$atype" = "cavecrew-investigator" ]; then
        h_count=$((h_count + 1))
      elif [ "$atype" = "Plan" ]; then
        o_count=$((o_count + 1))
      else
        s_count=$((s_count + 1))
      fi
    done <<< "$completed_types"

    # Format as "h1/s2/o1", omitting zero counts.
    parts_agents=()
    [ "$h_count" -gt 0 ] && parts_agents+=("h${h_count}")
    [ "$s_count" -gt 0 ] && parts_agents+=("s${s_count}")
    [ "$o_count" -gt 0 ] && parts_agents+=("o${o_count}")

    if [ ${#parts_agents[@]} -gt 0 ]; then
      subagent_models="$(IFS=/; echo "${parts_agents[*]}")"
    fi
  fi
fi

# ---------- 2c. ccusage (optional) — overrides calculated cost if installed ----------

ccusage_line=""
if command -v ccusage >/dev/null 2>&1 && [ -n "$payload" ]; then
  # ccusage statusline reads the same payload; pass it through (takes precedence).
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

# SCRIPTING_TMP — read from persistent file (set by SessionStart hook), env var, or fallback search.
tmp=""
# 1. Check persistent file written by SessionStart hook.
if [ -f "$HOME/.claude/scripting_tmp" ]; then
  tmp=$(cat "$HOME/.claude/scripting_tmp" 2>/dev/null | head -1 | tr -d '\n\r')
fi
# 2. Fallback to env var if file not available.
if [ -z "$tmp" ]; then
  tmp="${SCRIPTING_TMP}"
fi
# 3. Fallback: find most recent /tmp/scr_* directory.
if [ -z "$tmp" ]; then
  tmp=$(find /tmp -maxdepth 1 -type d -name 'scr_*' -printf '%T@ %p\n' 2>/dev/null | sort -rn | head -1 | awk '{print $2}' || true)
fi
# 4. Show "unset" if all fallbacks failed.
[ -z "$tmp" ] && tmp="unset"

# ---------- 5. Format output ----------

parts=()

# Model + cost + effort column.
if [ -n "$ccusage_line" ]; then
  parts+=("$ccusage_line")
else
  model_col="🤖 $model"
  [ -n "$cost_line" ] && model_col="$model_col | $cost_line"
  [ -n "$effort_line" ] && model_col="$model_col | $effort_line"
  parts+=("$model_col")
fi

# Cache hit column with token breakdown and context window fill.
[ -n "$cache_pct" ] && parts+=("📦 ${cache_pct}% hit (${cache_creation_k}k↑ ${cache_read_k}k↓) ctx: ${ctx_total_k}k/200k (${ctx_pct}%)")

# Subagents column: in-flight count + completed model breakdown (h/s/o).
if [ -n "$subagent_models" ]; then
  if [ "$subagents_inflight" -gt 0 ]; then
    parts+=("🛰 in: ${subagents_inflight} | models: ${subagent_models}")
  else
    parts+=("🛰 agents: ${subagent_models}")
  fi
elif [ "$subagents_inflight" -gt 0 ]; then
  parts+=("🛰 in: ${subagents_inflight}")
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
