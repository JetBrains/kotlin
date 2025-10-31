// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class StringWrapper(val s: String)

@JvmExposeBoxed
class Implicit {
    @JvmName("foo11")
    fun foo1(sw: StringWrapper): Int = 42
}
