// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class IntWrapper(val i: Int)

class Foo {
    var foo: IntWrapper get() = IntWrapper(0)
        set(value) {

        }
}

// LIGHT_ELEMENTS_NO_DECLARATION: Foo.class[getFoo-7j0DjTs;setFoo-ej2fCWs], IntWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]