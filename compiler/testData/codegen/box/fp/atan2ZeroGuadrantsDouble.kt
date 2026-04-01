// WITH_STDLIB
import kotlin.math.atan2
import kotlin.math.PI

private fun isPosZero(d: Double) = d == 0.0 && 1.0 / d == Double.POSITIVE_INFINITY
private fun isNegZero(d: Double) = d == 0.0 && 1.0 / d == Double.NEGATIVE_INFINITY

fun box(): String {

    val a = atan2(+0.0, -1.0)
    val b = atan2(-0.0, -1.0)
    if (a != PI) return "fail1"
    if (b != -PI) return "fail2"

    val c = atan2(+0.0, +1.0)
    val d = atan2(-0.0, +1.0)
    if (!isPosZero(c)) return "fail3"
    if (!isNegZero(d)) return "fail4"


    val a0 = atan2(+0.0, -0.0)
    val b0 = atan2(-0.0, -0.0)
    if (a0 != PI) return "fail5"
    if (b0 != -PI) return "fail6"

    return "OK"
}
