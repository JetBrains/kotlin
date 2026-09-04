// WITH_STDLIB
// TARGET_BACKEND: JVM
// a fused `sequence` builder inside a lambda contains copies of the sequences it yields,
// so the lambda is not copied to every place of use, otherwise the code size grows exponentially
fun box(): String {
    val s0 = sequenceOf(1)
    val s1 = sequenceOf(0).map { sequence { yieldAll(s0); yieldAll(s0) }.toList().sum() }
    val s2 = sequenceOf(0).map { sequence { yieldAll(s1); yieldAll(s1) }.toList().sum() }
    val s3 = sequenceOf(0).map { sequence { yieldAll(s2); yieldAll(s2) }.toList().sum() }
    val s4 = sequenceOf(0).map { sequence { yieldAll(s3); yieldAll(s3) }.toList().sum() }
    val s5 = sequenceOf(0).map { sequence { yieldAll(s4); yieldAll(s4) }.toList().sum() }
    val s6 = sequenceOf(0).map { sequence { yieldAll(s5); yieldAll(s5) }.toList().sum() }
    val s7 = sequenceOf(0).map { sequence { yieldAll(s6); yieldAll(s6) }.toList().sum() }
    val s8 = sequenceOf(0).map { sequence { yieldAll(s7); yieldAll(s7) }.toList().sum() }
    val s9 = sequenceOf(0).map { sequence { yieldAll(s8); yieldAll(s8) }.toList().sum() }
    val s10 = sequenceOf(0).map { sequence { yieldAll(s9); yieldAll(s9) }.toList().sum() }
    val s11 = sequenceOf(0).map { sequence { yieldAll(s10); yieldAll(s10) }.toList().sum() }
    val s12 = sequenceOf(0).map { sequence { yieldAll(s11); yieldAll(s11) }.toList().sum() }
    val s13 = sequenceOf(0).map { sequence { yieldAll(s12); yieldAll(s12) }.toList().sum() }
    val s14 = sequenceOf(0).map { sequence { yieldAll(s13); yieldAll(s13) }.toList().sum() }
    val n = s14.first() + s14.toList().single()
    return if (n == 32768) "OK" else "Fail: $n"
}
