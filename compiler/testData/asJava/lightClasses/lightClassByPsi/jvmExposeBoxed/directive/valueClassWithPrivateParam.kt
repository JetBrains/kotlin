// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

package pack

class OriginalClass

@JvmInline
value class ValueClass(private val value: OriginalClass)
