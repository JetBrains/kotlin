---
name: analysis-api-create-cherry-pick-issue
description: Create a KTIJ cherry-pick tracking issue for a KT fix that needs to be cherry-picked to an IntelliJ branch. Use when cherry-picking Analysis API fixes.
disable-model-invocation: true
---

# Analysis API: Create Cherry-Pick Issue

Creates a KTIJ tracking issue for cherry-picking a KT fix to an IntelliJ release branch.

## Prerequisites

This skill requires the **YouTrack MCP** (`mcp__YouTrack__*` tools). If the YouTrack MCP is not available, **stop immediately** and inform the user that this skill cannot run without it.

## Required User Inputs

- **KT issue ID** (e.g. `KT-84711`)
- **Target version** (e.g. `2026.1.1`)
- **Commit hashes** (e.g. `0e2ca82e`, `449bdb32`)

## Workflow

### Step 1: Gather data

Run these in parallel:

1. **Fetch the KT issue** via `mcp__YouTrack__get_issue` to get:
    - `summary` — used in the KTIJ issue summary
    - `customFields.Severity` — mapped to KTIJ Priority (see mapping below)
2. **Fetch the current user** via `mcp__YouTrack__get_current_user` to get the `login` for the assignee.

**Severity-to-Priority mapping** (KT Severity → KTIJ Priority):

| KT Severity | KTIJ Priority |
|---|---|
| Critical | Critical |
| Major | Major |
| Normal | Normal |
| Minor | Minor |

If the KT issue has no Severity or an unmapped value, default to `Normal`.

### Step 2: Confirm with user

Before creating the issue, present the resolved fields and ask for confirmation:

- **Summary:** `Cherry-pick {KT-ID}: {KT summary} to {target version}`
- **Priority:** (mapped from Severity)
- **Assignee:** (from current user)
- **Commits:** (as provided)

### Step 3: Create the issue

Use `mcp__YouTrack__create_issue` with:

```
project: KTIJ
summary: "Cherry-pick {KT-ID}: {KT summary} to {target version}"
description: (see template below)
permittedGroups: ["jetbrains-team"]
customFields:
  Type: Task
  Priority: (mapped)
  State: Open
  Subsystems: ["IDE"]
  Planned for: ["{target version}"]
  Assignee: (current user login)
```

**Description template:**

```
Cherry-pick the fix for [{KT-ID}](https://youtrack.jetbrains.com/issue/{KT-ID}) to IntelliJ {target version}.

**Commits:** `{hash1}`, `{hash2}`, ...

**Original issue:** {KT-ID} — {KT summary}
```

### Step 4: Add tag

Use `mcp__YouTrack__manage_issue_tags` to add `Needs cherry-picking` to the created issue.

### Step 5: Link issues

Use `mcp__YouTrack__link_issues` to link the KT issue as a **previous step** of the new KTIJ issue:

```
targetIssueId: {KTIJ-ID}
linkType: "previous step"
issueToLinkId: {KT-ID}
```

### Step 6: Verify

Fetch the created issue via `mcp__YouTrack__get_issue` and confirm all fields are correct. Report the issue ID and URL to the user.
