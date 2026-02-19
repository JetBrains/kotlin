// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// COMPILATION_ERRORS

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
class Foo {
    companion object {
        @JvmField
        var baz: StringWrapper = StringWrapper("OK")
    }
}
