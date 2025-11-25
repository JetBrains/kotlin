// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
@get:JvmExposeBoxed("bar")
val foo: StringWrapper get() = StringWrapper("str")

// LIGHT_ELEMENTS_NO_DECLARATION: GlobalGetterKt.class[bar], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]