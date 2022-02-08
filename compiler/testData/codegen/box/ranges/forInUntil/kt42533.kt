// IGNORE_BACKEND: JVM
// WITH_STDLIB

fun box(): String {
    // These should all be empty progressions
    for (i in (Int.MAX_VALUE - 2) until Int.MIN_VALUE) { return "Fail: Int" }
    for (i in ((Int.MAX_VALUE - 2) until Int.MIN_VALUE).reversed()) { return "Fail: Int reversed" }

    for (i in (Long.MAX_VALUE - 2) until Long.MIN_VALUE) { return "Fail: Long" }
    for (i in ((Long.MAX_VALUE - 2) until Long.MIN_VALUE).reversed()) { return "Fail: Long reversed" }

    for (i in (Char.MAX_VALUE - 2) until Char.MIN_VALUE) { return "Fail: Char" }
    for (i in ((Char.MAX_VALUE - 2) until Char.MIN_VALUE).reversed()) { return "Fail: Char reversed" }

    for (i in (UInt.MAX_VALUE - 2u) until UInt.MIN_VALUE) { return "Fail: UInt" }
    for (i in ((UInt.MAX_VALUE - 2u) until UInt.MIN_VALUE).reversed()) { return "Fail: UInt reversed" }

    for (i in (ULong.MAX_VALUE - 2u) until ULong.MIN_VALUE) { return "Fail: ULong" }
    for (i in ((ULong.MAX_VALUE - 2u) until ULong.MIN_VALUE).reversed()) { return "Fail: ULong reversed" }

    return "OK"
}
