# Analysis API Guidelines

A library for analyzing Kotlin code at the semantic level, providing structured access to symbols, types, and semantic relationships.

**Entry point:** Use [`analyze()`](analysis-api/src/org/jetbrains/kotlin/analysis/api/analyze.kt) to start an analysis session. See [Analysis API documentation](https://kotl.in/analysis-api) for a usage guide.

## Architecture

- **Platform** — Provides declarations, project structure, and modification events (IntelliJ, Standalone)
- **Engine** — Performs code analysis using platform-provided information (K1, K2)
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

When modifying test data files or running generated tests (`*Generated`) that compare output against `.txt` files, use `updateTestData` instead of standard test commands. **Never use `manageTestDataGlobally --mode=update`** — it re-runs Gradle configuration (1–2 min) every time a CLI flag value changes, which makes iteration painful. `updateTestData` accepts the same options as `-P` properties and keeps the configuration cache stable across runs (sub-second reconfiguration), so it is strictly better for any update workflow.

### `updateTestData` — the only recommended way to update test data

```bash
# Update test data by directory (preferred for iteration)
./gradlew updateTestData -Porg.jetbrains.kotlin.testDataManager.options.testDataPath=analysis/analysis-api/testData/components/resolver/

# Update test data by test class pattern
./gradlew updateTestData -Porg.jetbrains.kotlin.testDataManager.options.testClassPattern=.*ResolveTest.*

# Run only golden tests (useful for quick baseline updates)
./gradlew updateTestData -Porg.jetbrains.kotlin.testDataManager.options.goldenOnly=true

# Incremental update — only re-run variant tests for changed paths
./gradlew updateTestData -Porg.jetbrains.kotlin.testDataManager.options.incremental=true

# Limit to a subset of modules using task paths (Gradle task-name matching)
./gradlew :analysis:analysis-api-fir:updateTestData :analysis:stubs:updateTestData
```

`updateTestData` is fixed to update mode. There is no `updateTestDataGlobally` — Gradle's task-name matching runs the task in every applicable subproject when invoked from the repo root.

### `manageTestDataGlobally --mode=check` — verification only

If you specifically need to verify that existing test data is consistent without modifying anything (e.g., sanity-checking generated files after an `updateTestData` run), use check mode:

```bash
./gradlew manageTestDataGlobally --mode=check --test-data-path=analysis/analysis-api/testData/components/resolver/singleByPsi/
```

Use this only for verification. For any workflow that writes test data, always prefer `updateTestData`.

**Why use these tasks instead of plain `:test`?**
- Run only relevant tests (filtered by path or class pattern)
- Handle variant chains correctly (golden `.txt` files run before variant-specific `.js.txt`, `.wasm.txt`, etc.)
- Automatically discover all modules that use managed test data
- Detect and remove redundant variant files

For full options, see [test-data-manager-convention](../repo/gradle-build-conventions/test-data-manager-convention/README.md).

## Key Components

- [`analysis-api/`](analysis-api) - User-facing API surface (`KaSession`, `KaSymbol`, `KaType`)
- [`analysis-api-platform-interface/`](analysis-api-platform-interface) - Platform abstraction (declaration providers, project structure, lifetime)
- [`analysis-api-standalone/`](analysis-api-standalone) - CLI-based implementation of the Analysis API
- [`analysis-api-fir/`](analysis-api-fir) - K2 implementation based on FIR
- [`analysis-api-fe10/`](analysis-api-fe10) - K1 implementation based on classic frontend
- [`analysis-api-impl-base/`](analysis-api-impl-base) - Shared implementation utilities
- [`low-level-api-fir/`](low-level-api-fir) - K2-specific infrastructure for lazy/incremental analysis
- [`symbol-light-classes/`](symbol-light-classes) - Java PSI view of Kotlin declarations for interop
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
