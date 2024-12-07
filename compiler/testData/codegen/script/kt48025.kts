// JVM_ABI_K1_K2_DIFF: KT-63960, KT-63963

val p = 0

class ReducedFraction() {
    fun plus1() = reducedFractionOf(p)
    val y = 1
}

fun reducedFractionOf(a: Int) {
}

val c = ReducedFraction()
val x = c.y
// expected: x: 1
