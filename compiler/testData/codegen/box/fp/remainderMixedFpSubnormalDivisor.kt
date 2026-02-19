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
    val tinyD = Double.MIN_VALUE
    val tinyF = Float.MIN_VALUE

    fun checkD(a: Long, b: Double): String? {
        val r = a % b
        if (r.isNaN()) return "fail1"
        if (r == 0.0) {
            if (signOf(r) != signOf(a.toDouble())) return "fail2"
        } else {
            if (!r.isFinite()) return "fail3"
            if (!(abs(r) < abs(b))) return "fail4"
        }
        return null
    }
    for (a in listOf(1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE)) {
        checkD(a, tinyD)?.let { return it }
    }

    fun checkF(a: Long, b: Float): String? {
        val r = a % b
        if (r.isNaN()) return "fail5"
        if (r == 0.0f) {
            val s = signOf(a.toFloat())
            val rs = if (isNegZeroF(r)) -1 else 1
            if (rs != s) return "fail6"
        } else {
            if (!r.isFinite()) return "fail7"
            if (!(abs(r) < abs(b))) return "fail8"
        }
        return null
    }
    for (a in listOf(1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE)) {
        checkF(a, tinyF)?.let { return it }
    }

    return "OK"
}