// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@OptIn(ExperimentalStdlibApi::class)
@JvmInline
value class StringWrapper @JvmExposeBoxed constructor(val s: String = "OK")

// LIGHT_ELEMENTS_NO_DECLARATION: StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]