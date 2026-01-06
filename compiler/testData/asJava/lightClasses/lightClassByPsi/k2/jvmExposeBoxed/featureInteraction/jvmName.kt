// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
class Foo {
    @JvmExposeBoxed
    @JvmName("foo11")
    fun foo1(sw: StringWrapper): String = sw.s

    @JvmExposeBoxed("foo22")
    @JvmName("foo21")
    fun foo2(): StringWrapper = StringWrapper("OK")
}

// LIGHT_ELEMENTS_NO_DECLARATION: Foo.class[foo22], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]