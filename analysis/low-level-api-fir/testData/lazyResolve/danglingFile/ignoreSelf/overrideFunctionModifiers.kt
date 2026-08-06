// ISSUE: KT-77071
package one

open class Base {
    protected open operator infix fun times(other: Int): Int = 1
}

class Derived : Base() {
    override fun ti<caret>mes(other: Int): Int = 2
}
