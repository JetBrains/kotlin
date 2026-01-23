# Kotlin Project Guide

This file provides guidance for AI agents working with code in this repository.

## Repository Overview

This is the Kotlin programming language repository containing:
- Kotlin compiler (frontend and JVM, JS, WASM, Native backends)
- Compiler plugins (compose, serialization, allopen, noarg, etc.)
- Standard library, kotlin-reflect, kotlin-test
- Build system support (Gradle, Maven, JPS)
- Kotlin scripting support
- Analysis API

Note: The IntelliJ Kotlin plugin is in a separate repository (JetBrains/intellij-community).

## Build Commands

```bash
# Generate test sources (run after adding new test data files)
./gradlew generateTests
```
 
## Common Pitfalls

- Don't modify `*Generated.java` test files directly - regenerate them with `generateTests` Gradle task

## Running Individual Tests

Use `-q` (quiet) flag to reduce output noise and save tokens:

```bash
# Run a specific test class
./gradlew :compiler:test --tests "org.jetbrains.kotlin.codegen.BlackBoxCodegenTestGenerated" -q

# Run a specific test method
./gradlew :compiler:test --tests "org.jetbrains.kotlin.codegen.BlackBoxCodegenTestGenerated.testSomeTest"

# Run FIR compiler tests
./gradlew :compiler:fir:fir2ir:test --tests "org.jetbrains.kotlin.test.runners.ir.FirLightTreeJvmIrTextTestGenerated"

# Update test data files (when format changes)
./gradlew :compiler:test --tests "TestClassName" -Pkotlin.test.update.test.data=true --continue
```

## Commit Guidelines

**BEFORE creating any commit, you MUST read `docs/code_authoring_and_core_review.md`** — it contains essential rules for commit messages, code review process, and MR structure.

Key points (not exhaustive):
- Reference YouTrack issues (KT-XXXXX) in commit messages when applicable
- Use `^KT-XXXXX Fixed` in body to auto-close issues
- Keep subject line under 72 characters, imperative mood
- Commit messages must explain not just WHAT but also WHY and HOW
- Commit tests together with corresponding code changes
- Non-functional changes (refactorings, reformats) should be in separate commits

## Areas

**BEFORE modifying or investigating code, identify the area by class prefix or location, then READ the linked docs.**

| Area                 | Prefixes               | Location                                                  | Docs                                   |
|----------------------|------------------------|-----------------------------------------------------------|----------------------------------------|
| Analysis API         | `Ka*`, `KaFir*`, `LL*` | analysis/                                                 | [AGENTS.md](../analysis/AGENTS.md)     |
| Backend: JVM         |                        | compiler/ir/backend.jvm/                                  | [AGENTS.md](../compiler/AGENTS.md)     |
| Backend: JS          |                        | compiler/ir/backend.js/                                   | [AGENTS.md](../compiler/AGENTS.md)     |
| Backend: Native      |                        | kotlin-native/                                            | [AGENTS.md](../compiler/AGENTS.md)     |
| Backend: WASM        |                        | compiler/ir/backend.wasm/                                 | [AGENTS.md](../compiler/AGENTS.md)     |
| Compiler plugins     |                        | plugins/                                                  | —                                      |
| FIR (K2 frontend)    | `Fir*`                 | compiler/fir/                                             | [AGENTS.md](../compiler/AGENTS.md)     |
| IR                   | `Ir*`                  | compiler/ir/                                              | [AGENTS.md](../compiler/AGENTS.md)     |
| K1 (legacy frontend) |                        | compiler/frontend/                                        | —                                      |
| Kotlin Gradle Plugin |                        | libraries/tools/kotlin-gradle-plugin/                     | [AGENTS.md](../libraries/tools/kotlin-gradle-plugin/AGENTS.md) |
| Kotlin Gradle Plugin API |                        | libraries/tools/kotlin-gradle-plugin-api/                 | [AGENTS.md](../libraries/tools/kotlin-gradle-plugin-api/AGENTS.md) |
| PSI                  | `Kt*`                  | compiler/psi/                                             | [AGENTS.md](../compiler/psi/AGENTS.md) |
| Standard library     |                        | libraries/stdlib/                                         | —                                      |
| Test infrastructure  |                        | compiler/test-infrastructure/, compiler/tests-common-new/ | [testing.md](testing.md)               |

> **Adding new area docs:** Create `AGENTS.md` with content and `CLAUDE.md` containing only `@AGENTS.md`

## JetBrains IDE MCP - MANDATORY for the project files and operations

**NEVER use these tools:** `Grep`, `Glob`, `Read`, `Edit`, `Write`, `Task(Explore)`.
**ALWAYS use JetBrains MCP equivalents instead.**

**Exception:** for paths outside the project (e.g., `~/.claude/`), use standard tools — MCP only works with project-relative paths.

**NEVER use `execute_terminal_command` tool.**
**ALWAYS use default `Bash` instead.**

Use other similar tools only if it is not possible to use the JetBrains IDE MCP, and you together with the user can't manage to make it work.

### Why MCP over standard tools?

**Synchronization with IDE:**
- Standard tools work with the filesystem directly, MCP works with IDE's view of files
- If a file is open in IDE with unsaved changes, standard `Read` sees the old disk version, while MCP sees current IDE buffer
- Standard `Write`/`Edit` may conflict with IDE's buffer or not be picked up immediately
- MCP changes integrate with IDE's undo history

**IDE capabilities:**
- `search_in_files_by_text` uses IntelliJ indexes — faster than grep on large codebases
- `rename_refactoring` understands code structure and updates all references correctly
- `get_symbol_info` provides type info, documentation, and declarations
- `get_file_problems` runs IntelliJ inspections beyond syntax checking

### MCP server configuration

The JetBrains IDE MCP server can be called as `jetbrains`, `idea`, `my-idea`, `my-idea-dev`, etc.
If there are many options for the JetBrains IDE MCP server, ask the user what MCP server to use.

### Tool mapping

| Instead of      | Use JetBrains MCP                                     |
|-----------------|-------------------------------------------------------|
| `Read`          | `get_file_text_by_path`                               |
| `Edit`, `Write` | `replace_text_in_file`, `create_new_file`             |
| `Grep`          | `search_in_files_by_text`, `search_in_files_by_regex` |
| `Glob`          | `find_files_by_name_keyword`, `find_files_by_glob`    |
| `Task(Explore)` | `list_directory_tree`, `search_in_files_by_text`      |

### Additional MCP tools

- **Code analysis**: `get_symbol_info`, `get_file_problems` for understanding code
- **Refactoring**: `rename_refactoring` for symbol renaming (safer than text replacement)
- **Run configurations**: `get_run_configurations()` to discover, or `execute_run_configuration(name="...")` if name is known

### MANDATORY - Verify After Writing Code

Use JetBrains MCP `get_file_problems` with errorsOnly=false to check files for warnings. FIX any warnings related to the code changes made. You may ignore unrelated warnings.