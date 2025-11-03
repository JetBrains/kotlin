// WITH_STDLIB
import kotlin.math.*

private fun signOf(x: Double): Int {
    if (x.isNaN()) return 0
    if (x == 0.0) return if (1.0 / x == Double.NEGATIVE_INFINITY) -1 else 1
    return if (x > 0) 1 else -1
}

fun box(): String {
    val a = 1.234
    if (!((a % 0.0).isNaN())) return "fail1"
    if (!((a.mod(0.0)).isNaN())) return "fail2"

    val r1 = 10.125 % -0.5
    if (signOf(r1) != signOf(10.125)) return "fail3"

    val r2 = (-10.125) % 0.5
    if (signOf(r2) != signOf(-10.125)) return "fail4"

    val m1 = 10.125.mod(-0.5)
    if (signOf(m1) != signOf(-0.5)) return "fail5"
    val m2 = (-10.125).mod(0.5)
    if (signOf(m2) != signOf(0.5)) return "fail6"

    fun expectedMod(x: Double, y: Double): Double {
        val rem = x % y
        return if (signOf(rem) == signOf(y)) rem else rem + y
    }
    for ((x, y) in listOf(
        10.125 to 0.5, 10.125 to -0.5, -10.125 to 0.5, -10.125 to -0.5,
        7.3 to 0.4, -7.3 to 0.4, 7.3 to -0.4, -7.3 to -0.4
    )) {
        val got = x.mod(y)
        val exp = expectedMod(x, y)
        if (!(got == exp || (got.isNaN() && exp.isNaN())))
            return "fail"
    }
    return "OK"
}
