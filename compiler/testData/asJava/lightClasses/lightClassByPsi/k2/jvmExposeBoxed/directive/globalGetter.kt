// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class StringWrapper(val s: String)

val foo: StringWrapper get() = StringWrapper("str")

// LIGHT_ELEMENTS_NO_DECLARATION: StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]