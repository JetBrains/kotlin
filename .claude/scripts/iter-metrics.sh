#!/usr/bin/env bash
# Extract iteration metrics (cost, cache, subagents, model mix) from a Claude Code
# session JSONL. Output is a markdown block ready to paste into the iteration entry
# under the "Resources & Cost" section.
#
# Usage:
#   iter-metrics.sh                                 # auto-pick most recent session for this repo
#   iter-metrics.sh /path/to/session.jsonl          # explicit
#   iter-metrics.sh /path/to/session.jsonl session2.jsonl ...   # multiple sessions, summed
#
# Requires: jq.

set -e

if ! command -v jq >/dev/null 2>&1; then
  echo "iter-metrics.sh: jq required" >&2
  exit 1
fi

# Resolve session files.
sessions=("$@")
if [ ${#sessions[@]} -eq 0 ]; then
  # Auto-pick: most recent JSONL under ~/.claude/projects/<repo-encoded>/sessions/.
  repo_encoded=$(echo "$PWD" | sed 's|/|-|g')
  cand_dir=$(ls -d "$HOME/.claude/projects/"*"$repo_encoded"* 2>/dev/null | head -1)
  if [ -z "$cand_dir" ]; then
    echo "iter-metrics.sh: no session dir for $PWD under ~/.claude/projects/" >&2
    echo "Pass session path(s) explicitly." >&2
    exit 1
  fi
  latest=$(ls -t "$cand_dir"/sessions/*.jsonl 2>/dev/null | head -1)
  if [ -z "$latest" ]; then
    echo "iter-metrics.sh: no .jsonl in $cand_dir/sessions/" >&2
    exit 1
  fi
  sessions=("$latest")
fi

# Verify all files exist.
for f in "${sessions[@]}"; do
  if [ ! -f "$f" ]; then
    echo "iter-metrics.sh: file not found: $f" >&2
    exit 1
  fi
done

# Concatenate input.
input_file=$(mktemp)
trap 'rm -f "$input_file"' EXIT
cat "${sessions[@]}" > "$input_file"

# Aggregate cost + token counters via jq.
# Each assistant message line typically has usage info under .message.usage:
#   input_tokens, output_tokens, cache_creation_input_tokens, cache_read_input_tokens
# Sum across all lines.
read -r in_tokens out_tokens cc_tokens cr_tokens <<<"$(jq -r '
  [
    (.message.usage // {}) |
    [
      (.input_tokens // 0),
      (.output_tokens // 0),
      (.cache_creation_input_tokens // 0),
      (.cache_read_input_tokens // 0)
    ]
  ] | add // [0,0,0,0] | "\(.[0]) \(.[1]) \(.[2]) \(.[3])"
' "$input_file" 2>/dev/null | awk '
  { in_t += $1; out_t += $2; cc += $3; cr += $4 }
  END { print in_t, out_t, cc, cr }
')"
in_tokens=${in_tokens:-0}
out_tokens=${out_tokens:-0}
cc_tokens=${cc_tokens:-0}
cr_tokens=${cr_tokens:-0}

# Cache hit rate: cache_read / (cache_read + cache_creation + input).
cache_pct=$(awk -v r="$cr_tokens" -v c="$cc_tokens" -v i="$in_tokens" \
  'BEGIN { total = r + c + i; if (total > 0) printf "%.1f", (r * 100.0) / total; else printf "0.0" }')

# Cost: try .totalCostUsd field at session-summary boundaries (rare), else compute via posted prices.
# Posted Anthropic pricing (USD per 1M tokens, May 2026 snapshot — update if prices change):
#   Opus 4.7:    input $15, output $75, cache write $18.75, cache read $1.50
#   Sonnet 4.6:  input $3,  output $15, cache write $3.75,  cache read $0.30
#   Haiku 4.5:   input $1,  output $5,  cache write $1.25,  cache read $0.10
# We don't know the per-message model split reliably here without parsing each message's model field.
# Compute model-aware cost: sum per-line by .message.model prefix.
cost_usd=$(jq -r '
  .message as $m |
  ($m.model // "" | ascii_downcase) as $model |
  ($m.usage // {}) as $u |
  if $model == "" then empty else
    (if ($model | test("opus"))   then [15, 75, 18.75, 1.50]
     elif ($model | test("sonnet")) then [3,  15,  3.75, 0.30]
     elif ($model | test("haiku"))  then [1,   5,  1.25, 0.10]
     else [3, 15, 3.75, 0.30] end) as $price |
    (($u.input_tokens // 0) * $price[0]
   + ($u.output_tokens // 0) * $price[1]
   + ($u.cache_creation_input_tokens // 0) * $price[2]
   + ($u.cache_read_input_tokens // 0) * $price[3]) / 1000000
  end
' "$input_file" 2>/dev/null | awk '{ s += $1 } END { printf "%.4f", s }')
cost_usd=${cost_usd:-0.0000}

# Model mix: count messages by model family.
model_mix=$(jq -r '.message.model // empty' "$input_file" 2>/dev/null | awk '
  /opus/   { o++ }
  /sonnet/ { s++ }
  /haiku/  { h++ }
  END {
    total = o + s + h
    if (total == 0) { print "n/a"; exit }
    if (o > 0) printf "Opus %.0f%% ", (o * 100.0 / total)
    if (s > 0) printf "Sonnet %.0f%% ", (s * 100.0 / total)
    if (h > 0) printf "Haiku %.0f%%", (h * 100.0 / total)
    printf "\n"
  }')
[ -z "$model_mix" ] && model_mix="n/a"

# Subagent invocations by subagent_type.
sub_table=$(jq -r '
  . | (.message.content // [])[]? |
  select(.type == "tool_use" and .name == "Task") |
  (.input.subagent_type // "unknown")
' "$input_file" 2>/dev/null | sort | uniq -c | awk '{ printf "  - %s: %d\n", $2, $1 }')
[ -z "$sub_table" ] && sub_table="  - (none)"

# Task tool count (built-in Task tracking — different from Task agent).
# Counts unique tool-call IDs across all assistant turns.
total_subagent_calls=$(jq -r '
  . | (.message.content // [])[]? |
  select(.type == "tool_use" and .name == "Task") | .id
' "$input_file" 2>/dev/null | sort -u | wc -l | tr -d ' ')

# Session count and time span.
session_count=${#sessions[@]}
first_ts=$(jq -r '.timestamp // empty' "$input_file" 2>/dev/null | sort | head -1)
last_ts=$(jq -r '.timestamp // empty' "$input_file" 2>/dev/null | sort | tail -1)

# Output markdown block.
cat <<EOF
## Resources & Cost

| Metric | Value |
|---|---|
| Sessions aggregated | $session_count |
| Time span | ${first_ts:-n/a} → ${last_ts:-n/a} |
| Cost (USD, model-aware) | \$$cost_usd |
| Cache hit rate | ${cache_pct}% |
| Input tokens (non-cached) | $in_tokens |
| Output tokens | $out_tokens |
| Cache-creation tokens | $cc_tokens |
| Cache-read tokens | $cr_tokens |
| Model mix | $model_mix |
| Subagent calls (total) | $total_subagent_calls |

### Subagent breakdown
$sub_table

### Loadout-vs-actual

- Loadout matrix row: _(fill: e.g. "Migration-step execution, ~7k budget, Sonnet")_
- Actual model used: _(fill from "Model mix" above)_
- Budget hit / over / under: _(fill — compare cost vs row budget)_
- Subagent dispatch followed: _(yes / no — were cavecrew rules respected?)_

EOF
