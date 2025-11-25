// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed("create")
fun createUInt(): UInt = 1u

fun foo(u: UInt): Int = u.toInt()
// LIGHT_ELEMENTS_NO_DECLARATION: IntKt.class[create;foo-WZ4Q5Ns]