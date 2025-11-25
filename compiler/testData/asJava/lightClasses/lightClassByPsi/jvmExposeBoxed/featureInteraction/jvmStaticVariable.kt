// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
object Foo {
    @get:JvmExposeBoxed
    @set:JvmExposeBoxed
    @JvmStatic
    var baz: StringWrapper get() = StringWrapper("OK")
        set(value) {

        }
}

// LIGHT_ELEMENTS_NO_DECLARATION: Foo.class[Foo;getBaz-K4fyztM;setBaz-JELJCFg], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]