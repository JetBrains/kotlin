// pack.ValueClass
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
package pack

value class ValueClass(val value: Int) {
    fun function(other: ValueClass): ValueClass = other
    val property: ValueClass get() = this
}
