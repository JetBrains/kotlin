// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed("bar")
@JvmOverloads
fun foo(o: String = "O", k: StringWrapper = StringWrapper("K")): String = ""

// DECLARATIONS_NO_LIGHT_ELEMENTS: JvmOverloadsValueParameterKt.class[foo]
// LIGHT_ELEMENTS_NO_DECLARATION: JvmOverloadsValueParameterKt.class[bar;bar;bar;foo-WwgAR2g], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]