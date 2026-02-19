// WITH_STDLIB

private fun isNegZero(x: Float) = x == 0.0f && 1.0f / x == Float.NEGATIVE_INFINITY

fun box(): String {
    if (!((Float.NaN * 2.0f).isNaN())) return "fail1"
    if (!((0.0f * Float.POSITIVE_INFINITY).isNaN())) return "fail2"
    if (!(((-0.0f) * Float.NEGATIVE_INFINITY).isNaN())) return "fail3"

    if (!isNegZero((-0.0f) * 2.0f)) return "fail4"
    if (!isNegZero((+0.0f) * (-2.0f))) return "fail5"

    if (Float.POSITIVE_INFINITY * -1.0f != Float.NEGATIVE_INFINITY) return "fail6"
    return "OK"
}
