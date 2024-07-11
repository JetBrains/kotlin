// pack.ValueClass
// WITH_STDLIB
package pack

class OriginalClass

@JvmInline
value class ValueClass(internal val value: OriginalClass)
