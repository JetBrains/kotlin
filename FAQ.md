Q: How do I run my program?

A: Define top level function `fun main(args: Array<String>)`, please ensure it's not
in a package. Also compiler switch `-entry` could be use to make any function taking
`Array<String>` and returning `Unit` be an entry point.


Q: What is Kotlin/Native memory management model?

A: Kotlin/Native provides automated memory management scheme, similar to what Java or Swift provides.
Current implementation includes automated reference counter with cycle collector to collect cyclical
garbage.


Q: How do I create shared library?

A: Use `-produce dynamic` compiler switch, or `konanArtifacts { dynamic('foo') {} }` in Gradle.
It will produce platform-specific shared object (.so on Linux, .dylib on macOS and .dll on Windows targets) and
C language header, allowing to use all public APIs available in your Kotlin/Native program from C/C++ code.
See `samples/python_extension` as an example of using such shared object to provide a bridge between Python and
Kotlin/Native.

Q: How do I create static library or an object file?

A: Use `-produce static` compiler switch, or `konanArtifacts { static('foo') {} }` in Gradle.
It will produce platform-specific static object (.a library format) and C language header, allowing to
use all public APIs available in your Kotlin/Native program from C/C++ code.

Q: How do I run Kotlin/Native behind corporate proxy?

A: As Kotlin/Native need to download platform specific toolchain, you need to specify
`-Dhttp.proxyHost=xxx -Dhttp.proxyPort=xxx` as compiler's or `gradlew` arguments,
or set it via `JAVA_OPTS` environment variable.

Q: How do I specify custom Objective-C prefix/name for my Kotlin framework?

A: Use `-module_name` compiler option or matching Gradle DSL statement, i.e.
```
framework("MyCustomFramework") {
    extraOpts '-module_name', 'TheName'
}
```
