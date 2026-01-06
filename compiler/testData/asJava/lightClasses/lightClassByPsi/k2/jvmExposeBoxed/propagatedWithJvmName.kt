// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class StringWrapper(val s: String)

@JvmExposeBoxed
class Implicit {
    @JvmName("foo11")
    fun foo1(sw: StringWrapper): Int = 42
}

// LIGHT_ELEMENTS_NO_DECLARATION: StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]