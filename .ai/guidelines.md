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
- Generated test runners are frequently written to the gitignored `<module>/build/tests-gen/` directory, not the tracked `tests-gen/`. This isn't just a repo-search (grep/glob over Git content) blind spot — JetBrains MCP search/read tools and JetBrains AI Assistant also can't see or open gitignored files, even given the exact path, so a "no matches" result there is never proof the file doesn't exist. Before concluding a runner "doesn't exist", read [testing.md § Where generated runners land](testing.md#where-generated-runners-land--read-before-searching)
- Not every task whose name looks like a test runner is JUnit-filterable with `--tests`. Some modules register additional CI-only task names (e.g. `jsTest`/`jsES6Test` in `js/js.tests`, `fastJarFSLongTests` in `compiler`, `aggregateTests`/`nightlyTests` in `compiler/fir/fir2ir`) with `skipInLocalBuild = true`; locally (off TeamCity) these become plain no-op `Task`s, not `Test` tasks, so `--tests` silently fails on them. The real filterable task is normally the module's plain `test` task (e.g. `:js:js.tests:test`) — read [testing.md § Finding the real test task](testing.md#finding-the-real-test-task-vs-decoy-tasks) before assuming a task doesn't support filtering

## Areas

**BEFORE running tests, modifying, or investigating code — identify the area and READ its docs.**

| Area                     | Prefixes               | Location                                                  | Docs                                                                             |
|--------------------------|------------------------|-----------------------------------------------------------|----------------------------------------------------------------------------------|
| Analysis API             | `Ka*`, `KaFir*`, `LL*` | analysis/                                                 | [AGENTS.md](../analysis/AGENTS.md)                                               |
| Backend: JVM             |                        | compiler/ir/backend.jvm/                                  | [AGENTS.md](../compiler/AGENTS.md)                                               |
| Backend: JS              |                        | compiler/ir/backend.js/                                   | [AGENTS.md](../compiler/AGENTS.md)                                               |
| Backend: Native          |                        | kotlin-native/, native/                                   | [AGENTS.md](../compiler/AGENTS.md)                                               |
| Backend: WASM            |                        | compiler/ir/backend.wasm/                                 | [AGENTS.md](../compiler/AGENTS.md)                                               |
| Build Tools API          |                        | compiler/build-tools/                                     | [AGENTS.md](../compiler/build-tools/AGENTS.md)                                   |
| Compiler plugins         |                        | plugins/                                                  | —                                                                                |
| FIR (K2 frontend)        | `Fir*`                 | compiler/fir/                                             | [AGENTS.md](../compiler/AGENTS.md)                                               |
| FIR Analysis Tests       |                        | compiler/fir/analysis-tests/                              | [AGENTS.md](../compiler/fir/analysis-tests/AGENTS.md)                            |
| IR                       | `Ir*`                  | compiler/ir/                                              | [AGENTS.md](../compiler/AGENTS.md)                                               |
| K1 (legacy frontend)     |                        | compiler/frontend/                                        | —                                                                                |
| Kotlin Gradle Plugin     |                        | libraries/tools/kotlin-gradle-plugin/                     | [AGENTS.md](../libraries/tools/kotlin-gradle-plugin/AGENTS.md)                   |
| Kotlin Gradle Plugin API |                        | libraries/tools/kotlin-gradle-plugin-api/                 | [AGENTS.md](../libraries/tools/kotlin-gradle-plugin-api/AGENTS.md)               |
| KGP Integration Tests    |                        | libraries/tools/kotlin-gradle-plugin-integration-tests/   | [AGENTS.md](../libraries/tools/kotlin-gradle-plugin-integration-tests/AGENTS.md) |
| PSI                      | `Kt*`                  | compiler/psi/                                             | [AGENTS.md](../compiler/psi/AGENTS.md)                                           |
| Standard library         |                        | libraries/stdlib/                                         | —                                                                                |
| Test infrastructure      |                        | compiler/test-infrastructure/, compiler/tests-common-new/ | [testing.md](testing.md)                                                         |

> **Adding new area docs:** Create `AGENTS.md` with content and `CLAUDE.md` containing only `@AGENTS.md`

## Running Individual Tests

**MANDATORY: First check the [Areas](#areas) table above — some areas have specialized test tooling.**

Use `-q` (quiet) flag to reduce output noise. Example of commands for areas WITHOUT specialized tooling:

```bash
# Run a specific test class
./gradlew :compiler:test --tests "org.jetbrains.kotlin.codegen.BlackBoxCodegenTestGenerated" -q

# Run a specific test method
./gradlew :compiler:test --tests "org.jetbrains.kotlin.codegen.BlackBoxCodegenTestGenerated.testSomeTest"

# Run FIR compiler tests
./gradlew :compiler:fir:fir2ir:test --tests "org.jetbrains.kotlin.test.runners.ir.FirLightTreeJvmIrTextTestGenerated"

# Run JS backend box tests — use the plain `test` task, NOT `jsTest`/`jsES6Test` (those are CI-only decoy tasks locally)
./gradlew :js:js.tests:test --tests "org.jetbrains.kotlin.js.test.runners.JsCodegenBoxTestGenerated.testSomeTest"

# Update test data files (when format changes)
./gradlew :compiler:test --tests "TestClassName" -Pkotlin.test.update.test.data=true --continue
```

The first local invocation of JS/Native/Wasm test tasks can take a while to configure the build and provision the JS/Native toolchain (Node.js, D8, klib dependencies, etc.) before any test actually runs — this setup cost is expected and is not a sign that something is broken.

## Commit Guidelines

**When creating a commit, you MUST read [`commit-guidelines.md`](commit-guidelines.md) first** — do not author or amend a commit without following it. It holds the mandatory rules for commit messages, formatting, subsystem tag prefixes, and MR structure.

## JetBrains IDE MCP - MANDATORY for the project files and operations

**NEVER use these tools:** `Grep`, `Glob`, `Read`, `Edit`, `Write`, `Task(Explore)`.
**ALWAYS use JetBrains MCP equivalents instead.**

**Exception:** for paths outside the project (e.g., `~/.claude/`), use standard tools — MCP only works with project-relative paths.

**Exception:** for anything under a gitignored build output directory (e.g. `<module>/build/tests-gen/`, `<module>/build/reports/`), JetBrains MCP tools (`search_file`, `search_text`, `read_file`) — and JetBrains AI Assistant's own file search/reference more generally — will report **no matches / can't find it, even for files that exist**, because they filter out `.gitignore`d paths at the index level, regardless of how exact the path or glob pattern is. Use `Bash` (`find`, `ls`, `cat`) instead, which reads the filesystem directly. See [testing.md § Where generated runners land](testing.md#where-generated-runners-land--read-before-searching).

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
- `search_text` uses IntelliJ indexes — faster than grep on large codebases
- `rename_refactoring` understands code structure and updates all references correctly
- `get_symbol_info` provides type info, documentation, and declarations
- `get_file_problems` runs IntelliJ inspections beyond syntax checking

### MCP server configuration

The JetBrains IDE MCP server can be called as `jetbrains`, `idea`, `my-idea`, `my-idea-dev`, etc.
If there are many options for the JetBrains IDE MCP server, ask the user what MCP server to use.

### Tool mapping

| Instead of      | Use JetBrains MCP                    |
|-----------------|--------------------------------------|
| `Read`          | `read_file`                          |
| `Edit`, `Write` | `apply_patch`, `create_new_file`     |
| `Grep`          | `search_text`, `search_regex`        |
| `Glob`          | `search_file`                        |
| `Task(Explore)` | `list_directory_tree`, `search_text` |

### Additional MCP tools

- **Code analysis**: `get_symbol_info`, `get_file_problems` for understanding code
- **Refactoring**: `rename_refactoring` for symbol renaming (safer than text replacement)
- **Run configurations**: `get_run_configurations()` to discover, or `execute_run_configuration(name="...")` if name is known

### MANDATORY - Verify After Writing Code

Use JetBrains MCP `get_file_problems` with errorsOnly=false to check files for warnings. FIX any warnings related to the code changes made. You may ignore unrelated warnings.

Run the relevant tests after making changes. Slowness is never a reason to skip. Fix failures before declaring done.

## Working with YouTrack

"KT-XXXXX", where XXXXX is the issue number, is an issue in https://youtrack.jetbrains.com/.
The direct URL for an issue is `https://youtrack.jetbrains.com/issue/KT-XXXXX`.
When accessing youtrack.jetbrains.com, never fetch web pages from such URLs directly.
Use YouTrack MCP if configured.
Otherwise, use `youtrack-cli` skill if configured.
Otherwise, use YouTrack REST API, e.g. with a GET request to
```text
https://youtrack.jetbrains.com/api/issues/KT-XXXXX?fields=fields=summary,description,customFields(name,value(name,login,text))
```
