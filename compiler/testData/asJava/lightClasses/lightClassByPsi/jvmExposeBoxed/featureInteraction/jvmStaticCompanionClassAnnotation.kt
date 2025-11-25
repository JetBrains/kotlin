// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
class Foo {
    companion object {
        @JvmStatic
        fun foo(): StringWrapper = StringWrapper("OK")
    }
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: Foo.class[foo]
// LIGHT_ELEMENTS_NO_DECLARATION: Foo.class[Companion;foo-K4fyztM;foo-K4fyztM], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]