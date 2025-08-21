// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@OptIn(ExperimentalStdlibApi::class)
@JvmInline
value class StringWrapper(val s: String)

suspend fun foo(sw: StringWrapper): String = sw.s
