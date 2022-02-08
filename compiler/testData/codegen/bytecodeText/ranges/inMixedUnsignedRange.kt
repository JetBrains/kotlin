fun ub_ub(x: UByte, a: UByte, b: UByte) = x in a..b
fun ub_us(x: UByte, a: UShort, b: UShort) = x in a..b
fun ub_ui(x: UByte, a: UInt, b: UInt) = x in a..b
fun ub_ul(x: UByte, a: ULong, b: ULong) = x in a..b

fun us_ub(x: UShort, a: UByte, b: UByte) = x in a..b
fun us_us(x: UShort, a: UShort, b: UShort) = x in a..b
fun us_ui(x: UShort, a: UInt, b: UInt) = x in a..b
fun us_ul(x: UShort, a: ULong, b: ULong) = x in a..b

fun ui_ub(x: UInt, a: UByte, b: UByte) = x in a..b
fun ui_us(x: UInt, a: UShort, b: UShort) = x in a..b
fun ui_ui(x: UInt, a: UInt, b: UInt) = x in a..b
fun ui_ul(x: UInt, a: ULong, b: ULong) = x in a..b

// ul_ub, ul_us, ul_ui combinations are tested in inMixedUnsignedRange_2.kt (different behavior in non-IR vs IR backends)
fun ul_ul(x: ULong, a: ULong, b: ULong) = x in a..b

fun n_ub_ub(x: UByte, a: UByte, b: UByte) = x !in a..b
fun n_ub_us(x: UByte, a: UShort, b: UShort) = x !in a..b
fun n_ub_ui(x: UByte, a: UInt, b: UInt) = x !in a..b
fun n_ub_ul(x: UByte, a: ULong, b: ULong) = x !in a..b

fun n_us_ub(x: UShort, a: UByte, b: UByte) = x !in a..b
fun n_us_us(x: UShort, a: UShort, b: UShort) = x !in a..b
fun n_us_ui(x: UShort, a: UInt, b: UInt) = x !in a..b
fun n_us_ul(x: UShort, a: ULong, b: ULong) = x !in a..b

fun n_ui_ub(x: UInt, a: UByte, b: UByte) = x !in a..b
fun n_ui_us(x: UInt, a: UShort, b: UShort) = x !in a..b
fun n_ui_ui(x: UInt, a: UInt, b: UInt) = x !in a..b
fun n_ui_ul(x: UInt, a: ULong, b: ULong) = x !in a..b

// n_ul_ub, n_ul_us, n_ul_ui combinations are tested in inMixedUnsignedRange_2.kt (different behavior in non-IR vs IR backends)
fun n_ul_ul(x: ULong, a: ULong, b: ULong) = x !in a..b

// 0 contains
// 0 L2I
// 18 SIPUSH 255
// 2 LDC 255
// 20 LDC 65535
// 2 LDC 4294967295

// "SIPUSH/LDC 255" represent conversion from UByte to UInt/ULong
// "LDC 65535" is UShort to UInt/ULong
// "LDC 4294967295" is UInt to ULong