// ISSUE: KT-77071
package one

open class Base {
    protected open val property: Int
        get() = 1
}

class Derived : Base() {
    override val prop<caret>erty: Int
        get() = 2
}
