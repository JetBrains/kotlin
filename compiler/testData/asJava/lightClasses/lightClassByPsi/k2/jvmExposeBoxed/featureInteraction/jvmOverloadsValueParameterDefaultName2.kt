// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
@JvmOverloads
fun foo(o: StringWrapper = StringWrapper("O"), k: String = "K"): String = ""

// LIGHT_ELEMENTS_NO_DECLARATION: JvmOverloadsValueParameterDefaultName2Kt.class[foo-JELJCFg;foo-d-auiwc], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]