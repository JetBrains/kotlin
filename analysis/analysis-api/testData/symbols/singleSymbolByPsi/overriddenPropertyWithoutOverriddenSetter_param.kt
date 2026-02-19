// DO_NOT_CHECK_SYMBOL_RESTORE_K1
package pack

interface MyInterface {
    val bar: Int
}

class Impl(override var <caret>bar: Int) : MyInterface