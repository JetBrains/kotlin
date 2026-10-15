// pack.ValueClass
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
package pack

value class ValueClass(val first: Int, val second: Int) {
    override fun equals(other: Any?): Boolean = other is ValueClass && other.first == first
    override fun hashCode(): Int = first
    override fun toString(): String = "ValueClass"
}
