# Test Data Manager

Automated system for managing test data files across multiple test configurations.

It supports only tests which implement `ManagedTest` interface and are generated (`*Generated` suffix).

## Core Concepts

### Variant Chains and Priority

Tests declare `TestVariantChain` via `ManagedTest.variantChain`:
- `[]` (empty) — golden configuration, writes to `.txt` files
- `["js"]` — writes to `.js.txt` files
- `["knm", "wasm"]` — writes to `.wasm.txt` files (only the **last** variant determines the output file)

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
`checkTestData` writes no files, so it runs a single pass, and a flaky failure is not retried.

## Modes

- `CHECK` — fail on mismatch, never write (`checkTestData`, CI)
- `UPDATE` — silently update files on mismatch (`updateTestData`)

Set via `-Dorg.jetbrains.kotlin.testDataManager.options.mode=check|update` (managed by Gradle tasks automatically).
A test started without the mode (plain `test` task, IDE gutter) falls back to CHECK, but locally it still creates a
missing golden file and deletes redundant ones; see the behavior matrix in [AGENTS.md](AGENTS.md).

## Usage

### Wrapper script (recommended for agents and scripts)

[`test-data-manager.py`](test-data-manager.py) is a thin wrapper: it runs `checkTestData` or `updateTestData` in the
modules with managed test data in one Gradle call with a shared run id, passes the options to the runner as is, and
aggregates the reports the modules print into one summary:

```bash
# Check test data under a directory
python3 analysis/test-data-manager/test-data-manager.py check --path compiler/psi/psi-impl/testData/psi/annotation

# Update it; the summary lists the created, updated and deleted files
python3 analysis/test-data-manager/test-data-manager.py update --path compiler/psi/psi-impl/testData/psi/annotation

# Several paths go into one --path, separated by commas, as in the testDataPath option of the tasks
python3 analysis/test-data-manager/test-data-manager.py check \
    --path compiler/psi/psi-impl/testData/psi/annotation,compiler/psi/psi-impl/testData/psi/examples

# Limit the modules, print the summary as JSON
python3 analysis/test-data-manager/test-data-manager.py check --path analysis/analysis-api/testData/components \
    --module :analysis:analysis-api-fir --json
```

On top of the module reports, it fails when a module wrote no report for the run (the task didn't run, or its JVM
crashed) and when no test in any module matches the filters. The Gradle output goes to
`build/test-data-manager/<runId>.gradle.log` and the summary to `build/test-data-manager/<runId>.summary.json`. Both
paths are printed to stderr, each on its own line, before Gradle starts, so a long run can be followed. The summary path
is also the last line of the output (`--json` prints the file instead of the text). The summary is written at once
when the run ends, so it is never read half-written. Whether the run is over is told by the process exit, not by the
file: a killed wrapper writes none. The summary is an index: the status, the run errors, the totals, the
counts of problems and changed files by kind, and the module reports, which hold the entries themselves. The text output
lists at most 50 entries of each kind. The 5 most recent runs are kept. `--run-id` sets the id, and so the paths of the
log, the summary and the module reports, in advance. When Gradle fails for another reason than a failing module report
(a compilation error, a configuration failure, an unknown module), the summary lists its `* What went wrong:` sections
and the compiler errors from the log (`gradleFailures` and `compilerErrors`).

`--rerun <runId>` runs, instead of `--path`, the test data files of the tests that failed in that run with a problem or
a test error, so `update --rerun <runId>` after a `check` updates just what failed. All the variants of these files run
again, as they depend on each other. The rerun is a run of its own, with its own id; the run it reruns is kept as long
as it runs, its module reports included (the `rerunId` option). It fails before Gradle when a test data file is gone
since, and it fails when the failures of the run aren't all known: a module without a report, a failure without a test
data path.

Compared with calling Gradle directly, the command is much shorter, a mistyped option fails instead of being ignored
(Gradle silently ignores an unknown `-P` property, so a misspelled `testDataPath` would run every test), and the output
is a few lines however many modules run.

With `--path`, it runs only the modules whose generated tests may have test data under the paths, so the others aren't
even built. It reads the `@TestMetadata` of the generated tests. The committed ones in `tests-gen` give the path of
every test, matched as the runner matches them. For `generateTestsInBuildDirectory`, the ones in `build/tests-gen` may be
older than the test data, so only the directories of their outermost classes count: a new file or directory of a suite
is inside one. A module without generated tests always runs. Each path is a binary search, so thousands of files are
fine. A path that doesn't exist fails the run before Gradle starts. A path no module has tests of fails it too, while
the modules of the other paths still run: an output file (`foo.txt`; the error names its test data file `foo.kt`), an
unmanaged file, or a new test data file before `./gradlew generateTests`.
The summary lists the skipped modules. `--module` runs the given modules as is.
It takes a project path, with or without the leading `:`, or the module directory (`analysis/stubs`).

Exit codes: `0` passed, `1` test data problems or test errors, `2` the run itself failed (run errors of a module, a
missing report, Gradle failing without a failing report), `130` interrupted with Ctrl+C. An interrupt stops Gradle,
waiting for it to cancel the build (a second Ctrl+C kills it). The summary is still written, with `"interrupted": true`
and the reports of the modules that finished.

Its tests use only the standard library and run in temporary git repositories, with a stub `gradlew`; they aren't part
of the Gradle build, so run them after changing the script:

```bash
python3 analysis/test-data-manager/test-data-manager-test.py
```

### Global Execution

Run across all modules with the test-data-manager plugin:

```bash
# Check mode - fails if test data doesn't match, writes nothing
./gradlew checkTestData

# Update mode - rewrites mismatched test data files
./gradlew updateTestData

# Run only golden tests (skip all variant-specific tests)
./gradlew updateTestData -Porg.jetbrains.kotlin.testDataManager.options.goldenOnly=true

# Incremental update — skip variant tests for unchanged golden paths
./gradlew updateTestData -Porg.jetbrains.kotlin.testDataManager.options.incremental=true
```

`checkTestData` verifies test data without writing; `updateTestData` rewrites mismatched files. Both take their options as `-P` properties — see the [test-data-manager convention README](../../repo/gradle-build-conventions/test-data-manager-convention/README.md) for the option list and details.

### Per-Module Execution

Run on a single module:

```bash
./gradlew :analysis:analysis-api-fir:updateTestData
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
