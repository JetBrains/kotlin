// WITH_RUNTIME

fun box(): String {
    val charBound = Char.MIN_VALUE
    if ('b' in 'a' until charBound) return "Fail in Char.MIN_VALUE"
    if (!('b' !in 'a' until charBound)) return "Fail !in Char.MIN_VALUE"

    val intBound = Int.MIN_VALUE
    if (1 in 0 until intBound) return "Fail in Int.MIN_VALUE"
    if (!(1 !in 0 until intBound)) return "Fail !in Int.MIN_VALUE"

    val longBound = Long.MIN_VALUE
    if (1L in 0L until longBound) return "Fail in Long.MIN_VALUE"
    if (!(1L !in 0L until longBound)) return "Fail !in Long.MIN_VALUE"

    val uIntBound = UInt.MIN_VALUE
    if (1u in 0u until uIntBound) return "Fail in UInt.MIN_VALUE"
    if (!(1u !in 0u until uIntBound)) return "Fail !in UInt.MIN_VALUE"

    val uLongBound = ULong.MIN_VALUE
    if (1uL in 0uL until uLongBound) return "Fail in ULong.MIN_VALUE"
    if (!(1uL !in 0uL until uLongBound)) return "Fail !in ULong.MIN_VALUE"

    return "OK"
}