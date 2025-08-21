// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

@JvmExposeBoxed
fun foo(u: UInt): Int = u.toInt()
