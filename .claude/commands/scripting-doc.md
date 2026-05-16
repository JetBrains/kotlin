---
description: Load relevant scripting docs by topic keyword (bindings, daemon, compiler, test, defs, cli, gradle, q-N). Usage `/scripting-doc bindings`.
allowed-tools: Read
argument-hint: <topic-keyword>
---

# Load scripting docs by topic: $ARGUMENTS

Topic → doc map (case-insensitive substring match):

| Keyword | Docs to read |
|---|---|
| `bindings`, `jsr223`, `jsr-223` | `plugins/scripting/.ai/target/40-jsr223-target.md` + `plugins/scripting/.ai/current/60-jsr223.md` |
| `daemon`, `repl`, `legacy`, `remove` | `plugins/scripting/.ai/current/45-embedding-daemon-legacy.md` + `plugins/scripting/.ai/current/90-legacy-inventory.md` |
| `compiler`, `fir`, `ir`, `lowering` | `plugins/scripting/.ai/current/10-compiler-representation.md` + `plugins/scripting/.ai/target/10-compiler-target.md` |
| `customization`, `dsl`, `refinement` | `plugins/scripting/.ai/current/20-customization.md` |
| `api`, `module`, `jvm-host` | `plugins/scripting/.ai/current/30-api-layer.md` + `plugins/scripting/.ai/target/20-api-target.md` |
| `cli`, `script-flag`, `autoload` | `plugins/scripting/.ai/current/40-embedding-cli.md` + `plugins/scripting/.ai/target/30-embedding-target.md` |
| `gradle`, `bta`, `build-tools` | `plugins/scripting/.ai/current/41-embedding-build.md` |
| `defs`, `main-kts`, `definitions` | `plugins/scripting/.ai/current/50-script-definitions.md` |
| `test`, `tests` | `plugins/scripting/.ai/current/70-tests.md` |
| `principles`, `architecture` | `plugins/scripting/.ai/target/00-principles.md` |
| `migration`, `plan`, `step` | `plugins/scripting/.ai/target/50-migration-plan.md` |
| `q`, `question`, `open` | `plugins/scripting/.ai/target/90-open-questions.md` |
| `overview`, `layer`, `pipeline` | `plugins/scripting/.ai/current/00-overview.md` |

## Procedure

1. Match `$ARGUMENTS` against the keyword column. If multiple keywords match, load union of docs.
2. Read each matched doc.
3. If no match, list the keyword table and stop — do not guess.

## Caveats

- Always include `AGENT_INSTRUCTIONS.md` if not already loaded this session (stable prefix).
- For mutable-on-prototype docs (target/40, current/60), note the Cache lifetime tag at top — content may shift between sessions.
