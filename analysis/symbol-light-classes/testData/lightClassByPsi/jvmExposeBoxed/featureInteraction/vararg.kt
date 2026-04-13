// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// COMPILATION_ERRORS
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class StringWrapper(val s: String)

@JvmExposeBoxed
fun foo(vararg sw: StringWrapper) {

}

class Bar {
    @JvmExposeBoxed
    fun foo(vararg sw: StringWrapper) {

    }
}
