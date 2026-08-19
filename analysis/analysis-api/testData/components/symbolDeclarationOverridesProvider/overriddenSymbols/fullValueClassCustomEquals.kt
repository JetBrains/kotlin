// LANGUAGE: +FullValueClasses
// callable: test/FullValueClass.equals
package test

value class FullValueClass(val first: String, val second: String) {
    override fun equals(other: Any?): Boolean = other is FullValueClass && first == other.first

    override fun hashCode(): Int = first.hashCode()
}
