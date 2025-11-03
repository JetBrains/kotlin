// WITH_STDLIB
import kotlin.math.*

private fun signOf(x: Float): Int {
    if (x.isNaN()) return 0
    if (x == 0.0f) return if (1.0f / x == Float.NEGATIVE_INFINITY) -1 else 1
    return if (x > 0f) 1 else -1
}

fun box(): String {
    val a = 1.234f
    if (!((a % 0.0f).isNaN())) return "fail1"
    if (!((a.mod(0.0f)).isNaN())) return "fail2"

    val r1 = 10.125f % -0.5f
    if (signOf(r1) != signOf(10.125f)) return "fail3"
    val r2 = (-10.125f) % 0.5f
    if (signOf(r2) != signOf(-10.125f)) return "fail4"

    val m1 = 10.125f.mod(-0.5f)
    if (signOf(m1) != signOf(-0.5f)) return "fail5"
    val m2 = (-10.125f).mod(0.5f)
    if (signOf(m2) != signOf(0.5f)) return "fail6"

    fun expectedMod(x: Float, y: Float): Float {
        val rem = x % y
        return if (signOf(rem) == signOf(y)) rem else rem + y
    }
    for ((x, y) in listOf(
        10.125f to 0.5f, 10.125f to -0.5f, -10.125f to 0.5f, -10.125f to -0.5f,
        7.3f to 0.4f, -7.3f to 0.4f, 7.3f to -0.4f, -7.3f to -0.4f
    )) {
        val got = x.mod(y)
        val exp = expectedMod(x, y)
        if (!(got == exp || (got.isNaN() && exp.isNaN())))
            return "fail"
    }
    return "OK"
}
