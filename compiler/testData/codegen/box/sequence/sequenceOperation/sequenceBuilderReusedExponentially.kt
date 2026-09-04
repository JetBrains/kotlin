// WITH_STDLIB
// TARGET_BACKEND: JVM
// a `sequence` builder is not copied to every place of use, otherwise the code size grows exponentially
fun box(): String {
    val s0 = sequenceOf(1)
    val s1 = sequence { yieldAll(s0); yieldAll(s0) }
    val s2 = sequence { yieldAll(s1); yieldAll(s1) }
    val s3 = sequence { yieldAll(s2); yieldAll(s2) }
    val s4 = sequence { yieldAll(s3); yieldAll(s3) }
    val s5 = sequence { yieldAll(s4); yieldAll(s4) }
    val s6 = sequence { yieldAll(s5); yieldAll(s5) }
    val s7 = sequence { yieldAll(s6); yieldAll(s6) }
    val s8 = sequence { yieldAll(s7); yieldAll(s7) }
    val s9 = sequence { yieldAll(s8); yieldAll(s8) }
    val s10 = sequence { yieldAll(s9); yieldAll(s9) }
    val s11 = sequence { yieldAll(s10); yieldAll(s10) }
    val s12 = sequence { yieldAll(s11); yieldAll(s11) }
    val s13 = sequence { yieldAll(s12); yieldAll(s12) }
    val s14 = sequence { yieldAll(s13); yieldAll(s13) }
    val n = s14.count { true } + s14.toList().size
    return if (n == 32768) "OK" else "Fail: $n"
}
