// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@OptIn(ExperimentalStdlibApi::class)
@JvmInline
@JvmExposeBoxed
value class StringWrapper(val s: String)

suspend fun foo(sw: StringWrapper): String = sw.s

// DECLARATIONS_NO_LIGHT_ELEMENTS: SuspendKt.class[foo]
// LIGHT_ELEMENTS_NO_DECLARATION: StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], SuspendKt.class[foo-d-auiwc]