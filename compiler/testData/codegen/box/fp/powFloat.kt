// WITH_STDLIB
import kotlin.math.pow

private fun isNegInf(x: Float) = x == Float.NEGATIVE_INFINITY
private fun isPosInf(x: Float) = x == Float.POSITIVE_INFINITY

fun box(): String {
    if (!isPosInf(0.0f.pow(-1.0f))) return "fail1"
    if (!isNegInf((-0.0f).pow(-1.0f))) return "fail2"

    if (Float.NaN.pow(0.0f) != 1.0f) return "fail3"
    if (123.456f.pow(0.0f) != 1.0f) return "fail4"

    if (!isNegInf((-0.0f).pow(-3.0f))) return "fail5"
    if (!isPosInf((-0.0f).pow(-2.0f))) return "fail6"

    return "OK"
}
