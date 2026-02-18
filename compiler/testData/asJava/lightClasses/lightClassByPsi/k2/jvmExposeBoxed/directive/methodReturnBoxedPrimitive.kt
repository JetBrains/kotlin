// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class IntWrapper(val i: Int)

class Foo {
    fun foo(): IntWrapper = IntWrapper(0)
}

// LIGHT_ELEMENTS_NO_DECLARATION: Foo.class[foo-7j0DjTs], IntWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]