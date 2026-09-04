// WITH_STDLIB
// Each sequence variable refers to the previous one twice: gathering the sequence data must not take exponential time
fun box(): String {
    val s0 = sequenceOf(1)
    val s1 = s0 + s0
    val s2 = s1 + s1
    val s3 = s2 + s2
    val s4 = s3 + s3
    val s5 = s4 + s4
    val s6 = s5 + s5
    val s7 = s6 + s6
    val s8 = s7 + s7
    val s9 = s8 + s8
    val s10 = s9 + s9
    val s11 = s10 + s10
    val s12 = s11 + s11
    val s13 = s12 + s12
    val s14 = s13 + s13
    val s15 = s14 + s14
    val s16 = s15 + s15
    val s17 = s16 + s16
    val s18 = s17 + s17
    val s19 = s18 + s18
    val s20 = s19 + s19
    val s21 = s20 + s20
    val s22 = s21 + s21
    val s23 = s22 + s22
    val s24 = s23 + s23
    val s25 = s24 + s24
    val s26 = s25 + s25
    val s27 = s26 + s26
    val s28 = s27 + s27
    val s29 = s28 + s28
    val s30 = s29 + s29
    return if (s30.take(2).toList() == listOf(1, 1)) "OK" else "Fail"
}
