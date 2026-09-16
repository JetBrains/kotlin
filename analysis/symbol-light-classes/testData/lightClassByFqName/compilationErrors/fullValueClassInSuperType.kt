// pack.TargetClass
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
package pack

value class ValueClass(val first: Int, val second: Int)

class TargetClass : ValueClass(1, 2)
