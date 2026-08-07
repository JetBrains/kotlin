// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@OptIn(ExperimentalStdlibApi::class)
@JvmInline
@JvmExposeBoxed
value class Inner(val s: String)

@OptIn(ExperimentalStdlibApi::class)
@JvmInline
@JvmExposeBoxed
value class Outer(val inner: Inner)

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
fun unwrap(outer: Outer): String = outer.inner.s

// LIGHT_ELEMENTS_NO_DECLARATION: Inner.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], NestedValueClassKt.class[unwrap-53ZlloE], Outer.class[constructor-impl;equals-impl;equals-impl0;getInner-kBA5sJY;hashCode-impl;toString-impl]
