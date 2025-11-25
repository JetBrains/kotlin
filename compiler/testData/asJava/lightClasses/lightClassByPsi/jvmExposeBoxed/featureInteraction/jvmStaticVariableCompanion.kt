// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
class Foo {
    companion object {
        @JvmStatic
        var baz: StringWrapper
            @JvmExposeBoxed
            get() = StringWrapper("OK")
            @JvmExposeBoxed
            set(value) {

            }
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: Foo.class[Companion;getBaz-K4fyztM;getBaz-K4fyztM;setBaz-JELJCFg;setBaz-JELJCFg], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]