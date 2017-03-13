fun box(): String {
    9 in 0..9
    val intRange = 0..9
    9 in intRange
    val charRange = '0'..'9'
    '9' in charRange
    val byteRange = 0.toByte()..9.toByte()
    // seems no stdlib available here, thus no contains as extension
    9 in byteRange
    val longRange = 0.toLong()..9.toLong()
    9.toLong() in longRange
    val shortRange = 0.toShort()..9.toShort()
    9 in shortRange

    return "OK"
}
