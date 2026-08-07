// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@OptIn(ExperimentalStdlibApi::class)
@JvmInline
@JvmExposeBoxed
value class Box<T>(val value: T)

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
fun <T> roundTrip(b: Box<T>): Box<T> = b

// LIGHT_ELEMENTS_NO_DECLARATION: Box.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], GenericValueClassKt.class[roundTrip-RbRCt1M]
