// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
class Foo {
    @JvmExposeBoxed
    companion object {
        @JvmStatic
        var baz: StringWrapper
            get() = StringWrapper("OK")
            set(value) {

            }
    }
}
