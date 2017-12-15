Q: How do I run my program?

A: Define top level function `fun main(args: Array<String>)`, please ensure it's not
in a package. Also compiler switch `-entry` could be use to make any function taking
`Array<String>` and returning `Unit` be an entry point.


Q: What is Kotlin/Native memory management model?

A: Kotlin/Native provides automated memory management scheme, similar to what Java or Swift provides.
Current implementation includes automated reference counter with cycle collector to collect cyclical
garbage.


Q: How do I create shared library?

A: Use `-produce dynamic` compiler switch, or `konanArtifacts { dynamic {} }` in Gradle.
It will produce platform-specific shared object (.so on Linux, .dylib on macOS and .dll on Windows targets) and
C language header, allowing to use all public APIs available in your Kotlin/Native program from C code.
See `samples/python_extension` as an example of using such shared object to provide a bridge between Python and
Kotlin/Native.
