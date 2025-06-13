// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

class Baz {
    @OptIn(ExperimentalStdlibApi::class)
    @JvmExposeBoxed
    @JvmOverloads
    fun foo(o: String = "O", k: String = "K"): StringWrapper = StringWrapper(o + k)
}
