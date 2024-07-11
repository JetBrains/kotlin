// pack.ValueClass
// WITH_STDLIB
package pack

class OriginalClass

@JvmInline
value class AnotherValueClass(val original: OriginalClass)

@JvmInline
value class ValueClass(val another: AnotherValueClass)
