# Codegen tests on Android

This module runs codegen box tests on Android. It discovers eligible tests from:

- `compiler/testData/codegen/box`
- `compiler/testData/codegen/boxJvm`
- `compiler/testData/codegen/boxInline`

The runner compiles the discovered tests into a temporary Android project, builds and installs APKs by flavor, and then
runs the tests on an emulator. The emulator and SDK are provisioned on the first run. See `CodegenTestsOnAndroidGenerator`
for the exact exclusions; the main ones are tests annotated with `// IGNORE_BACKEND: ANDROID`, tests with Java source
files, and tests that use unsupported Kotlin/JVM features.

Run the tests via Gradle:

```bash
./gradlew :compiler:android-tests:test
```

The JUnit report is split into phases:

- `Discovery`: finds Android-compatible box tests.
- `Compilation`: compiles discovered tests before the emulator is started.
- `Emulator`: creates and starts the Android emulator.
- `Runtime`: builds, installs, and runs tests grouped by Android flavor.

## Useful properties

| Property                                      | Description |
|-----------------------------------------------|-------------|
| `kotlin.test.android.path.filter`             | Runs only tests whose file path contains this substring. The value is not a regular expression. |
| `kotlin.test.android.compilation.parallelism` | Limits concurrent test compilation. By default, the runner uses up to 4 workers, capped by available processors. Values lower than 1 are coerced to 1. |
| `kotlin.test.android.teamcity`                | Enables CI behavior outside TeamCity. This disables emulator hardware acceleration and adds extra stabilization delay before install. `-Pteamcity=true` has the same effect. |
| `kotlin.test.android.avd.systemImage`         | Overrides the Android system image package passed to `avdmanager` when creating the AVD. The image must be available in the provisioned SDK. |

Run a small subset by path:

```bash
./gradlew :compiler:android-tests:test -Pkotlin.test.android.path.filter=dontReify
```

Run one file and force sequential compilation:

```bash
./gradlew :compiler:android-tests:test \
  -Pkotlin.test.android.path.filter=arrayWrite.kt \
  -Pkotlin.test.android.compilation.parallelism=1
```

Increase compilation parallelism on a machine with enough memory (default is 4):

```bash
./gradlew :compiler:android-tests:test -Pkotlin.test.android.compilation.parallelism=8
```

On TeamCity we run emulator without hardware acceleration.
Force TeamCity-like emulator behavior locally:

```bash
./gradlew :compiler:android-tests:test -Pkotlin.test.android.teamcity=true
```

Use a specific Android system image:

```bash
./gradlew :compiler:android-tests:test \
  "-Pkotlin.test.android.avd.systemImage=system-images;android-26;default;x86_64"
```
