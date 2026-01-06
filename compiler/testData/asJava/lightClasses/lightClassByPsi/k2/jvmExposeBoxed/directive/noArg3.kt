// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class StringWrapper(val s: String = "str")

@JvmInline
value class StringWrapperWrapper(val s1: StringWrapper = StringWrapper("str2"))

// LIGHT_ELEMENTS_NO_DECLARATION: StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], StringWrapperWrapper.class[constructor-impl;equals-impl;equals-impl0;getS1-K4fyztM;hashCode-impl;toString-impl]