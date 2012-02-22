fun box() : String {
    val r1 = IntRange(1, 4)
    if(r1.end != 4 || r1.isReversed || r1.size != 4) return "fail"

    val r3 = -(0..5)
    if(r3.start != 5 || r3.end != 0 || !r3.isReversed || r3.size != 6) return "fail"

    val r4 = -r3
    if(r4.end != 5 || r4.isReversed || r4.size != 6) return "fail"

    val r5 = ByteRange(1, 4)
    if(r5.end != 4.toByte() || r5.isReversed || r5.size != 4) return "fail"

    val r7 = -(0.toByte()..5.toByte())
    if(r7.start != 5.toByte() || r7.end != 0.toByte() || !r7.isReversed) return "fail"

    val r9 = -r7
    if(r9.end != 5.toByte() || r9.isReversed) return "fail"

    val r10 = ShortRange(1, 4)
    if(r10.end != 4.toShort() || r10.isReversed || r10.size != 4) return "fail"

    val r12 = -(0.toShort()..5.toShort())
    if(r12.start != 5.toShort() || r12.end != 0.toShort() || !r12.isReversed) return "fail"

    val r13 = -r12
    if(r13.end != 5.toShort() || r13.isReversed) return "fail"

    val r14 = CharRange('a', 4)
    if(r14.end != 'd' || r14.isReversed || r14.size != 4) return "fail"

    val r16 = -('a'..'e')
    if(r16.start != 'e' || r16.end != 'a' || !r16.isReversed) return "fail"

    val r17 = -r16
    if(r17.end != 'e' || r17.isReversed) return "fail"

    return "OK"
}