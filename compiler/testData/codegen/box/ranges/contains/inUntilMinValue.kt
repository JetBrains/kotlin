// WITH_STDLIB
// USE_OLD_INLINE_CLASSES_MANGLING_SCHEME

fun box(): String {
    if ('b' in 'a' until Char.MIN_VALUE) return "Fail in Char.MIN_VALUE"
    if (!('b' !in 'a' until Char.MIN_VALUE)) return "Fail !in Char.MIN_VALUE"

    if (1 in 0 until Int.MIN_VALUE) return "Fail in Int.MIN_VALUE"
    if (!(1 !in 0 until Int.MIN_VALUE)) return "Fail !in Int.MIN_VALUE"

    if (1L in 0L until Long.MIN_VALUE) return "Fail in Long.MIN_VALUE"
    if (!(1L !in 0L until Long.MIN_VALUE)) return "Fail !in Long.MIN_VALUE"

    if (1u in 0u until UInt.MIN_VALUE) return "Fail in UInt.MIN_VALUE"
    if (!(1u !in 0u until UInt.MIN_VALUE)) return "Fail !in UInt.MIN_VALUE"

    if (1uL in 0uL until ULong.MIN_VALUE) return "Fail in ULong.MIN_VALUE"
    if (!(1uL !in 0uL until ULong.MIN_VALUE)) return "Fail !in ULong.MIN_VALUE"

    return "OK"
}
