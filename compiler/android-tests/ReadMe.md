# Codegen Tests on Android

The module prepares codegen tests for running
(`compiler/testData/codegen/box` and `compiler/testData/codegen/boxInline` directories) on an Android platform.
It achieves this by compiling all tests, except those explicitly excluded, into a single Android project.
The tests are then executed on an Android Virtual Device, leveraging the Kotlin Gradle Plugin integration tests
(`libraries/tools/kotlin-gradle-plugin-integration-tests`).
This testing environment is well-suited for building and running external Gradle projects.
Exclusions in the `CodegenTestsOnAndroidGenerator` typically include tests marked with `// IGNORE_BACKEND: ANDROID`,
tests that contain Java source files, or tests that use JVM features not supported by the Android Runtime.

To generate the project with tests, execute:
```
./gradlew :compiler:android-tests:generateAndroidTests
```

Run the according Gradle integration tests by executing:

```
./gradlew :kotlin-gradle-plugin-integration-tests:kgpAndroidCodegenTests
```
