---
name: scripting-q-resolver
description: "Resolve a Q* item in plugins/scripting/.ai/target/90-open-questions.md: flip Status to `resolved` (or update sub-question status), link the landing iteration, bump Last-touched. Invoke when a Q's resolution lands (typically as part of `/scripting-iter-close`)."
disable-model-invocation: true
---

# Scripting Open-Question Resolver

Flips a Q* (or sub-question) in `plugins/scripting/.ai/target/90-open-questions.md` from `open` / `in-design` to `resolved` (or any other status) with required field updates. Keeps the triage table honest.

## When to use

- A migration-plan step lands that closes a Q* (e.g. step 1 lands → Q10a "DSL naming" resolves).
- Design discussion settles a sub-question (e.g. Q5b sidecar format picked).
- A Q* turns out to be a duplicate of another → mark `resolved` and link the survivor.

Do not use to:
- Add new Q* — append them directly (cheap edit).
- Reopen a resolved Q* — that needs a new Q-id, not a status flip.

## Procedure

### 1. Locate the Q

Read `plugins/scripting/.ai/target/90-open-questions.md`. Match `$ARGUMENTS` against `## Q<N>.` headings (top-level) OR `| Q<N><sub> |` rows (sub-questions). If ambiguous, list candidates and stop.

### 2. Gather the resolution

Ask the user for:
- New status (`resolved` | `in-design` | `blocked` | `open`).
- Owner if changing (or "unassigned").
- YT issue (if resolution generated a KT-XXXXX).
- Target doc (relative link to where the resolution landed — usually a migration-plan step or a current/* doc).
- Landing iteration (if applicable): `iterations/YYYY-MM-DD_<slug>.md`.

For sub-questions: the resolution typically resolves the row's `Status` cell + `Last touched` cell only — do not edit the parent Q unless ALL sub-questions are now resolved.

### 3. Apply edits

Top-level Q resolution:
- Replace `- Status: <old>` → `- Status: resolved` (or chosen status).
- Update `- Last touched: <today>`.
- Update `- Target doc: <path>` if the resolution moved it.
- Update `- YT: <id>` if applicable.
- If `resolved`: prepend `~~` and append `~~ — resolved` to the heading (matches existing convention for Q1/Q3/Q4/Q7/Q9).
- Append landing iteration link: `Landing: [<DATE>](../iterations/<DATE>_<slug>.md)`.

Sub-question resolution:
- Replace the `Status` cell value.
- Update the `Last touched` cell.

### 4. Promote the parent if all subs are resolved

If you resolved the last open sub-question of a parent (e.g. Q5e was the last open sub of Q5), ask the user if the parent Q should also flip to `resolved`. Do not auto-flip — the parent may carry additional context worth keeping open.

### 5. Bump "Last verified" header

`plugins/scripting/.ai/target/90-open-questions.md` carries a `> **Last verified**: YYYY-MM-DD` line. Update to today.

## Non-negotiable rules

- Never delete a Q-id — even fully resolved Qs stay (strikethrough heading + landing link). Q-ids are referenced from migration-plan steps, iterations, commits. Deleting breaks history.
- Never reassign a Q-id to a different question — same reason.
- Do not auto-commit (Non-Negotiable Rule #8).

## Caveats

- For Q2 (KT-83498): the canonical home is `target/50-migration-plan.md` step 2 (per the consolidation). Q2's body in `90-open-questions.md` is a pointer — when KT-83498 fully lands, flip Q2 to `resolved` here AND strike step 2 in the migration plan (per the post-iteration checklist).
- For Q10 / Q5 (umbrella Qs): the parent stays `in-design` while any sub remains open. Mark the umbrella `resolved` only when the design space is fully closed, not just when one sub is decided.
