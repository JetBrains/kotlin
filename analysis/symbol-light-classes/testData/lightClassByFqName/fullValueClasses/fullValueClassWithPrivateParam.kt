// pack.ValueClass
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

package pack

class OriginalClass

value class ValueClass(private val value: OriginalClass, private val count: Int)
