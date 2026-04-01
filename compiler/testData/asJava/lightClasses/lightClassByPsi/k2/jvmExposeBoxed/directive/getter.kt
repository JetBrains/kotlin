// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class StringWrapper(val s: String) // This getter is exposed by default

@JvmInline
value class StringWrapper2(val s1: StringWrapper) // This is not

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed("create")
fun createWrapper(): StringWrapper2 = StringWrapper2(StringWrapper("OK"))

// LIGHT_ELEMENTS_NO_DECLARATION: GetterKt.class[create], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], StringWrapper2.class[constructor-impl;equals-impl;equals-impl0;getS1-K4fyztM;hashCode-impl;toString-impl]