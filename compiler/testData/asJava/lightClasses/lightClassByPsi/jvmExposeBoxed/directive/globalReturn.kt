// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class StringWrapper(val s: String)

// foo is unmangled and returns string
fun foo(): StringWrapper = StringWrapper("OK")
