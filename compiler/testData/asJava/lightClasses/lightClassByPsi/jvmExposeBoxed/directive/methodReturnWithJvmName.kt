// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class StringWrapper(val s: String)

class Foo {
    @JvmName("foo")
    fun thenamedoesnotmatter(): StringWrapper = StringWrapper("OK")
}
