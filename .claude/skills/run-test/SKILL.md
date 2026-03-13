---
name: run-test
description: Running tests in the kotlin repository, use instead of direct `gradlew test`
---

# Why?
Kotlin repository uses a complicated internal infrastructure to run tests, instead of invoking `gradlew test` directly, use those recipes instead.

## Running a specific test
Use `.claude/skills/run-test/scripts/run_test_by_test_data_path.sh <test_data_path>` script to run related tests. 
Always save the output into a temporary file, as it is large and tests can take a long time to run.

The script will run all tests that is based on the test data path and output list of test reports.
Examine reports to understand which tests failed.
Example: `.claude/skills/run-test/scripts/run_test_by_test_data_path.sh compiler/testData/codegen/boxInline/anonymousObject/lambdaWithCapturedOuterVar.1.kt`

To update test data files (when expected output changes), pass the `--update-test-data` flag:
Example: `.claude/skills/run-test/scripts/run_test_by_test_data_path.sh compiler/testData/codegen/boxInline/anonymousObject/lambdaWithCapturedOuterVar.1.kt --update-test-data`



## Running all tests in the module
Use `.claude/skills/run-test/scripts/run_tests_in_module.sh <module_name> [extra_gradle_args...]` script to run all tests in the module.
Any arguments after the module name are forwarded to the Gradle invocation (e.g. `--tests` filters).

The script will run all tests that is based on the test data path and output list of test reports. 
Examine reports to understand which tests failed.
Example: `.claude/skills/run-test/scripts/run_tests_in_module.sh compiler:fir:analysis-tests`
Example with filter: `.claude/skills/run-test/scripts/run_tests_in_module.sh compiler:fir:analysis-tests --tests "org.jetbrains.kotlin.test.runners.ir.FirLightTree*"`
