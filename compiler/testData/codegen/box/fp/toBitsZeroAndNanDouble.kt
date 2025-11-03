// WITH_STDLIB

fun box(): String {
    if (0.0.toBits() == (-0.0).toBits()) return "fail1"
    if (Double.fromBits(0.0.toBits()) != 0.0) return "fail2"
    if (Double.fromBits((-0.0).toBits()) != -0.0) return "fail3"

    val n1 = Double.fromBits(0x7ff8_0000_0000_0001UL.toLong())
    val n2 = Double.fromBits(0x7ff8_0000_0000_0002UL.toLong())
    if (!n1.isNaN() || !n2.isNaN()) return "fail4"
    val r1 = Double.fromBits(n1.toBits())
    val r2 = Double.fromBits(n2.toBits())
    if (!r1.isNaN() || !r2.isNaN()) return "fail5"
    return "OK"
}
