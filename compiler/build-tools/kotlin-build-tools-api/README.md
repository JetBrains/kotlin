## Build Tools API

This module contains public interfaces for Kotlin build tools. 
Using APIs from this module should be the preferred way to work with Kotlin compiler when integrating Kotlin builds into different build systems.
The Kotlin stdlib of at least Kotlin 1.4 is expected to be a dependency of a consumer of the API. 

The default implementation of the API is located in the [kotlin-build-tools-impl](../kotlin-build-tools-impl) directory.
It is recommended to load the implementation in an isolated classloader. For example:

```kotlin
val compilerImplClasspath = 
    arrayOf(
        "/path/to/build-tools-impl-2.3.0.jar",             // (1)
        // ...                                             // (2)
    )
val compilerClassloader = URLClassLoader(
    args.map { Path.of(it).toUri().toURL() }.toTypedArray(), 
    SharedApiClassesClassLoader()                          // (3)
)
val toolchains = KotlinToolchains.loadImplementation(compilerClassloader)
```

Please notice the following:
1. The `build-tools-impl` JAR file matches the compiler version that you want to use for compiling code.

   > For using the `build-tools-api` package Kotlin compiler versions older than 2.3.0, please see the [kotlin-build-tools-compat](../kotlin-build-tools-compat) module. 
2. You can put any other JARs required for compilation, e.g. compiler plugins, into the classpath.
3. `SharedApiClassesClassLoader` from the `build-tools-api` package must be used as the parent ClassLoader for the compiler implementation in order to share the necessary interfaces between API and implementation.
 
# Generated files

This module generates files from compiler arguments descriptions located in `:compiler:arguments`.

When changing compiler arguments, please regenerate the generated files using `./gradlew :compiler:build-tools:kotlin-build-tools-api:generateBtaArguments`

Please also remember to regenerate the `./gradlew :compiler:build-tools:kotlin-build-tools-api:apiDump`
