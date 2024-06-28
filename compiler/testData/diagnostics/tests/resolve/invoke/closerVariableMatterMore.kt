// FIR_IDENTICAL
// ISSUE: KT-37375

fun takeDouble(x: Double) {}

val bar: Int = 1
operator fun Double.invoke(): Double = 1.0 // (1)

fun test_1() {
    val bar: Double = 2.0
    operator fun Int.invoke(): Int = 1  // (2)

    val res = bar() // should resolve to (1)
    takeDouble(res) // should be OK
}