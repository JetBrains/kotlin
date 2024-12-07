// pack.ValueClass
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package pack

class OriginalClass

@JvmInline
value class AnotherValueClass(val original: OriginalClass)

@JvmInline
value class ValueClass(val another: AnotherValueClass)
