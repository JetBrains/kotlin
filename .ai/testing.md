# Test Infrastructure

## Test Data Files

Tests use `.kt` files in `testData/` directories with special directive comments:

```kotlin
// FILE: fileName.kt           - Split test into multiple files
// MODULE: moduleName          - Define module boundaries
// MODULE: name(dep1, dep2)    - Module with dependencies

// Common directives for language features and compiler options
// LANGUAGE: +Feature          - Enable language feature
// API_VERSION: 1.9            - Set API version
// WITH_STDLIB                 - Include stdlib in compilation
```

## Test Generation
Tests are generated from abstract test runners on demand for most runners like diagnostics, box, etc.

For Analysis API, CLI tests and some others:

After adding test data:
1. Add test data file to appropriate `testData/` directory
2. Run `./gradlew generateTests`
3. New test methods appear in `*Generated.java` files in `tests-gen/`

## Grouped tests

Some test runners (those extending `AbstractTwoStageKotlinCompilerTest`, like the Native, Wasm and JS codegen box tests)
are run by `CompilerTestGroupingTestEngine`: every test is compiled alone, and then the tests of a class are linked
and executed in batches. A test whose settings or checks do not fit a batch is isolated by a `GroupingTestIsolator`
and runs alone.

- A test that fails in a batch is rerun alone, and reported by the result of the rerun. So a failure which only
  grouping causes does not fail the test: the only trace is a `Grouping engine: ...` line in the output.
  `-Pkotlin.test.grouping.engine.retry.failed.tests.isolated=false` turns the reruns off, to see such failures.
- Running a single test does not reproduce a failure of its batch: the batches are made of the selected tests only,
  so a single test is a batch of its own. Run the whole test class with the reruns turned off instead.
