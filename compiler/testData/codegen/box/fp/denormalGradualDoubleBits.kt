// WITH_STDLIB

fun box(): String {
    val MIN_NORMAL_BITS = 0x0010_0000_0000_0000L
    val ZERO_BITS = 0x0L
    val NEG_ZERO_BITS = 1L shl 63

    if (Double.fromBits(ZERO_BITS + 1) != Double.MIN_VALUE) return "fail1"
    if (Double.fromBits(NEG_ZERO_BITS or 0x1L) != -Double.MIN_VALUE) return "fail2"

    val minNormal = Double.fromBits(MIN_NORMAL_BITS)
    val almost = Double.fromBits(MIN_NORMAL_BITS - 1)
    if (!(almost > 0.0)) return "fail3"
    if (Double.fromBits(almost.toBits() + 1) != minNormal) return "fail4"
    if (Double.fromBits(MIN_NORMAL_BITS - 1) != almost) return "fail5"

    return "OK"
}
