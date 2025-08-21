// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class StringWrapper(val s: String) {
    companion object {
        @JvmStatic
        @JvmExposeBoxed
        fun unwrap(s: StringWrapper): String = s.s
    }
}
