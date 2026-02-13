# Test Data Manager

Automated system for managing test data files across multiple test configurations.

It supports only tests which implement `ManagedTest` interface and are generated (`*Generated` suffix).

## Core Concepts

### Variant Chains and Priority

Tests declare `TestVariantChain` via `ManagedTest.variantChain`:
- `[]` (empty) — golden configuration, writes to `.txt` files
- `["js"]` — writes to `.js.txt` files
- `["knm", "wasm"]` — writes to `.js.txt` files (only the **last** variant determines the output file)

**Priority rule**: Tests are grouped by variant chain **depth**. Fewer variants = higher priority.
- Depth 0 (golden) runs first
- Depth 1 (`[js]`, `[wasm]`, `[jvm]`) runs together
- Depth 2 (`[knm, native]`, `[knm, wasm]`) runs together
- etc.

Chose the variant chain that best reuses test data across configurations.

### File Access Rules

Each test can:
- **Read**: golden file and all files from its variant chain
- **Write**: only the most specific (last) variant file

Example for `["knm", "wasm"]`:
- Reads: `.txt` (golden), `.knm.txt`, `.wasm.txt`
- Writes: `.wasm.txt` only

This means:
- Tests with `["knm", "wasm"]` can inherit expected output from `.knm.txt` if no `.wasm.txt` exists. `.txt` (golden) will be used only if both `.knm.txt` and `.wasm.txt` don't exist. 
- Tests with just `["standalone"]` can only inherit from `.txt` (golden).

### Conflict Rule

Within the same variant depth group, tests run in parallel. A **conflict** occurs when:
- Test A's **last variant** appears **anywhere** in test B's variant chain

This would cause a race condition (A writes, B reads the same file).

**Examples:**
- `[a, b, c]` and `[a, c, b]` — CONFLICT (`c` is last in first, present in second)
- `[x, y, c]` and `[a, b, c]` — CONFLICT (both write to `.c.txt`)
- `[lib, js]` and `[lib, wasm]` — OK (different last variants, no overlap)

**Important:** The system doesn't resolve conflicts – execution will be terminated with an error in this case.

### Why Multi-Level Chains Matter

- Inheritance: `["knm", "js"]` can fall back to `.knm.txt` → `.txt`
- Shared behavior: Multiple tests (`["knm", "js"]`, `["knm", "wasm"]`) share `.knm.txt` and `.txt`
- Minimal redundancy: No need to duplicate content across similar configurations

### Examples

#### [AbstractCompiledStubsTest](../stubs/testFixtures/org/jetbrains/kotlin/analysis/stubs/AbstractCompiledStubsTest.kt)

The test generates tests for three platforms: JVM, JS, and Common. To properly share the test data, it has such a variant chain:
- JVM: `[]` (golden)
- Common: `[knm]`
- JS: `[knm, js]`
- Native: `[knm, native]` (not supported yet)
- WASM: `[knm, js, wasm]` (not supported yet)

Why this hierarchy?
1. JVM usually is treated as the main platform, so it makes sense to have it as the golden configuration since we are interested in its behavior most.
2. Non-JVM platforms are usually the same between each other, so it makes sense to have them share the same test data.
   Hence, some common variants should be used – `[knm]`.
3. Common is the most neutral platform, so it makes sense to have it as the golden configuration for non-JVM platforms (`[knm]`).
4. Native and JS are not related to each other, so most likely either both of them are equal to the `knm` variant, or they are different.
   Hence, they represent the next hierarchy level: `[knm, js]` and `[knm, native]`.
5. WASM in most cases behaves the same as JS and, most likely, if `js.txt` exists, then it will have the same difference with `knm.txt`.
   Hence, it represents the next hierarchy level: `[knm, js, wasm]`.

An alternative approach would be to use Common as the golden configuration:
- Common: `[]`
- JVM: `[jvm]` (golden)
- JS: `[js]`
- Native: `[native]` (not supported yet)
- WASM: `[js, wasm]` (not supported yet)

This would result in more efficient parallel execution (`[1, 3, 1]` grouping instead of `[1, 1, 2, 1]`) while potentially introducing
more redundant special suffices (`jvm.txt`). This is because some tests depend on the JVM-only features, so we wouldn't be able to use just `.txt`
because it won't be available since Common won't be executed, so we would have only `.jvm.txt` and no `.txt`.


#### [AbstractSymbolLightClassesTestBase](../symbol-light-classes/testFixtures/org/jetbrains/kotlin/light/classes/symbol/base/AbstractSymbolLightClassesTestBase.kt)

The test generates tests for:
- JVM-as-sources: `[]` (golden)
- JVM-as-library: `[lib]`
- JS-as-sources: `[knm]`
- JS-as-library: `[kmp.lib]`

Why this hierarchy?
1. JVM-as-sources is the main target for which light classes are built, so in most cases than adding a new test, this file is the most relevant.
2. JVM-as-library is the compiled version of the sources, and in some cases it has the same output as the sources. While they are in some sense "golden",
   they don't exist in cases with compilation errors and still sources are treated as the source of truth.
3. JS-as-sources is another target for which light classes are built, and it might have the same output as the JVM, so it makes sense reuse the test data.
4. JS-as-library is the compiled version of the JS sources, and they might have the same output as the JS sources and JVM library sources.
   In most cases the output is the same as the golden one, so there is no need to share it with `lib`. `[kmp]` could be used as the shared variant,
   but effectively there are no such cases that it would help to deduplicate the data.

### Convergence Loop

Within each group, tests may update files that other tests depend on.
The runner executes passes until no more updates occur (max 10 passes).

## Modes

- `CHECK` — fail on mismatch (default, for CI)
- `UPDATE` — silently update files on mismatch

Set via `-Dkotlin.test.data.manager.mode=check|update` (managed by Gradle tasks automatically)

## Usage

### Global Execution (Recommended)

Run across all modules with the test-data-manager plugin:

```bash
# Check mode (default) - fails if test data doesn't match
./gradlew manageTestDataGlobally

# Update mode - updates test data files
./gradlew manageTestDataGlobally --mode=update

# Run only golden tests (skip all variant-specific tests)
./gradlew manageTestDataGlobally --mode=update --golden-only

# Incremental update — skip variant tests for unchanged golden paths
./gradlew manageTestDataGlobally --mode=update --incremental
```

### Per-Module Execution

Run on a single module:

```bash
./gradlew :analysis:analysis-api-fir:manageTestData --mode=update
```

For full CLI options, see `repo/gradle-build-conventions/test-data-manager-convention/README.md`.

## Architecture

### Key Classes

- **`TestDataManagerRunner`** — Main entry point. Discovers tests, groups by variant depth, validates for conflicts, runs with convergence.
- **`VariantChainComparator`** — Orders variant chains (empty first, then by depth, then alphabetically).
- **`DiscoveredTest`** — Represents a test with its unique ID, display name, and variant chain.
- **`TestGroup`** — Represents tests with the same variant depth.
- **`GroupingResult`** — Result of grouping, includes groups and any detected conflicts.
- **`VariantChainConflict`** — Describes a conflict between two variant chains.

### Pure Functions

The runner uses pure functions for testability:

- **`discoverTests(testPlan)`** — Extracts test information from JUnit test plan
- **`groupByVariantDepth(tests)`** — Groups tests by variant chain depth, validates conflicts
- **`validateConflicts(tests)`** — Checks for race condition conflicts within a group

### Execution Flow

1. **Discovery**: Find all tests matching criteria
2. **Grouping**: Group by variant depth (not exact variant chain)
3. **Validation**: Check for conflicts that would cause race conditions
4. **Execution**: Run each group in order (0, 1, 2, ...) with a convergence loop

## Testing

The test data manager has comprehensive test coverage organized by component:

### Test Structure

| Component                | Test File                                    | Description                               |
|--------------------------|----------------------------------------------|-------------------------------------------|
| `VariantChainComparator` | `VariantChainComparatorTest.kt`              | Unit tests for variant chain ordering     |
| `groupByVariantDepth()`  | `TestDataManagerGroupingTest.kt`             | Unit tests for test grouping logic        |
| `validateConflicts()`    | `TestDataManagerGroupingTest.kt`             | Unit tests for conflict detection         |
| `ManagedTestFilter`      | `ManagedTestFilterTest.kt`                   | Filter for ManagedTest implementations    |
| `TestMetadataFilter`     | `TestMetadataFilterTest.kt`                  | Filter by @TestMetadata paths             |
| Discovery + Grouping     | `TestDiscoveryAndGroupingIntegrationTest.kt` | Integration tests for full pipeline       |

### Fake Test Classes

Located in `tests/.../fakes/`, these classes simulate real test configurations:

**Analysis tests** (`fakes/analysis/`):
- `FakeGoldenAnalysisApiTestGenerated` — golden (no variant)
- `FakeStandaloneAnalysisApiTestGenerated` — `[standalone]`
- `FakeLibrarySourceTestGenerated` — `[librarySource]`

**Light classes tests** (`fakes/lightclasses/`):
- `FakeGoldenLightClassesTestGenerated` — golden
- `FakeKnmLightClassesTestGenerated` — `[knm]`
- `FakeLibLightClassesTestGenerated` — `[lib]`
- `FakeLibKmpLightClassesTestGenerated` — `[lib, kmp.lib]`
- `FakeWasmLightClassesTestGenerated` — `[knm, wasm]`

**Conflict test cases** (`fakes/conflicts/`):
- `FakeConflictingTestAB` — `[a, b]`
- `FakeConflictingTestBA` — `[b, a]` (conflicts with AB)
- `FakeConflictingTestXB` — `[x, b]` (same last variant as AB)
- `FakeNonConflictingTestXY` — `[x, y]` (no conflict)

### Running Tests

```bash
# Run all tests
./gradlew :analysis:test-data-manager:test -q

# Run specific test class
./gradlew :analysis:test-data-manager:test --tests "*TestDiscoveryAndGroupingIntegrationTest" -q
```
