# Generated files

This module generates files from compiler arguments descriptions located in `:compiler:arguments`.
When changing compiler arguments, please regenerate the generated files using
```
./gradlew :compiler:build-tools:kotlin-build-tools-impl:generateBtaArguments
```

Please also run the test in `../kotlin-build-tools-api-tests/src/testConsistency/kotlin/GenConsistencyTest.kt`
and update the expected hash in the test with values from the test result:

```
./gradlew :compiler:build-tools:kotlin-build-tools-api-tests:testConsistency --tests "GenConsistencyTest"
```
