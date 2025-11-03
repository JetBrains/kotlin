// WITH_STDLIB

private fun isPosZero(x: Float) = x == 0.0f && 1.0f / x == Float.POSITIVE_INFINITY
private fun isNegZero(x: Float) = x == 0.0f && 1.0f / x == Float.NEGATIVE_INFINITY

fun box(): String {
    val z1 = (-0.0f) / 3.0f
    val z2 = (+0.0f) / 3.0f
    if (!isNegZero(z1)) return "fail1"
    if (!isPosZero(z2)) return "fail2"
    return "OK"
}
