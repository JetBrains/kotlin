---
project: Kotlin
languages: [Kotlin, Java]
build-system: Gradle
repository: monorepo
---

# Guidelines for Kotlin Development

## Project Guidelines

**CRITICAL: [guidelines.md](../.ai/guidelines.md) MUST be followed at all times.**

## Junie-specific notes

See [`../.ai/junie.md`](../.ai/junie.md) for how Junie maps to the Claude Code-centric tooling
in `.claude/` (hooks, subagents, slash commands, MCP tools).

## Per-workstream guidelines

- **Scripting / REPL (`plugins/scripting/**`, `libraries/scripting/**`, scripting-related parts of
  `compiler/cli/`, `compiler/daemon/`, `compiler/fir/`, `compiler/ir/`, `compiler/build-tools/`,
  and `libraries/tools/kotlin-{main-kts,gradle-plugin}` scripting bits):**
  follow [`plugins/scripting/.ai/AGENT_INSTRUCTIONS.md`](../plugins/scripting/.ai/AGENT_INSTRUCTIONS.md)
  as the source of truth, with Junie-specific overrides in
  [`plugins/scripting/.ai/JUNIE_NOTES.md`](../plugins/scripting/.ai/JUNIE_NOTES.md).
  Optional local pre-load index template: [`memory/scripting.md`](memory/scripting.md)
  (`.junie/memory/` is gitignored — each contributor opts in by creating the file locally).
