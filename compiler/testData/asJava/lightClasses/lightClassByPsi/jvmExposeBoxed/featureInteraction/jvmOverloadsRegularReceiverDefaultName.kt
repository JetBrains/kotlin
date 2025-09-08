// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
@JvmOverloads
fun String.foo(o: String = "O", k: StringWrapper = StringWrapper("K")): String = ""
