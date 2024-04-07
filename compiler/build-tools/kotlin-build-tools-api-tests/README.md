## Build Tools API integration tests

This module contains integration tests covering the build tools API implementation using the DSL
built on top of the [Build Tools API](../kotlin-build-tools-api/README.md).

#### How to run

To run all tests use the `check` task.

The module defines test suites using the `jvm-test-suite` plugin.

#### Test suites:

* Compatibility: a special test suit that runs against a set of implementation versions
    * Use `./gradlew :compiler:build-tools:kotlin-build-tools-api-tests:testCompatibility1.9.20`
      to run the tests against BTA implementation 1.9.20
    * Use `./gradlew :compiler:build-tools:kotlin-build-tools-api-tests:testCompatibilitySnapshot`
      to run the tests against the current BTA implementation
    * Avoid adding new tests here unless you can articulate their necessity as they will be executed multiple times significantly increasing
      the overall test execution time.
* Escapable characters: a special test suit that runs against classpath and module paths containing symbols that typically should be escaped (whitespaces, hashes, etc)
    * Use `./gradlew :compiler:build-tools:kotlin-build-tools-api-tests:testEscapableCharacters` to run them
* Example: provides examples of the DSL usage. Excluded from the `check` task
    * Use `./gradlew :compiler:build-tools:kotlin-build-tools-api-tests:testExample` to run them

#### How to work with the tests

Few rules you should follow while writing tests:

- All tests should be written using [JUnit 5 platform](https://junit.org/junit5/docs/current/user-guide/#overview).
- Add `@DisplayName(...)` with meaningful description both for test class and methods inside. This will allow developers easier
  to understand what test is about.
- Don't create one big test class. Consider splitting tests into smaller classes. All tests can run in parallel, thus having small tests
  classes should improve overall tests running time.
- Don't create one big test suit. Consider grouping test classes semantically into test suits. Adding a new test suit is as easy as adding
  an entry to `businessLogicTestSuits` in the [build.gradle.kts](./build.gradle.kts)
- All test classes should extend [BaseTest](./src/main/kotlin/BaseTest.kt)
- Set the `@TestMetadata(...)` annotation to provide convenient Intellij IDEA navigation to the test data. It does not support navigation to multiple test data locations, thus please put a link to at least one tested module

The rules specific to compilation tests:

- All the compilation test classes should extend [BaseCompilationTest](./src/main/kotlin/compilation/BaseCompilationTest.kt)
- Consider using the scenario DSL for the incremental compilation tests, a usage example is
  located [here](src/testExample/kotlin/ExampleIncrementalScenarioTest.kt)
- Mark the compilation test with the `DefaultStrategyAgnosticCompilationTest` annotation if the test is expected to perform exactly
  the same using the daemon or in-process compiler execution strategy.
- If you're writing a test for a specific strategy, consider configuring it manually
  through `CompilationService.makeCompilerExecutionStrategyConfiguration()`

#### The scenario DSL

The incremental compilation tests written using the scenario DSL are subject to some optimizations and automatic checks, allowing you to
avoid boilerplate.

Please refer to the example test [class](src/testExample/kotlin/ExampleIncrementalScenarioTest.kt) for more information

