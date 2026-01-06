// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed("bar")
@JvmOverloads
fun String.foo(o: String = "O", k: StringWrapper = StringWrapper("K")): String = ""

// DECLARATIONS_NO_LIGHT_ELEMENTS: JvmOverloadsRegularReceiverKt.class[foo]
// LIGHT_ELEMENTS_NO_DECLARATION: JvmOverloadsRegularReceiverKt.class[bar;bar;bar;foo-bCqDVWw], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]