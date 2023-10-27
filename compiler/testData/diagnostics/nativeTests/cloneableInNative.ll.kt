// LL_FIR_DIVERGENCE
// KT-62935
// LL_FIR_DIVERGENCE
// ISSUE: KT-58549

fun main() {
    val x: kotlin.Cloneable = if (true) intArrayOf(1) else longArrayOf(1)
    x
}