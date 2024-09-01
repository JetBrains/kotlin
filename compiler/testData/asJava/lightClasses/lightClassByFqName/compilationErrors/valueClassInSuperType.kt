// pack.TargetClass
// WITH_STDLIB
package pack

open class OriginalClass

@JvmInline
value class ValueClass(val original: OriginalClass)

class TargetClass : ValueClass(OriginalClass())