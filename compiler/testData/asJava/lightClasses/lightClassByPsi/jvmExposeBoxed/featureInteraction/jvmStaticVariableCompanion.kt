// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
class Foo {
    companion object {
        @JvmStatic
        var baz: StringWrapper
            @JvmExposeBoxed
            get() = StringWrapper("OK")
            @JvmExposeBoxed
            set(value) {

            }
    }
}
