// WITH_STDLIB

private fun isNegZero(x: Double) = x == 0.0 && 1.0 / x == Double.NEGATIVE_INFINITY

fun box(): String {
    if (!((Double.NaN * 2.0).isNaN())) return "fail1"
    if (!((0.0 * Double.POSITIVE_INFINITY).isNaN())) return "fail2"
    if (!(((-0.0) * Double.NEGATIVE_INFINITY).isNaN())) return "fail3"

    if (!isNegZero((-0.0) * 2.0)) return "fail4"
    if (!isNegZero((+0.0) * (-2.0))) return "fail5"

    if (Double.POSITIVE_INFINITY * -1.0 != Double.NEGATIVE_INFINITY) return "fail6"
    return "OK"
}
