// WITH_STDLIB

private fun isPosZero(x: Double) = x == 0.0 && 1.0 / x == Double.POSITIVE_INFINITY
private fun isNegZero(x: Double) = x == 0.0 && 1.0 / x == Double.NEGATIVE_INFINITY

fun box(): String {
    if (!((Double.NaN + 1.0).isNaN())) return "fail1"
    if (!((1.0 + Double.NaN).isNaN())) return "fail2"
    if (!((Double.POSITIVE_INFINITY + Double.NEGATIVE_INFINITY).isNaN())) return "fail3"

    if (!isNegZero((-0.0) + (-0.0))) return "fail4"
    if (!isPosZero((+0.0) + (-0.0))) return "fail5"
    if (!isPosZero(0.0 - 0.0)) return "fail6"

    if (0.0 + Double.MIN_VALUE != Double.MIN_VALUE) return "fail7"
    return "OK"
}
