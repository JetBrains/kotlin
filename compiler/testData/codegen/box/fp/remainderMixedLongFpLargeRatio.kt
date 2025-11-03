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

    val big1 = (1L shl 62) + 1
    val big2 = (1L shl 61) - 1

    fun checkD(a: Long, b: Double): String? {
        val r = a % b
        if (r.isNaN()) return "fail1"
        if (r == 0.0 || isPosZero(r) || isNegZero(r)) return "fail2"
        if (signOf(r) != signOf(a.toDouble())) return "fail3"
        if (!(abs(r) < abs(b))) return "fail4"
        return null
    }
    for ((a,b) in listOf(
        big1 to 3.0, big1 to -3.0, big2 to 5.0, -big2 to 5.0,
    )) checkD(a,b)?.let { return it }

    fun checkF(a: Long, b: Float): String? {
        val r = a % b
        if (r.isNaN()) return "fail5"
        if (r == 0.0f || isPosZeroF(r) || isNegZeroF(r)) return "fail6"
        if (signOf(r) != signOf(a.toFloat())) return "fail7"
        if (!(abs(r) < abs(b))) return "fail88"
        return null
    }
    for ((a,b) in listOf(
        big1 to 3.0f, big1 to -3.0f, big2 to 5.0f, -big2 to 5.0f,
    )) checkF(a,b)?.let { return it }

    return "OK"
}
