// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class StringWrapper(val s: String)

var foo: StringWrapper
    get() = StringWrapper("str")
    set(value) {

    }
// LIGHT_ELEMENTS_NO_DECLARATION: GlobalVariableKt.class[setFoo-JELJCFg], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]