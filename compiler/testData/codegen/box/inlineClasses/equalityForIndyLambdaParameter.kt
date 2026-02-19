// WITH_STDLIB

val nullableUInt: UInt? = 2U
val uInt: UInt = 7U

fun box(): String {
    val l1 = { arg: UInt -> nullableUInt == arg }
    if (!l1(2U)) return "Fail l1"

    val l2 = { arg: UInt -> arg == nullableUInt }
    if (!l2(2U)) return "Fail l2"

    val l3 = { arg: UInt -> uInt == arg }
    if (!l3(7U)) return "Fail l3"

    val l4 = { arg: UInt -> arg == uInt }
    if (!l4(7U)) return "Fail l4"

    val l5 = { arg: UInt? -> nullableUInt == arg }
    if (!l5(2U)) return "Fail l5"

    val l6 = { arg: UInt? -> arg == nullableUInt }
    if (!l6(2U)) return "Fail l6"

    val l7 = { arg: UInt? -> uInt == arg }
    if (!l7(7U)) return "Fail l7"

    val l8 = { arg: UInt? -> arg == uInt }
    if (!l8(7U)) return "Fail l8"

    return "OK"
}
