// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@OptIn(ExperimentalStdlibApi::class)
@JvmInline
value class StringWrapper @JvmExposeBoxed constructor(val s: String = "OK")
