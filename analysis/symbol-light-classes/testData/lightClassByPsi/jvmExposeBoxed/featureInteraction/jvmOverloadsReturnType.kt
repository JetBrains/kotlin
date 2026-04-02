// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed("bar")
@JvmOverloads
fun foo(o: String = "O", k: String = "K"): StringWrapper = StringWrapper(o + k)

// LIGHT_ELEMENTS_NO_DECLARATION: JvmOverloadsReturnTypeKt.class[bar;bar;bar], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]