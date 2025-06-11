// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class StringWrapper(val s: String) // This getter is exposed by default

@JvmInline
value class StringWrapper2(val s1: StringWrapper) // This is not

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed("create")
fun createWrapper(): StringWrapper2 = StringWrapper2(StringWrapper("OK"))
