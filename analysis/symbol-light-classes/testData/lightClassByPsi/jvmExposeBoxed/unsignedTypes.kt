// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

// 'UInt' is covered by 'int.kt'; these are the remaining unsigned value classes.
@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
fun concat(b: UByte, s: UShort, l: ULong): String = "$b$s$l"

// LIGHT_ELEMENTS_NO_DECLARATION: UnsignedTypesKt.class[concat-rlAifOc]
