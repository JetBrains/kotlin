// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

@JvmInline
value class StringWrapper(val s: String)

class Foo {
    fun foo(): StringWrapper = StringWrapper("OK")
}
// LIGHT_ELEMENTS_NO_DECLARATION: Foo.class[foo-K4fyztM], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]