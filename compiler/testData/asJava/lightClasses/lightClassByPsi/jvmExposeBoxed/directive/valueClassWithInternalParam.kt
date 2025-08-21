// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

package pack

class OriginalClass

@JvmInline
value class ValueClass(internal val value: OriginalClass)
