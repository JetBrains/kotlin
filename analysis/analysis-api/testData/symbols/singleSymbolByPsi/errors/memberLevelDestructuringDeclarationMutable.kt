// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
// IGNORE_FIR
// ^KT-70663

data class X(val a: Int, val b: Int)

class B {
    v<caret>ar (a, b) = X(1, 2)
}
