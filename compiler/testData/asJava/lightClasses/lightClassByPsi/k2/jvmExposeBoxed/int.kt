// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

@JvmExposeBoxed
fun foo(u: UInt): Int = u.toInt()

// LIGHT_ELEMENTS_NO_DECLARATION: IntKt.class[foo-WZ4Q5Ns]