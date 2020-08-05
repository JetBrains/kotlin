// WITH_RUNTIME

fun box(): String {
    if (Char.MAX_VALUE in Char.MAX_VALUE until Char.MAX_VALUE) return "Fail in Char.MAX_VALUE"
    if (!(Char.MAX_VALUE !in Char.MAX_VALUE until Char.MAX_VALUE)) return "Fail !in Char.MAX_VALUE"

    if (Int.MAX_VALUE in Int.MAX_VALUE until Int.MAX_VALUE) return "Fail in Int.MAX_VALUE"
    if (!(Int.MAX_VALUE !in Int.MAX_VALUE until Int.MAX_VALUE)) return "Fail !in Int.MAX_VALUE"

    if (Long.MAX_VALUE in Long.MAX_VALUE until Long.MAX_VALUE) return "Fail in Long.MAX_VALUE"
    if (!(Long.MAX_VALUE !in Long.MAX_VALUE until Long.MAX_VALUE)) return "Fail !in Long.MAX_VALUE"

    if (UInt.MAX_VALUE in UInt.MAX_VALUE until UInt.MAX_VALUE) return "Fail in UInt.MAX_VALUE"
    if (!(UInt.MAX_VALUE !in UInt.MAX_VALUE until UInt.MAX_VALUE)) return "Fail !in UInt.MAX_VALUE"

    if (ULong.MAX_VALUE in ULong.MAX_VALUE until ULong.MAX_VALUE) return "Fail in ULong.MAX_VALUE"
    if (!(ULong.MAX_VALUE !in ULong.MAX_VALUE until ULong.MAX_VALUE)) return "Fail !in ULong.MAX_VALUE"

    return "OK"
}