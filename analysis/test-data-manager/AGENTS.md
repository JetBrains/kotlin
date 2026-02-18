# Test Data Manager - Agent Guidelines

Automated system for managing test data files across multiple test configurations.

## Module Overview

This module provides infrastructure for:
- Comparing test outputs with expected files using variant chains
- Automatic file management (creation, update, redundancy removal)
- Test discovery, grouping, and conflict detection

**Structure:**
- `testFixtures/` — Runtime API for use by other modules
- `tests/` — Module's own test suite

For conceptual details (variant chains, conflicts, convergence), see [README.md](README.md).
For running test data management tasks (checking/updating test data via Gradle), see [test-data-manager-convention](../../repo/gradle-build-conventions/test-data-manager-convention/README.md).

## Testing Guidelines (for tests within this module)

### Core Principles

1. **Readable multi-line string expectations** — Format results as human-readable strings, compare with `assertEquals`
2. **Custom formatters** — Create formatters that produce deterministic, readable output
3. **Domain-specific assertion helpers** — Encapsulate complex assertions in named functions
4. **Descriptive test names** — Use backticks with clear descriptions

### Testing Patterns

#### Pattern 1: Readable Output Formatting

Create formatters that produce deterministic, human-readable output for complex results.

From `TestDiscoveryAndGroupingIntegrationTest.kt`:

```kotlin
private fun formatResult(result: GroupingResult): String = buildString {
    for (group in result.groups) {
        val header = if (group.variantDepth == 0) "Group 0 (golden)" else "Group ${group.variantDepth}"
        appendLine("=== $header ===")
        for (test in group.tests.sortedBy { it.displayName }) {
            appendLine("${test.displayName} -> ${test.variantChain}")
        }
        appendLine()
    }
}.trimEnd()

@Test
fun `discovery finds all tests`() {
    val result = runDiscovery()
    assertEquals(expected.trimIndent(), formatResult(result))
}
```

#### Pattern 2: Domain-Specific Assertions

Encapsulate complex assertions in helper functions with clear names.

From `TestDataManagerGroupingTest.kt`:

```kotlin
private fun assertGrouping(tests: List<DiscoveredTest>, expected: String) {
    val result = groupByVariantDepth(tests)
    val actual = result.groups.joinToString("\n") { group ->
        "depth=${group.variantDepth}: ${group.uniqueVariantChains.joinToString(", ")}"
    }
    assertEquals(expected.trimIndent(), actual)
}

private fun assertConflicts(tests: List<DiscoveredTest>, expected: String) {
    val conflicts = validateConflicts(tests)
    val actual = conflicts.joinToString("\n") {
        "${it.chainA} vs ${it.chainB}: '${it.conflictingVariant}'"
    }
    assertEquals(expected.trimIndent(), actual)
}

@Test
fun `tests grouped by variant depth`() {
    assertGrouping(
        tests = listOf(
            DiscoveredTest("1", "golden", emptyList()),
            DiscoveredTest("2", "js", listOf("js")),
        ),
        expected = """
            depth=0: []
            depth=1: [js]
        """
    )
}
```

#### Pattern 3: State-Based Testing with Setup/Assert Helpers

For file-based operations, use setup and assertion helpers.

From `ManagedTestAssertionsTest.kt`:

```kotlin
private fun assertFileState(expected: String) {
    val actual = listOf("test.txt", "test.js.txt").mapNotNull { name ->
        val file = tempDir.resolve(name)
        if (file.exists()) "$name: ${file.readText().trim()}" else null
    }.joinToString("\n")
    assertEquals(expected.trimIndent(), actual)
}

private fun setupFiles(vararg files: Pair<String, String>) {
    for ((name, content) in files) {
        tempDir.resolve(name).writeText("$content\n")
    }
}

@Test
fun `UPDATE mode - mismatch updates file`() {
    setupFiles("test.txt" to "old")
    runAssertion(variantChain = emptyList(), actual = "new")
    assertFileState("test.txt: new")
}
```

#### Pattern 4: Filter Testing with Base Class

For JUnit filter tests, extend `AbstractPostDiscoveryFilterTest`.

From `ManagedTestFilterTest.kt`:

```kotlin
internal class ManagedTestFilterTest : AbstractPostDiscoveryFilterTest() {
    @Test
    fun `ClassSource with ManagedTest is included`() {
        assertIncluded(
            filter = ManagedTestFilter,
            descriptor = descriptorFromClass<FakeGoldenAnalysisApiTestGenerated>(),
        )
    }

    @Test
    fun `ClassSource without ManagedTest is excluded`() {
        assertExcluded(
            filter = ManagedTestFilter,
            descriptor = descriptorFromClass<NoMetadataClass>(),
        )
    }
}
```

Available utilities from `AbstractPostDiscoveryFilterTest`:
- `assertIncluded(filter, descriptor)` / `assertExcluded(filter, descriptor)`
- `descriptorFromClass<T>()` — Create descriptor from class
- `descriptorFromMethod(method)` — Create descriptor from method reference
- `descriptorWithSource(source)` — Create descriptor with custom source

#### Pattern 5: Fake Test Classes for Integration Testing

Create fake test classes in `tests/.../fakes/` to simulate real test configurations.

```kotlin
// Base class for all fakes
abstract class FakeManagedTest : ManagedTest

// Golden test (no variant)
@TestMetadata("testData/analysis/api")
class FakeGoldenAnalysisApiTestGenerated : FakeManagedTest() {
    override val variantChain = emptyList<String>()

    @Test
    @TestMetadata("symbols.kt")
    fun testSymbols() {}
}

// Multi-level variant test
@TestMetadata("testData/lightClasses")
class FakeWasmLightClassesTestGenerated : FakeManagedTest() {
    override val variantChain = listOf("knm", "wasm")

    @Test
    @TestMetadata("simple.kt")
    fun testSimple() {}
}
```

## Usage from Other Modules

### Implementing ManagedTest

Implement `ManagedTest` interface and provide variant chain:

```kotlin
abstract class MyTestBase : ManagedTest {
    override val variantChain: List<String>
        get() = emptyList()
}
```

Variant chain rules:
- `[]` (empty) — Golden/default configuration, writes to `.txt`
- `["js"]` — Single variant, writes to `.js.txt`
- `["knm", "wasm"]` — Multi-level variant, writes to `.wasm.txt` (last element only)

### Using Assertions

Use the extension function `ManagedTest.assertEqualsToTestDataFile()`  for comparing test output:

```kotlin
class MyTest : ManagedTest {
    override val variantChain = listOf("js")

    fun runTest(testDataFile: File) {
        val actual = computeResult()
        assertEqualsToTestDataFile(
            testDataPath = testDataFile.toPath(),
            actual = actual,
            extension = ".txt",
        )
    }
}
```

Or use `ManagedTestAssertions.assertEqualsToTestDataFile()` directly:

```kotlin
ManagedTestAssertions.assertEqualsToTestDataFile(
    testDataPath = testDataFile.toPath(),
    actual = actualContent,
    variantChain = variantChain,
    extension = ".txt",
)
```


### Behavior Matrix

| Scenario                 | UPDATE mode | CHECK mode (local) | CHECK mode (CI) |
|--------------------------|-------------|--------------------|-----------------|
| File missing (golden)    | Create      | Create + throw     | Throw           |
| File missing (secondary) | Throw       | Throw              | Throw           |
| Content matches          | Pass        | Pass               | Pass            |
| Write-target redundant   | Delete      | Delete + throw     | Throw           |
| Content mismatch         | Update      | Throw              | Throw           |

## Key Classes Reference

| Class                    | Location             | Purpose                                      |
|--------------------------|----------------------|----------------------------------------------|
| `ManagedTest`            | testFixtures         | Interface for tests managed by the system    |
| `ManagedTestAssertions`  | testFixtures         | Assertion functions for test data comparison |
| `TestDataManagerRunner`  | testFixtures         | Main runner (discovery, grouping, execution) |
| `TestDataFiles`          | testFixtures         | File path resolution for variant chains      |
| `ManagedTestFilter`      | testFixtures/filters | JUnit filter for ManagedTest implementations |
| `TestMetadataFilter`     | testFixtures/filters | JUnit filter by @TestMetadata paths          |
| `VariantChainComparator` | testFixtures         | Orders variant chains by depth               |

