// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
@JvmOverloads
fun String.foo(o: String = "O", k: StringWrapper = StringWrapper("K")): String = ""

// LIGHT_ELEMENTS_NO_DECLARATION: JvmOverloadsRegularReceiverDefaultNameKt.class[foo-bCqDVWw], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]