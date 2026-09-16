// pack.ValueClass
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
package pack

value class ValueClass(val first: Int = 0, val second: String = "") {
    fun function(first: Int = 0, second: ValueClass = this) {}
}
