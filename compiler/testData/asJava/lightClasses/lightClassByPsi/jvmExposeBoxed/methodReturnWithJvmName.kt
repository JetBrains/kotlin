// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
class Foo {
    @JvmExposeBoxed("bar")
    @JvmName("foo")
    fun thenamedoesnotmatter(): StringWrapper = StringWrapper("OK")
}

// LIGHT_ELEMENTS_NO_DECLARATION: Foo.class[bar], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]