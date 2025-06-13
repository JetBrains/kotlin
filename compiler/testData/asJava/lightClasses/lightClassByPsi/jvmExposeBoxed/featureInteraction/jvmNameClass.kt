// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class StringWrapper(val s: String)

@JvmExposeBoxed
class Foo {
    @JvmName("foo11")
    fun foo1(sw: StringWrapper): String = sw.s

    @JvmExposeBoxed("foo22")
    @JvmName("foo21")
    fun foo2(): StringWrapper = StringWrapper("OK")
}
