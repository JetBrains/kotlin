// pack.ValueClass
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package pack

class OriginalClass

@JvmInline
value class ValueClass(internal val value: OriginalClass)
