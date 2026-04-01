// WITH_STDLIB

private fun isPosZero(x: Double) = x == 0.0 && 1.0 / x == Double.POSITIVE_INFINITY
private fun isNegZero(x: Double) = x == 0.0 && 1.0 / x == Double.NEGATIVE_INFINITY

fun box(): String {
    val z1 = (-0.0) / 3.0
    val z2 = (+0.0) / 3.0
    if (!isNegZero(z1)) return "fail1"
    if (!isPosZero(z2)) return "fail2"
    return "OK"
}
