// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
// DO_NOT_CHECK_SYMBOL_RESTORE_K1
data class P(val x: Int, val y: Int)

fun destruct(): Int {
    val (l, r) = P(1, 2)
    return l + r
}
