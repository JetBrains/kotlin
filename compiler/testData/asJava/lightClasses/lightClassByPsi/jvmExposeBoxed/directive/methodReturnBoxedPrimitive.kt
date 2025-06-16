// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class IntWrapper(val i: Int)

class Foo {
    fun foo(): IntWrapper = IntWrapper(0)
}
