fun box() : String {
    val r1 = IntRange(1, 4)
    if(r1.start != 1 || r1.end != 4) return "fail"

    val r2 = ByteRange(1, 4)
    if(r2.start != 1.toByte() || r2.end != 4.toByte()) return "fail"

    val r3 = ShortRange(1, 4)
    if(r3.start != 1.toShort() || r3.end != 4.toShort()) return "fail"

    val r4 = CharRange('a', 'd')
    if(r4.start != 'a' || r4.end != 'd') return "fail"

    val r5 = FloatRange(0.0.toFloat(), 1.0.toFloat())
    if(r5.start != 0.0.toFloat() || r5.end != 1.0.toFloat()) return "fail"

    val r6 = DoubleRange(0.0, 1.0)
    if(r6.start != 0.0 || r6.end != 1.0) return "fail"

    val r7 = LongRange(1, 4)
    if(r7.start != 1.toLong() || r7.end != 4.toLong()) return "fail"

    return "OK"
}
