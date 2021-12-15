// WITH_STDLIB

import kotlin.math.*

fun almostEqual(a: Float, b: Float): Boolean = abs(a - b) < 0.0000001F

val umax = UInt.MAX_VALUE
val ulmax = ULong.MAX_VALUE
val imax = Int.MAX_VALUE
val lmax = Long.MAX_VALUE

fun box(): String {
    if (1F != 1.toFloat()) return "fail 1"
    if (1.0F != 1.0.toFloat()) return "fail 2"
    if (1e1F != 1e1.toFloat()) return "fail 3"
    if (1.0e1F != 1.0e1.toFloat()) return "fail 4"
    if (1e-1F != 1e-1.toFloat()) return "fail 5"
    if (1.0e-1F != 1.0e-1.toFloat()) return "fail 6"

    if (1f != 1.toFloat()) return "fail 7"
    if (1.0f != 1.0.toFloat()) return "fail 8"
    if (1e1f != 1e1.toFloat()) return "fail 9"
    if (1.0e1f != 1.0e1.toFloat()) return "fail 10"
    if (1e-1f != 1e-1.toFloat()) return "fail 11"
    if (1.0e-1f != 1.0e-1.toFloat()) return "fail 12"

    if (!almostEqual(kotlin.math.E.toFloat(), exp(1.0F))) return "fail 13"

    if (2147483647.toFloat() != imax.toFloat()) return "fail 14"

    if (9223372036854775807L.toFloat() != lmax.toFloat()) return "fail 15"

    if (0xFFFF_FFFF.toFloat() != umax.toFloat()) return "fail 16"

    if ((2.0f * Long.MAX_VALUE + 1) != ulmax.toFloat()) return "fail 17"

    return "OK"
}