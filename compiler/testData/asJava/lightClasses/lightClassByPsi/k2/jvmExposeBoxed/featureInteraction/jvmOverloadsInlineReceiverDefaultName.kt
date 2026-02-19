// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
@JvmOverloads
fun StringWrapper.foo(o: String = "O", k: StringWrapper = StringWrapper("K")): String = ""

// LIGHT_ELEMENTS_NO_DECLARATION: JvmOverloadsInlineReceiverDefaultNameKt.class[foo-JELJCFg;foo-d-auiwc;foo-wHlS-Gg], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]