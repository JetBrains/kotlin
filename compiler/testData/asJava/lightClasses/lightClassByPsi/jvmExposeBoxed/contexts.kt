// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ContextParameters
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Z(val value: String)

@JvmExposeBoxed
class A {
    context(o: Z)
    fun f(k: Z): String = o.value + k.value
}

// LIGHT_ELEMENTS_NO_DECLARATION: A.class[f-e_ggP3o], Z.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]