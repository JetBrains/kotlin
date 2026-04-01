// WITH_STDLIB

fun box(): String {
    if (0.0f.toBits() == (-0.0f).toBits()) return "fail1"
    if (Float.fromBits(0.0f.toBits()) != 0.0f) return "fail2"
    if (Float.fromBits((-0.0f).toBits()) != -0.0f) return "fail3"

    val n1 = Float.fromBits(0x7fc0_0001)
    val n2 = Float.fromBits(0x7fc0_0002)
    if (!n1.isNaN() || !n2.isNaN()) return "fail4"
    val r1 = Float.fromBits(n1.toBits())
    val r2 = Float.fromBits(n2.toBits())
    if (!r1.isNaN() || !r2.isNaN()) return "fail5"

    return "OK"
}
