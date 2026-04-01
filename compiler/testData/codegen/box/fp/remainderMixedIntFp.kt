// WITH_STDLIB
import kotlin.math.abs

private fun isPosZero(x: Double) = x == 0.0 && 1.0 / x == Double.POSITIVE_INFINITY
private fun isNegZero(x: Double) = x == 0.0 && 1.0 / x == Double.NEGATIVE_INFINITY
private fun isPosZeroF(x: Float) = x == 0.0f && 1.0f / x == Float.POSITIVE_INFINITY
private fun isNegZeroF(x: Float) = x == 0.0f && 1.0f / x == Float.NEGATIVE_INFINITY

private fun signOf(x: Double) = when {
    x.isNaN() -> 0
    x == 0.0 -> if (isNegZero(x)) -1 else 1
    x > 0 -> 1
    else -> -1
}
private fun signOf(x: Float) = when {
    x.isNaN() -> 0
    x == 0.0f -> if (isNegZeroF(x)) -1 else 1
    x > 0f -> 1
    else -> -1
}

fun box(): String {
    run {
        val r1 = 12 % 3.0
        if (!(r1 == 0.0 && isPosZero(r1))) return "fail1"
        val r2 = (-12) % 3.0
        if (!(r2 == 0.0 && isNegZero(r2))) return "fail2"

        val r3 = 12 % 3.0f
        if (!(r3 == 0.0f && isPosZeroF(r3))) return "fail3"
        val r4 = (-12) % 3.0f
        if (!(r4 == 0.0f && isNegZeroF(r4))) return "fail4"
    }

    fun checkD(a: Int, b: Double): String? {
        val r = a % b
        if (r.isNaN()) return "fail5"
        if (r == 0.0 || isPosZero(r) || isNegZero(r)) return "fail6"
        if (signOf(r) != signOf(a.toDouble())) return "fail7"
        if (!(abs(r) < abs(b))) return "fail8"
        return null
    }
    listOf(
        Int.MAX_VALUE to 3.0, Int.MAX_VALUE to -3.0,
        Int.MIN_VALUE to 3.0, Int.MIN_VALUE to -3.0
    ).forEach { (a,b) -> checkD(a,b)?.let { return it } }

    fun checkF(a: Int, b: Float): String? {
        val r = a % b
        if (r.isNaN()) return "fail9"
        if (r == 0.0f || isPosZeroF(r) || isNegZeroF(r)) return "fail10"
        if (signOf(r) != signOf(a.toFloat())) return "fail11"
        if (!(abs(r) < abs(b))) return "fail12"
        return null
    }
    listOf(
        Int.MAX_VALUE to 3.0f, Int.MAX_VALUE to -3.0f,
        Int.MIN_VALUE to 3.0f, Int.MIN_VALUE to -3.0f
    ).forEach { (a,b) -> checkF(a,b)?.let { return it } }

    return "OK"
}
