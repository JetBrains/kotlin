// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

package one

@JvmInline
value class IntValue(val value: Int)

fun foo(vararg varargParam: String, valueParam: IntValue) = Unit
