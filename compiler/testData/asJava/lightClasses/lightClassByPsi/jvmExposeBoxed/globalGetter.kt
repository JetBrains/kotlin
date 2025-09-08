// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
@get:JvmExposeBoxed("bar")
val foo: StringWrapper get() = StringWrapper("str")
