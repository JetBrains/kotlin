#!/usr/bin/env bash
# Status line for scripting/REPL K2-migration work.
# Reads Claude Code statusline payload (JSON) from stdin if available.
# Outputs one line with: model + session cost, cache hit % + context fill,
# in-flight/done subagents, migration step, open Q-count, last-iter date, $SCRIPTING_TMP.
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
  payload=$(cat || true)
fi

jq_get() {
  if [ -z "$payload" ] || ! command -v jq >/dev/null 2>&1; then
    return
  fi
  echo "$payload" | jq -r "$1 // empty" 2>/dev/null || true
}

model=$(jq_get '.model.display_name')
model_id=$(jq_get '.model.id')
cache_read=$(jq_get '.context_window.current_usage.cache_read_input_tokens')
cache_creation=$(jq_get '.context_window.current_usage.cache_creation_input_tokens')
input_tokens=$(jq_get '.context_window.current_usage.input_tokens')
ctx_window_size=$(jq_get '.context_window.context_window_size')
ctx_used_pct_prebuilt=$(jq_get '.context_window.used_percentage')
transcript_path=$(jq_get '.transcript_path')

# ---------- 2. Per-turn cache hit % + context fill (from payload — always current) ----------
# These are per-message values from the current context window state.
# Reliable for monitoring cache warmth and context pressure.
# Show 0% explicitly when cache is cold (after /compact or session start).

cache_pct="0"
cache_read_k="0"
cache_creation_k="0"
ctx_total_k="0"
ctx_pct="${ctx_used_pct_prebuilt:-0}"
ctx_window_k=$(awk -v w="${ctx_window_size:-200000}" 'BEGIN { printf "%.0f", w / 1000 }' 2>/dev/null || echo 200)

if [ -n "$cache_read" ] && [ -n "$cache_creation" ] && command -v awk >/dev/null 2>&1; then
  cache_read_k=$(awk -v r="$cache_read" 'BEGIN { printf "%.0f", r / 1000 }')
  cache_creation_k=$(awk -v c="$cache_creation" 'BEGIN { printf "%.0f", c / 1000 }')
  cache_pct=$(awk -v r="$cache_read" -v c="$cache_creation" -v i="${input_tokens:-0}" \
    'BEGIN { total = r + c + i; printf "%.0f", (total > 0 ? (r * 100.0) / total : 0) }')
  ctx_total_k=$(awk -v r="$cache_read" -v c="$cache_creation" -v i="${input_tokens:-0}" \
    'BEGIN { printf "%.0f", (r + c + i) / 1000 }')
  if [ -z "$ctx_used_pct_prebuilt" ]; then
    win="${ctx_window_size:-200000}"
    ctx_pct=$(awk -v r="$cache_read" -v c="$cache_creation" -v i="${input_tokens:-0}" -v w="$win" \
      'BEGIN { printf "%.0f", (w > 0 ? ((r + c + i) / w) * 100 : 0) }')
  fi
fi

cache_col="📦 ${cache_pct}% hit (${cache_creation_k}k↑ ${cache_read_k}k↓) ctx: ${ctx_pct}% (${ctx_total_k}k/${ctx_window_k}k)"

# ---------- 3. Session cost from JSONL (accurate — both input+output from transcript) ----------
# Cached by transcript file size: no re-parse when nothing new was written (every keystroke).
# Computes true session totals by summing all assistant messages in the transcript.

session_cost_usd=""
if [ -n "$transcript_path" ] && [ -f "$transcript_path" ] && command -v jq >/dev/null 2>&1 && command -v awk >/dev/null 2>&1; then
  session_id=$(basename "$transcript_path" .jsonl)
  COST_CACHE="/tmp/claude_cost_${session_id}"

  current_size=$(wc -c < "$transcript_path" 2>/dev/null | tr -d ' \n' || echo 0)
  cached_size=$(head -1 "$COST_CACHE" 2>/dev/null | tr -d ' \n' || echo -1)

  if [ "$current_size" = "$cached_size" ] && [ -s "$COST_CACHE" ]; then
    session_cost_usd=$(sed -n '2p' "$COST_CACHE" 2>/dev/null | tr -d ' \n' || true)
  else
    # Determine pricing tier from model_id (default sonnet).
    case "${model_id:-sonnet}" in
      *haiku*)  in_r=0.80;  out_r=4.00;  cr_r=0.08;  cc_r=1.00 ;;
      *opus*)   in_r=15.00; out_r=75.00; cr_r=1.50;  cc_r=18.75 ;;
      *)        in_r=3.00;  out_r=15.00; cr_r=0.30;  cc_r=3.75 ;;  # sonnet
    esac

    session_cost_usd=$(jq -r 'select(.type == "assistant") |
      "\(.message.usage.input_tokens // 0) \(.message.usage.cache_creation_input_tokens // 0) \(.message.usage.cache_read_input_tokens // 0) \(.message.usage.output_tokens // 0)"' \
      "$transcript_path" 2>/dev/null | \
      awk -v in_r="$in_r" -v out_r="$out_r" -v cr_r="$cr_r" -v cc_r="$cc_r" \
        '{ i+=$1; cc+=$2; cr+=$3; o+=$4 }
         END { printf "%.2f", (i*in_r + cc*cc_r + cr*cr_r + o*out_r) / 1000000 }' || echo "")

    if [ -n "$session_cost_usd" ]; then
      printf '%s\n%s\n' "$current_size" "$session_cost_usd" > "$COST_CACHE" 2>/dev/null || true
    fi
  fi
fi

cost_col=""
if [ -n "$session_cost_usd" ] && [ "$session_cost_usd" != "0.00" ]; then
  cost_col="💰 \$$session_cost_usd"
fi

# ---------- 4. Wall-clock elapsed time from transcript timestamps ----------

effort_line=""
if [ -n "$transcript_path" ] && [ -f "$transcript_path" ] && command -v jq >/dev/null 2>&1; then
  first_ts=$(jq -r '.timestamp // empty' "$transcript_path" 2>/dev/null | grep -v '^$' | head -1 || true)
  last_ts=$(jq  -r '.timestamp // empty' "$transcript_path" 2>/dev/null | grep -v '^$' | tail -1 || true)

  if [ -n "$first_ts" ] && [ -n "$last_ts" ] && command -v date >/dev/null 2>&1; then
    first_ts_s=$(echo "$first_ts" | sed 's/\.[0-9]*Z$//' | sed 's/Z$//')
    last_ts_s=$(echo  "$last_ts"  | sed 's/\.[0-9]*Z$//' | sed 's/Z$//')
    first_unix=$(date -j -f "%Y-%m-%dT%H:%M:%S" "$first_ts_s" +%s 2>/dev/null || date -d "$first_ts" +%s 2>/dev/null || echo 0)
    last_unix=$(date  -j -f "%Y-%m-%dT%H:%M:%S" "$last_ts_s"  +%s 2>/dev/null || date -d "$last_ts"  +%s 2>/dev/null || echo 0)

    if [ "$first_unix" != "0" ] && [ "$last_unix" != "0" ] && [ "$last_unix" -gt "$first_unix" ]; then
      elapsed=$((last_unix - first_unix))
      if [ "$elapsed" -ge 3600 ]; then
        effort_line="⏱ $((elapsed/3600))h$(( (elapsed%3600)/60 ))m"
      elif [ "$elapsed" -ge 60 ]; then
        effort_line="⏱ $((elapsed/60))m"
      fi
    fi
  fi
fi

# ---------- 5. Subagent tracking — readable type names, clear in-flight count ----------
# Maps Agent subagent_type to short names: cavecrew-investigator→inv, cavecrew-builder→bld,
# cavecrew-reviewer→rev, Plan→plan, Explore→exp, others→agt.
# In-flight shown as count+⏳; done shown as type list with ✓ count.

subagent_col=""
if [ -n "$transcript_path" ] && [ -f "$transcript_path" ] && command -v jq >/dev/null 2>&1; then
  # All Agent tool_use ids and completed result ids.
  agent_ids=$(jq -r 'select(.type == "assistant") | (.message.content // [])[] | select(.type=="tool_use" and .name=="Agent") | .id' \
    "$transcript_path" 2>/dev/null | sort -u || true)
  result_ids=$(jq -r 'select(.type == "user") | (.message.content // [])[] | select(.type=="tool_result") | .tool_use_id' \
    "$transcript_path" 2>/dev/null | sort -u || true)

  inflight=0
  if [ -n "$agent_ids" ]; then
    inflight=$(comm -23 <(printf '%s\n' "$agent_ids" | sort -u) <(printf '%s\n' "$result_ids" | sort -u) 2>/dev/null | wc -l | tr -d ' ' || echo 0)
  fi

  # Completed agent type names (all Agent tool_use types from transcript).
  done_col=""
  if [ -n "$agent_ids" ]; then
    completed_types=$(jq -r 'select(.type == "assistant") | (.message.content // [])[] | select(.type=="tool_use" and .name=="Agent") | (.input.subagent_type // "agt")' \
      "$transcript_path" 2>/dev/null | head -20 || true)

    if [ -n "$completed_types" ]; then
      inv=0; bld=0; rev=0; plan=0; exp=0; other=0
      while IFS= read -r t; do
        case "$t" in
          *investigator*) inv=$((inv+1)) ;;
          *builder*)      bld=$((bld+1)) ;;
          *reviewer*)     rev=$((rev+1)) ;;
          Plan|plan)      plan=$((plan+1)) ;;
          Explore|explore) exp=$((exp+1)) ;;
          *) other=$((other+1)) ;;
        esac
      done <<< "$completed_types"

      parts_done=()
      [ "$inv"   -gt 0 ] && parts_done+=("inv×${inv}")
      [ "$bld"   -gt 0 ] && parts_done+=("bld×${bld}")
      [ "$rev"   -gt 0 ] && parts_done+=("rev×${rev}")
      [ "$plan"  -gt 0 ] && parts_done+=("plan×${plan}")
      [ "$exp"   -gt 0 ] && parts_done+=("exp×${exp}")
      [ "$other" -gt 0 ] && parts_done+=("agt×${other}")
      [ "${#parts_done[@]}" -gt 0 ] && done_col="✓ $(IFS=' '; echo "${parts_done[*]}")"
    fi
  fi

  if [ "$inflight" -gt 0 ] && [ -n "$done_col" ]; then
    subagent_col="🛰 ${inflight}⏳ ${done_col}"
  elif [ "$inflight" -gt 0 ]; then
    subagent_col="🛰 ${inflight}⏳"
  elif [ -n "$done_col" ]; then
    subagent_col="🛰 ${done_col}"
  fi
fi

# ---------- 6. Scripting/REPL project context ----------

step="done"
if [ -f "$AI_DIR/target/50-migration-plan.md" ]; then
  step_line=$(grep -E '^### [0-9]+\.' "$AI_DIR/target/50-migration-plan.md" 2>/dev/null | grep -v '~~' | head -1 || true)
  [ -n "$step_line" ] && step=$(echo "$step_line" | sed -E 's/^### //; s/ —.*//' | head -c 40)
fi

open_q=0
if [ -f "$AI_DIR/target/90-open-questions.md" ]; then
  open_q=$(grep -cE '^- Status: (open|in-design)' "$AI_DIR/target/90-open-questions.md" 2>/dev/null || echo 0)
fi

last_iter="—"
if [ -f "$AI_DIR/ITERATION_RESULTS.md" ]; then
  found=$(grep -oE '20[0-9]{2}-[0-9]{2}-[0-9]{2}' "$AI_DIR/ITERATION_RESULTS.md" 2>/dev/null | sort -r | head -1 || true)
  [ -n "$found" ] && last_iter="$found"
fi

tmp=""
[ -f "$HOME/.claude/scripting_tmp" ] && tmp=$(head -1 "$HOME/.claude/scripting_tmp" 2>/dev/null | tr -d '\n\r')
[ -z "$tmp" ] && tmp="${SCRIPTING_TMP}"
[ -z "$tmp" ] && tmp=$(find /tmp -maxdepth 1 -type d -name 'scr_*' 2>/dev/null | sort | tail -1 || true)
[ -z "$tmp" ] && tmp="unset"

# ---------- 7. Assemble output ----------

parts=()

# Model column.
model_col="${model:-unknown}"
[ -n "$cost_col" ]    && model_col="$model_col | $cost_col"
[ -n "$effort_line" ] && model_col="$model_col | $effort_line"
parts+=("🤖 $model_col")

# Cache + context column (always shown, shows 0% when cold).
parts+=("$cache_col")

# Subagents column (only when there were agents this session).
[ -n "$subagent_col" ] && parts+=("$subagent_col")

# Project context (always shown).
parts+=("🔧 step: $step")
parts+=("❓ Q: $open_q")
parts+=("📅 $last_iter")
parts+=("📁 $(basename "$tmp")")

out=""
for p in "${parts[@]}"; do
  out="${out:+$out | }$p"
done
echo "$out"
