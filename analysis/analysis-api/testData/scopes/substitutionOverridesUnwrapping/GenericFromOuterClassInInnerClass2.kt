// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
package test

class SomeClass

open class TopLevel<Outer> {
    open inner class Base<T> {
        fun noGeneric() {}
        fun withOuter(): Outer? = null
        fun withOwnAndOuter(t: T): Outer? = null
    }

    inner class <caret>Child : Base<SomeClass>()
}
