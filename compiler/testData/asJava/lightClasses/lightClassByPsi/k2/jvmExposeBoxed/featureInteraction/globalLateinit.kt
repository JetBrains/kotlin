// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed
// COMPILATION_ERRORS

@JvmInline
value class StringWrapper(val s: String)

lateinit var foo: StringWrapper
