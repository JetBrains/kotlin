// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
package pack

interface MyInterface {
    val bar: Int
}

class Impl : MyInterface {
    override var b<caret>ar: Int = 0
}
