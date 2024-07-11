// pack.ValueClass
// WITH_STDLIB
package pack

class OriginalClass

@JvmInline
value class ValueClass(private val value: OriginalClass)
