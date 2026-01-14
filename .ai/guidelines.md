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

## Key Directories

- `compiler/fir/` - K2 frontend (FIR)
- `compiler/ir/` - Backend IR and lowerings
- `compiler/frontend/` - Legacy K1
- `compiler/test-infrastructure/` - Test framework core
- `compiler/tests-common-new/` - Test runners and handlers
- `analysis/` - Analysis API related code
- `compiler/psi` - Kotlin PSI
- `plugins/` - Compiler plugins
- `libraries/` - stdlib, kotlin-test, kotlin-reflect

## Commit Guidelines

**BEFORE creating any commit, you MUST read `docs/code_authoring_and_core_review.md`** — it contains essential rules for commit messages, code review process, and MR structure.

Key points (not exhaustive):
- Reference YouTrack issues (KT-XXXXX) in commit messages when applicable
- Use `^KT-XXXXX Fixed` in body to auto-close issues
- Keep subject line under 72 characters, imperative mood
- Commit messages must explain not just WHAT but also WHY and HOW
- Commit tests together with corresponding code changes
- Non-functional changes (refactorings, reformats) should be in separate commits

## Area-Specific Guidelines

To add new area-specific guidelines, create two files in the module directory:
1. `AGENTS.md` — the actual documentation content
2. `CLAUDE.md` — contains only `@AGENTS.md` (for tool compatibility)

WHEN working with compiler internals (FIR, IR, backends):
→ READ compiler/AGENTS.md

WHEN writing or modifying tests:
→ READ .ai/testing.md

WHEN working with Analysis API:
→ READ analysis/AGENTS.md

WHEN working with Kotlin PSI (syntax tree, KtElement, KtExpression):
→ READ compiler/psi/AGENTS.md

## JetBrains IDE MCP - MANDATORY for file and project operations

**NEVER use these tools:** `Grep`, `Glob`, `Read`, `Edit`, `Write`, `Task(Explore)`.
**ALWAYS use JetBrains MCP equivalents instead.**

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
- **Terminal**: `execute_terminal_command` for running commands
- **Run configurations**: `get_run_configurations()` to discover, or `execute_run_configuration(name="...")` if name is known

### MANDATORY - Verify After Writing Code

Use JetBrains MCP `get_file_problems` with errorsOnly=false to check files for warnings. FIX any warnings related to the code changes made. You may ignore unrelated warnings.