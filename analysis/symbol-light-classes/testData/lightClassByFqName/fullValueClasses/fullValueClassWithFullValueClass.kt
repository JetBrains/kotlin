// pack.ValueClass
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
package pack

class OriginalClass

value class AnotherValueClass(val original: OriginalClass, val count: Int)

value class ValueClass(val another: AnotherValueClass, val nullable: AnotherValueClass?)
