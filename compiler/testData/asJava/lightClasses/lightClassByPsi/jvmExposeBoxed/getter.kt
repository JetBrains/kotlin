// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(kotlin.ExperimentalStdlibApi::class)

@JvmInline
value class StringWrapper(val s: String) // This getter is exposed by default

@JvmExposeBoxed
@JvmInline
value class StringWrapper2(val s1: StringWrapper) // This is not

@JvmExposeBoxed("create")
fun createWrapper(): StringWrapper2 = StringWrapper2(StringWrapper("OK"))
