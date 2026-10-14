// pack.ValueClass
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
package pack

abstract value class Base(parameter: Int) {
    final override fun toString(): String = "Base"
}

value class ValueClass(val first: Int, val second: Int) : Base(first)
