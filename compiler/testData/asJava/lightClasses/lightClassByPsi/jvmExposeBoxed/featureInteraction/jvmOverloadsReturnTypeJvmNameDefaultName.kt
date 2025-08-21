// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class IntWrapper(val s: Int)

class Baz {
    @JvmOverloads
    @JvmName("jvmMemberLevel")
    @OptIn(ExperimentalStdlibApi::class)
    @JvmExposeBoxed
    fun memberLevel(o: Int = 0, k: Int = 1): IntWrapper = IntWrapper(o + k)
}
