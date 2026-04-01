## Build Tools API forward compatibility integration tests

This module contains integration tests covering the usage of an older Build Tools API (current major version - 1) with the current
BTA implementation.

#### How to run

To run all tests use the `check` task.

The module defines test suites using the `jvm-test-suite` plugin.

#### How to work with the tests

These tests are not meant to be a comprehensive integration test suite for the entire BTA feature set.

Instead, these tests should focus on verifying the BTA forward compatibility guarantee ("X+1"): 

```
BTA version X is guaranteed to work with major implementation versions [X-3, X+1].
```

That means, whenever there's a change in the BTA API that can potentially break compatibility, a test must be added to ensure this 
combination of API+IMPL continues to work.

Tests for backward compatibility ("X-3") are located in the `[kotlin-build-tools-api-tests](../kotlin-build-tools-api-tests)` project.
