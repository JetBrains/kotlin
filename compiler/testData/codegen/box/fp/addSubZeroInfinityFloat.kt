// WITH_STDLIB

private fun isPosZero(x: Float) = x == 0.0f && 1.0f / x == Float.POSITIVE_INFINITY
private fun isNegZero(x: Float) = x == 0.0f && 1.0f / x == Float.NEGATIVE_INFINITY

fun box(): String {
    if (!((Float.NaN + 1.0f).isNaN())) return "fail1"
    if (!((1.0f + Float.NaN).isNaN())) return "fail2"
    if (!((Float.POSITIVE_INFINITY + Float.NEGATIVE_INFINITY).isNaN())) return "fail3"

    if (!isNegZero((-0.0f) + (-0.0f))) return "fail4"
    if (!isPosZero((+0.0f) + (-0.0f))) return "fail5"
    if (!isPosZero(0.0f - 0.0f)) return "fail6"

    if (0.0f + Float.MIN_VALUE != Float.MIN_VALUE) return "fail7"
    return "OK"
}
