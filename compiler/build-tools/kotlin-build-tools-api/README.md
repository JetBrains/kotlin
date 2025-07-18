## Build Tools API

This module contains public interfaces for Kotlin build tools. 
Using APIs from this module should be the preferred way to work with Kotlin compiler when integrating Kotlin builds into different build systems.
The Kotlin stdlib of at least Kotlin 1.4 is expected to be a dependency of a consumer of the API. 

The default implementation of the API is located in the [kotlin-build-tools-impl](../kotlin-build-tools-impl) directory.
Interfaces implementation are expected to be loaded using the [ServiceLoader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html).
The purpose of such a segregation is to allow using this API with different Kotlin compiler versions. 

# Generated files

This module generates files from compiler arguments descriptions located in `:compiler:arguments`.
When changing compiler arguments, please regenerate the generated files using
```
./gradlew :compiler:build-tools:kotlin-build-tools-api:generateBtaArguments
```

Please also run the test in `../kotlin-build-tools-api-tests/src/testConsistency/kotlin/GenConsistencyTest.kt` 
and update the expected hash in the test with values from the test result:

```
./gradlew :compiler:build-tools:kotlin-build-tools-api-tests:testConsistency --tests "GenConsistencyTest"
```
