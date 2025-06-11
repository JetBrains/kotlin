// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class StringWrapper constructor(val s: String)

class Test(val s: StringWrapper) {
    fun ok(): String = s.s
}
