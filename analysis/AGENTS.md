# Analysis API Guidelines

A library for analyzing Kotlin code at the semantic level, providing structured access to symbols, types, and semantic relationships.

**Entry point:** Use [`analyze()`](analysis-api/src/org/jetbrains/kotlin/analysis/api/analyze.kt) to start an analysis session. See [Analysis API documentation](https://kotl.in/analysis-api) for a usage guide.

## Architecture

- **Platform** — Provides declarations, project structure, and modification events (IntelliJ, Standalone)
- **Engine** — Performs code analysis using platform-provided information (based on K2 compiler)
- **User** — Code that calls `analyze()` to work with symbols and types

→ READ [`analysis-api-platform-interface/README.md`](analysis-api-platform-interface/README.md) for detailed architecture overview

## Relationship with PSI

Analysis API builds on top of Kotlin PSI (`compiler/psi/`):
- **PSI** provides syntax (structure of code): `KtElement`, `KtExpression`, `KtDeclaration`
- **Analysis API** provides semantics (meaning of code): `KaSymbol`, `KaType`

```
PSI (syntax) → Analysis API (semantics) → Symbols, Types, Resolution
```

**Both PSI and Analysis API follow shared development principles** documented in [`docs/contribution-guide/api-development.md`](docs/contribution-guide/api-development.md).

WHEN working with PSI elements:
→ READ [`compiler/psi/AGENTS.md`](../compiler/psi/AGENTS.md) for PSI-specific rules and conventions

## Key Conventions

- `Ka` prefix for Analysis API types, `Kt` for PSI types
- Prefer interfaces to classes for better binary compatibility
- Properties for attributes, functions for actions with parameters
- Return nullable types for operations that can fail (avoid exceptions for non-exceptional cases)
- All implementations must validate lifetime ownership with `withValidityAssertion`
- Mark experimental APIs with `@KaExperimentalApi`, implementation details with `@KaImplementationDetail`

## Working with Test Data

Golden files of the `*Generated` tests implementing `ManagedTest`, in the modules applying `id("test-data-manager")`
(most of `analysis/`, and `compiler/psi/psi-impl`), variants (`.knm.*`, `.js.*`, …) included, are checked and updated
with one command. Don't use `:test`, `-Pkotlin.test.update.test.data=true` or the Gradle tasks directly for them: a
plain test run doesn't order the variant chains and never writes a variant file.

```bash
python3 analysis/test-data-manager/test-data-manager.py update --path <dir-or-file>[,<dir-or-file>...]
python3 analysis/test-data-manager/test-data-manager.py check --path <dir-or-file>[,<dir-or-file>...]
```

- `--path` is repository-relative, as in `@TestMetadata`; one test is its test data file (`.../foo.kt`), not its
  output (`foo.txt`). A new test data file needs `./gradlew generateTests` first.
- The script picks the modules: don't add `--module`, don't split the run per module. Don't add `--incremental` or
  `--golden-only` unless the user asks: they skip tests.
- The output is a few lines: read it as is, don't pipe it into `tail`/`grep` (the pipe hides the exit code).
- Wait for the command to exit and read its exit code; the output and the summary are its result. A run may outlast
  the tool timeout, which may kill it: raise the timeout or use a tracked background task. The Gradle log, whose path
  is printed first, shows the progress.
- `--rerun <runId>` instead of `--path` runs again the test data files of the tests that failed in that run, in all
  their suites and variants; that run's `--class-pattern` and `--golden-only` don't carry over. Its id is in the first
  line of its result (`… FAILED, runId <id>`).

| Exit | Output                                                          | Next step                                                                                                                                          |
|------|-----------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| 0    | `PASSED`; after `update`, `CREATED`/`UPDATED`/`DELETED` lines   | Done; no `check` after a passed `update`. Review the change with `git diff`.                                                                       |
| 0    | `RECOVERED`                                                     | The test failed and then passed on a rerun within the run: nothing to do.                                                                          |
| 1    | `MISMATCH`, `MISSING_FILE`                                      | Intended change: `update --rerun <runId>`. Otherwise diff `actualFile` with `comparedFile` (in the module report, linked from the summary JSON; a missing file has only `actualFile`). After a golden file changes, its variants may be listed too, as `REDUNDANT_FILE` or `MISSING_FILE`: the same `update` settles them. |
| 1    | `REDUNDANT_FILE`, `UNEXPECTED_FILE`                             | The file has to go, `update` deletes it: it equals the file it falls back to, or the test produces nothing for it. There is no output to diff.     |
| 1    | `TEST ERROR`                                                    | The test crashed; `update` won't fix it. A known problem (below): `check --rerun <runId>`. Otherwise fix the code or the test; the stack trace is in the module report (`testErrors[].stackTrace`). |
| 2    | `ERROR …`, `GRADLE FAILURE`, `e: …`                             | Fix what the line names (a path, a compilation error) and rerun. `is an output file`: pass the test data file it names. `matches no managed test`: a new test data file needs `./gradlew generateTests`; otherwise the files aren't managed, use the area's own runner. |
| 130  | `Interrupted …`                                                 | The run was interrupted; run it again.                                                                                                             |

Known problems, not caused by your change:

- `TEST ERROR` with `BuiltinsVirtualFileProvider duplicated` in a random `:analysis:stubs` test: a flake that `update`
  retries by itself and `check` doesn't.

Options, the JSON summary and direct Gradle calls: [test-data-manager/README.md](test-data-manager/README.md#wrapper-script-recommended-for-agents-and-scripts)
and [test-data-manager-convention](../repo/gradle-build-conventions/test-data-manager-convention/README.md).

## Key Components

- [`analysis-api/`](analysis-api) - User-facing API surface (`KaSession`, `KaSymbol`, `KaType`)
- [`analysis-api-platform-interface/`](analysis-api-platform-interface) - Platform abstraction (declaration providers, project structure, lifetime)
- [`analysis-api-standalone/`](analysis-api-standalone) - CLI-based implementation of the Analysis API
- [`analysis-api-fir/`](analysis-api-fir) - K2 implementation based on FIR
- [`analysis-api-fir-diagnostics/`](analysis-api-fir-diagnostics) - `KaFirDiagnostic` interfaces generated from the compiler's diagnostics
- [`low-level-api-fir/`](low-level-api-fir) - K2-specific infrastructure for lazy/incremental analysis
- [`symbol-light-classes/`](symbol-light-classes) - Java PSI view of Kotlin declarations for interop
- [`stubs/`](stubs) - Stub implementation (stub classes, their serializers, and the file stub builder) and stub tests
- [`decompiled/decompiler`](decompiled/decompiler) - Decompilers, cls stub builders, and decompiled PSI
- [`decompiled/light-classes-for-decompiled`](decompiled/light-classes-for-decompiled) - Light classes for decompiled/library code
- [`test-data-manager/`](test-data-manager) - Infrastructure for managing test data files with variant chains

## Detailed Documentation

WHEN adding or modifying API endpoints:
→ READ [`docs/contribution-guide/api-development.md`](docs/contribution-guide/api-development.md)

WHEN deprecating API or understanding stability categories:
→ READ [`docs/contribution-guide/api-evolution.md`](docs/contribution-guide/api-evolution.md)

WHEN implementing platform components:
→ READ [`analysis-api-platform-interface/README.md`](analysis-api-platform-interface/README.md)

WHEN working with light classes:
→ READ [`symbol-light-classes/README.md`](symbol-light-classes/README.md)

WHEN working with lazy resolution (LL API):
→ READ [`low-level-api-fir/README.md`](low-level-api-fir/README.md)

WHEN writing or managing test data files:
→ READ [`test-data-manager/AGENTS.md`](test-data-manager/AGENTS.md)

WHEN seeking historical context on design decisions:
→ READ [`docs/design-documents/README.md`](docs/design-documents/README.md) (these are historical snapshots, not necessarily up to date)

WHEN working with stubs:
→ READ [`stubs/README.md`](stubs/README.md)
