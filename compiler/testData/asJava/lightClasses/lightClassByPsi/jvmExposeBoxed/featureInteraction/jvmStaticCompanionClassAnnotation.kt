// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
class Foo {
    companion object {
        @JvmStatic
        fun foo(): StringWrapper = StringWrapper("OK")
    }
}
