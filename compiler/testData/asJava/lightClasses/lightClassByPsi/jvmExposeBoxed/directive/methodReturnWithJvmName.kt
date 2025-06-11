// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class StringWrapper(val s: String)

class Foo {
    // Because of @JvmName, no exposed method can be generated, since it is unmangled
    @JvmName("foo")
    fun thenamedoesnotmatter(): StringWrapper = StringWrapper("OK")
}
