// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
// IGNORE_FE10

package test

class SomeClass

fun <Outer> topLevel() {
    open class Base {
        fun withOuter(): Outer? = null
    }

    class Child : Base() {}

    Child().<caret>withOuter()
}