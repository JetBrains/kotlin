// WITH_STDLIB
// IGNORE_BACKEND: JS_IR_ES6, JS_IR

private fun isNegZeroF(x: Float) = x == 0.0f && 1.0f / x == Float.NEGATIVE_INFINITY

fun box(): String {
    val ZERO_BITS = 0x00000000
    val NEG_ZERO_BITS = 0x80000000.toInt()
    val MIN_VALUE_BITS = 0x00000001
    val MIN_NORMAL_BITS = 0x00800000

    if (Float.fromBits(MIN_VALUE_BITS) != Float.MIN_VALUE) return "fail1"
    if (Float.fromBits(ZERO_BITS) != 0.0f) return "fail2"
    if (!isNegZeroF(Float.fromBits(NEG_ZERO_BITS))) return "fail3"

    if (Float.fromBits(ZERO_BITS + 1) != Float.MIN_VALUE) return "fail4"
    if (Float.fromBits(NEG_ZERO_BITS or 0x1) != -Float.MIN_VALUE) return "fail5"

    val minNormal = Float.fromBits(MIN_NORMAL_BITS)
    val almost = Float.fromBits(MIN_NORMAL_BITS - 1)
    if (!(almost > 0.0f)) return "fail6"
    if (Float.fromBits(almost.toBits() + 1) != minNormal) return "fail7"
    if (Float.fromBits(MIN_NORMAL_BITS - 1) != almost) return "fail8"

    return "OK"
}
