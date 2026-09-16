// pack.OtherClass
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
package pack

class OtherClass(val svc: SimpleValueClass)

value class SimpleValueClass(val value: Int, val other: Int)
