// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed("bar")
@JvmOverloads
fun foo(o: StringWrapper = StringWrapper("O"), k: String = "K"): String = ""

// DECLARATIONS_NO_LIGHT_ELEMENTS: JvmOverloadsValueParameter2Kt.class[foo]
// LIGHT_ELEMENTS_NO_DECLARATION: JvmOverloadsValueParameter2Kt.class[bar;bar;bar;foo-JELJCFg;foo-d-auiwc], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]