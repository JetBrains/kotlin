## Build Tools API compat module

This module contains a compatibility layer for Kotlin build tools API 2.3.0+ (using the `KotlinToolchains` class as the entry point) 
for use with compiler versions older than 2.3.0.

Include this module on the classpath together with the build tools implementation if you're going to use the new API with older compilers. 
It is recommended to load the implementation (and this `compat` package) in an isolated classloader. For example:

```kotlin
val compilerImplClasspath = 
    arrayOf(
        "/path/to/build-tools-impl-2.1.21.jar",             // (1)
        "/path/to/build-tools-compat-2.3.0.jar",            // (2)
    )
val compilerClassloader = URLClassLoader(
    args.map { Path.of(it).toUri().toURL() }.toTypedArray(), 
    SharedApiClassesClassLoader()                           // (3)
)
val toolchains = KotlinToolchains.loadImplementation(compilerClassloader)
```

Please notice the following:
1. The `build-tools-impl` JAR file matches the compiler version that you want to use for compiling code.
2. The `build-tools-compat` JAR file matches the `build-tools-api` version that you are using in your build system/tooling (usually the newest available).
3. `SharedApiClassesClassLoader` from the `build-tools-api` package must be used as the parent ClassLoader for the compiler implementation in order to share the necessary interfaces between API and implementation.

# Generated files

This module generates files from compiler arguments descriptions located in `:compiler:arguments`.

When changing compiler arguments,
please regenerate the generated files using `./gradlew :compiler:build-tools:kotlin-build-tools-compat:generateBtaArguments`