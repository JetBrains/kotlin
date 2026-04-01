// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1
data class P(val x: Int, val y: Int)

fun destruct(): Int {
    val (l, r) = P(1, 2)
    return l + r
}
