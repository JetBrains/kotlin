// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
class Foo {
    companion object {
        @JvmStatic
        var baz: StringWrapper
            get() = StringWrapper("OK")
            set(value) {

            }
    }
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: Foo.class[baz]
// LIGHT_ELEMENTS_NO_DECLARATION: Foo.class[getBaz-K4fyztM;getBaz-K4fyztM;setBaz-JELJCFg;setBaz-JELJCFg], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]