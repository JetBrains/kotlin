// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class StringWrapper(val s: String)

class Foo {
    @set:JvmExposeBoxed
    @set:JvmName("setter")
    var foo: StringWrapper get() = StringWrapper("OK")
        set(value) {

        }
}

// LIGHT_ELEMENTS_NO_DECLARATION: Foo.class[getFoo-K4fyztM], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]