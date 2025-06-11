// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
object Foo {
    @JvmExposeBoxed
    @JvmStatic
    fun foo(): StringWrapper = StringWrapper("OK")
}
