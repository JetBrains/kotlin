// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class StringWrapper @JvmExposeBoxed constructor(val s: String)

@JvmExposeBoxed
class Test(val s: StringWrapper) {
    fun ok(): String = s.s
}
