---
name: make-tests-cacheable
description: "Guide for making a Gradle test task cacheable by removing `workingDir = rootDir` and fixing all resulting path resolution issues."
---

# Making Project Tests Cacheable

## Background

The convention in `project-tests-convention.gradle.kts` disables caching when `workingDir == rootDir`:

```kotlin
outputs.doNotCacheIf("`workingDir` shouldn't be set to `rootDir`") { workingDir == rootDir }
```

Many test modules set `workingDir = rootDir` because the test infrastructure resolves paths (test data, libraries, dist) relative to the repo root. Making tests cacheable requires removing this setting and ensuring all paths resolve correctly from the new CWD (the module's project directory by default).

Separately, `common-configuration.gradle.kts` has a `notCacheableTestProjects` list — remove the module from it.

## Step 0: Verify the tests pass locally

Run the tests and verify there are no pre-existing failures. Fix the issues if necessary.

## Step 1: Initial setup

* Remove `workingDir = rootDir`

## Step 1: Identify path resolution mechanisms

The test infrastructure resolves paths in two ways:

1. **System properties** (set via `with*()` DSL calls in `projectTests {}`): These pass absolute paths to the test JVM as `-D` flags. The test code reads them via `System.getProperty()`. These are CWD-independent and always work.

2. **Relative fallback paths** (hardcoded in `ForTestCompileRuntime.java`, `KtTestUtil.java`, `PathUtil.kt`, and test fixtures): Used when the corresponding system property is NOT set. These are relative to CWD and break when CWD changes from rootDir.

The goal is to ensure every path the test needs is provided via system properties (option 1).

## Step 2: Register runtime inputs via `with*()` calls

Check what the test infrastructure uses and add the corresponding DSL calls in the `projectTests {}` block. Common ones:

| What the test uses                            | DSL call needed                                                  | System property set                       |
|-----------------------------------------------|------------------------------------------------------------------|-------------------------------------------|
| `ForTestCompileRuntime.runtimeJarForTests()`  | `withJvmStdlibAndReflect()`                                      | `kotlin.full.stdlib.path`                 |
| `KtTestUtil.getAnnotationsJar()`              | `withMockJdkAnnotationsJar()`                                    | `kotlin.mockJDK.annotations.path`         |
| `KtTestUtil.findMockJdkRtJar()`               | `withMockJdkRuntime()`                                           | `kotlin.mockJDK.runtime.path`             |
| Plugin sandbox annotations                    | `withPluginSandboxAnnotations()`                                 | `firPluginAnnotations.{jvm,js,wasm}.path` |
| Plugin sandbox JAR                            | `withPluginSandboxJar()`                                         | `firPlugin.jar.path`                      |
| JS/WASM stdlib                                | `withJsRuntime()` / `withWasmRuntime()`                          | various                                   |

**How to find missing inputs:** Run the tests. Look for errors like:
```
plugins/plugin-sandbox/plugin-annotations/build/libs/plugin-annotations-wasm-js-2.4.255-SNAPSHOT.klib doesn't exist; property: firPluginAnnotations.wasm.path; distPath: plugins/plugin-sandbox/plugin-annotations/build/libs/plugin-annotations-wasm-js-2.4.255-SNAPSHOT.klib
```

It means that property `firPluginAnnotations.wasm.path` is not set. Check the property constant in `repo/gradle-build-conventions/project-tests-convention/src/main/kotlin/TestCompilePaths.kt` 
and find its usage in `TestCompilerRuntimeArgumentProvider`, for example:
```kotlin
ifNotEmpty(PLUGIN_SANDBOX_ANNOTATIONS_WASM_KLIB_PATH, pluginSandboxAnnotationsWasmKlib)
```

Now take a look at `project-tests-convention.gradle.kts` and find usage of `pluginSandboxAnnotationsWasmKlib`:
```kotlin
pluginSandboxAnnotationsWasmKlib.from(extension.pluginSandboxAnnotationsWasmKlib)
```

Now locate `pluginSandboxAnnotationsWasmKlib` in `ProjectTestsExtension` and find a `with*()` method that sets it:
```kotlin
fun withPluginSandboxAnnotations() {
    add(pluginSandboxAnnotationsJar) { project(":plugins:plugin-sandbox:plugin-annotations") }
    add(pluginSandboxAnnotationsJsKlib) { project(":plugins:plugin-sandbox:plugin-annotations", "jsRuntimeElements") }
    add(pluginSandboxAnnotationsWasmKlib) { project(":plugins:plugin-sandbox:plugin-annotations", "wasmJsRuntimeElements") }
}
```

**Conclusion**: you should add `withPluginSandboxAnnotations()` to the `projectTests {}` block.

If there is no such method, create it. It's possible that you will need to:
* add new configuration in `ProjectTestsExtension`
* add new file collection in `TestCompilerRuntimeArgumentProvider`
* add new property constant in `TestCompilePaths`
* set the property in `TestCompilerRuntimeArgumentProvider`

### Try to eliminate `withDist()`

`withDist()` declares the entire `dist/` directory (the full Kotlin compiler distribution) as a task input. This means **any change anywhere in the project** that affects `dist/` will invalidate the test cache — making it effectively useless. The `@OptIn(KotlinCompilerDistUsage::class)` annotation exists precisely as a warning.

**How to eliminate `withDist()`:** If the test infrastructure sets `kotlinHome` in `createCompilerArguments()` (e.g., `kotlinHome = ForTestCompileRuntime.distKotlincForTests().absolutePath`), simply remove that line. The compiler will attempt to auto-discover `dist/kotlinc/` via `PathUtil.kotlinPathsForCompiler`, but since `kotlin.dist.path` is not set and `dist/` doesn't exist relative to the module dir, the auto-discovery silently returns a non-existent path. As long as stdlib is already on the explicit classpath, this is harmless — the compiler uses the explicit classpath, not the auto-discovered one.

## Step 3: Register `testData` inputs

Generated tests use repo-root-relative paths like:
```java
runTest("plugins/my-plugin/my-plugin-tests/testData/someTest/");
```

The `transformTestDataPath()` method in `ForTestCompileRuntime` remaps these using the `KOTLIN_TESTDATA_ROOTS` system property. Register mappings via `testData()` in the `projectTests {}` block:

```kotlin
projectTests {
    testData(project.isolated, "testData")                          // this module's testData
    testData(project(":other:module").isolated, "testData")          // cross-project testData
}
```

**How to find missing mappings:** Run the tests. If a test fails with `FileNotFoundException` or `AssertionError` about a missing test data directory, check if the path starts with a prefix not covered by any `testData()` call. Look for constants like `TEST_DATA_DIR_PATH` in the test infrastructure to trace where paths come from.

### Try to minimize `testData()` scope

For example, if a test needs file `testData/moduleEmulation.js` from `:js:js.translator` project, it's better to register this exact file
instead of the whole `testData` driectory.

Don't do this:
```kotlin
testData(project(":js:js.translator").isolated, "testData")
```

Do this instead:
````kotlin
testData(project(":js:js.translator").isolated, "testData/moduleEmulation.js")
````

## Step 4: Remove explicit `dependsOn()` calls

Projects often declare explicit task dependencies for the `test` task, for example:
```kotlin
projectTests {
    testTask() {
        dependsOn(":plugins:plugin-sandbox:jar")
    }
}
```

Remove all such calls.

The task dependencies should be inferred from the registered inputs:
```kotlin
projectTests {
    withPluginSandboxJar()
}
```

Or from regular dependencies:
```kotlin
dependencies {
    testImplementation(project(":plugins:plugin-sandbox"))
}
```

## Step 5: Apply `test-inputs-check` plugin and configure security permissions

Add the plugin:
```kotlin
plugins {
    id("test-inputs-check")
}
```

Remove the module path from the `notCacheableTestProjects` list in `common-configuration.gradle.kts`.

The security manager restricts file access to declared inputs, build dir, and temp dir. Common extra permissions needed:

```kotlin
testInputsCheck {
    with(extraPermissions) {
        // If tests write system properties at runtime:
        add("""permission java.util.PropertyPermission "kotlin.incremental.compilation", "write";""")

        // If the compiler checks relative paths for synthetic/virtual source files
        // (e.g., plugin-generated files like AllOpenGenerated.kt):
        add("""permission java.io.FilePermission "${projectDir.absolutePath}/-", "read";""")
    }
}
```

**How to find missing permissions:** Run the tests. `java.security.AccessControlException` messages tell you exactly which permission is missing — the denied permission type, path, and action are all in the error message.

## Step 6: Verify

1. Run all tests in the module
2. Verify 0 failures across all test suites
3. Run again to confirm the task is `UP-TO-DATE` (cached)

## Reference implementation

Take a look at `:plugins:plugin-sandbox:plugin-sandbox-ic-test` as a reference
