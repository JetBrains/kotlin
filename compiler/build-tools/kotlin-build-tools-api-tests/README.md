## Build Tools API integration tests

This module contains integration tests covering the build tools API implementation using the DSL 
built on top of the [Build Tools API](../kotlin-build-tools-api/README.md).

#### How to run

To run all tests for all Gradle plugins use `check` task.

The module defines test matrix using the `jvm-test-suite` plugin to cover different combinations of 
the Build Tools API's and its implementations' versions.

#### Test suites:
* `testSnapshotToSnapshot`: runs all tests against the API and implementation of the snapshot version. 
  * Use `./gradlew :compiler:build-tools:kotlin-build-tools-api-tests:testSnapshotToSnapshot` to run them
* `test1.9.20ToSnapshot`: runs all the tests marked with the `CompatibilityTests` annotation against API 1.9.20 and the snapshot implementation
  * Use `./gradlew :compiler:build-tools:kotlin-build-tools-api-tests:test1.9.20ToSnapshot` to run them
* `testSnapshotTo1.9.20`: runs all the tests marked with the `CompatibilityTests` annotation against the snapshot API and 1.9.20 implementation
    * Use `./gradlew :compiler:build-tools:kotlin-build-tools-api-tests:testSnapshotTo1.9.20` to run them

#### How to work with the tests

Few rules you should follow while writing tests:
- All tests should be written using [JUnit 5 platform](https://junit.org/junit5/docs/current/user-guide/#overview).
- All the compilation test classes should extend [BaseCompilationTest](./src/testCommon/kotlin/compilation/model/BaseCompilationTest.kt)
- Consider using the scenario DSL for the incremental compilation tests, an usage example is located [here](./src/testCommon/kotlin/compilation/ExampleIncrementalScenarioTest.kt)
- Add `@DisplayName(...)` with meaningful description both for test class and methods inside. This will allow developers easier
  to understand what test is about.
- Don't create one big test suite (class). Consider splitting tests into smaller suites. All tests are running in parallel (except daemon tests)
  and having small tests suites should improve overall tests running time.
- Mark the test with the `DefaultStrategyAgnosticCompilationTest` annotation if the test is expected to perform exactly 
  the same using the daemon or in-process compiler execution strategy. This way the test will be executed using both strategies.
- If you're writing a test for a specific strategy, consider configuring it manually through `CompilationService.makeCompilerExecutionStrategyConfiguration()` 

#### The scenario DSL

The incremental compilation tests written using the scenario DSL are subject to some optimizations and automatic checks, allowing you to avoid boilerplate.

* Creating a module (e.g. `val module1 = module("jvm-module-1")`), you already have this module compiled non-incrementally 
to apply further incremental changes. If this module is reused between different tests, the initial compilation output will be reused
instead of recompiling it again and again.
* Methods for applying modifications allow to perform automatic checks of resulting outputs files after the compilation, 
so you don't need to create assertions using `assertOutputs`

