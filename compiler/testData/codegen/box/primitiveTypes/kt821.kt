fun box() : String {
    val r1 = IntRange(1, 4)
    if(r1.end != 4 || r1.size != 4) return "fail"

    val r2 = ByteRange(1, 4)
    if(r2.end != 4.toByte() || r2.size != 4) return "fail"

    val r3 = ShortRange(1, 4)
    if(r3.end != 4.toShort() || r3.size != 4) return "fail"

    val r4 = CharRange('a', 4)
    if(r4.end != 'd' || r4.size != 4) return "fail"

    val r5 = FloatRange(0.0.toFloat(), 1.0.toFloat())
    if(r5.end != 1.0.toFloat() || r5.size != 1.0.toFloat()) return "fail"

    val r6 = DoubleRange(0.0, 1.0)
    if(r6.end != 1.0 || r6.size != 1.0) return "fail"

    val r7 = LongRange(1, 4)
    if(r7.end != 4.toLong() || r7.size != 4.toLong()) return "fail"

    return "OK"
}
