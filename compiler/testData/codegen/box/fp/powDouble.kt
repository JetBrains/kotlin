// WITH_STDLIB
import kotlin.math.pow

private fun isNegInf(x: Double) = x == Double.NEGATIVE_INFINITY
private fun isPosInf(x: Double) = x == Double.POSITIVE_INFINITY

fun box(): String {
    if (!isPosInf(0.0.pow(-1.0))) return "fail1"
    if (!isNegInf((-0.0).pow(-1.0))) return "fail2"

    if (Double.NaN.pow(0.0) != 1.0) return "fail3"
    if (123.456.pow(0.0) != 1.0) return "fail4"

    if (!isNegInf((-0.0).pow(-3.0))) return "fail5"
    if (!isPosInf((-0.0).pow(-2.0))) return "fail6"

    return "OK"
}
