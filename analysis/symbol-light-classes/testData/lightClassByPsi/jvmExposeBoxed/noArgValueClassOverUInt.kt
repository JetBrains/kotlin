// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

// The underlying type is itself a value class
@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
@JvmInline
value class Z(val a: UInt = 7u)

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
@JvmInline
value class P(val a: Int = 7)

// LIGHT_ELEMENTS_NO_DECLARATION: P.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], Z.class[constructor-impl;equals-impl;equals-impl0;getA-pVg5ArA;hashCode-impl;toString-impl]
