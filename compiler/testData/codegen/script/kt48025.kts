// IGNORE_BACKEND_K2: JVM_IR

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
