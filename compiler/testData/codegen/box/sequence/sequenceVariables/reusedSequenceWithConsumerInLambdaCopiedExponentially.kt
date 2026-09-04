// WITH_STDLIB
// TARGET_BACKEND: JVM
// Each sequence variable is read twice, once inside the lambda of the next one: copying the fused code must not take exponential space
fun box(): String {
    val s0 = sequenceOf(1)
    val s1 = s0.map { it + s0.first() }
    val s2 = s1.map { it + s1.first() }
    val s3 = s2.map { it + s2.first() }
    val s4 = s3.map { it + s3.first() }
    val s5 = s4.map { it + s4.first() }
    val s6 = s5.map { it + s5.first() }
    val s7 = s6.map { it + s6.first() }
    val s8 = s7.map { it + s7.first() }
    val s9 = s8.map { it + s8.first() }
    val s10 = s9.map { it + s9.first() }
    val s11 = s10.map { it + s10.first() }
    val s12 = s11.map { it + s11.first() }
    val s13 = s12.map { it + s12.first() }
    val s14 = s13.map { it + s13.first() }
    val s15 = s14.map { it + s14.first() }
    val s16 = s15.map { it + s15.first() }
    val s17 = s16.map { it + s16.first() }
    val s18 = s17.map { it + s17.first() }
    val s19 = s18.map { it + s18.first() }
    val s20 = s19.map { it + s19.first() }
    val s21 = s20.map { it + s20.first() }
    val s22 = s21.map { it + s21.first() }
    val s23 = s22.map { it + s22.first() }
    val s24 = s23.map { it + s23.first() }
    val s25 = s24.map { it + s24.first() }
    val s26 = s25.map { it + s25.first() }
    val s27 = s26.map { it + s26.first() }
    val s28 = s27.map { it + s27.first() }
    val s29 = s28.map { it + s28.first() }
    val s30 = s29.map { it + s29.first() }
    return if (s30.first() == 1 shl 30) "OK" else "Fail: ${s30.first()}"
}
