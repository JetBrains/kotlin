// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

package one

@JvmInline
value class IntValue(val value: Int)

fun foo(vararg varargParam: String, valueParam: IntValue) = Unit

// LIGHT_ELEMENTS_NO_DECLARATION: IntValue.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], VarargAndValueClassKt.class[foo-fWO2PMw]