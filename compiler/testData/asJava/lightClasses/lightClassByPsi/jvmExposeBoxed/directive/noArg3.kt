// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class StringWrapper(val s: String = "str")

@JvmInline
value class StringWrapperWrapper(val s1: StringWrapper = StringWrapper("str2"))
