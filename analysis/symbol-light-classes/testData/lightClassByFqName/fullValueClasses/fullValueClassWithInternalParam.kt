// pack.ValueClass
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
package pack

class OriginalClass

value class ValueClass(internal val value: OriginalClass, internal val count: Int)
