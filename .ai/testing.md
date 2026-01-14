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

Tests are generated from abstract test runners. After adding test data:
1. Add test data file to appropriate `testData/` directory
2. Run `./gradlew generateTests`
3. New test methods appear in `*Generated.java` files in `tests-gen/`
