// ULong in range of UInt, uses non-intrinsic 'contains' for non-IR backend

fun ul_ub(x: ULong, a: UByte, b: UByte) = x in a..b
fun ul_us(x: ULong, a: UShort, b: UShort) = x in a..b
fun ul_ui(x: ULong, a: UInt, b: UInt) = x in a..b

fun n_ul_ub(x: ULong, a: UByte, b: UByte) = x !in a..b
fun n_ul_us(x: ULong, a: UShort, b: UShort) = x !in a..b
fun n_ul_ui(x: ULong, a: UInt, b: UInt) = x !in a..b

// JVM_TEMPLATES
// 6 contains

// JVM_IR_TEMPLATES
// 0 contains
// 4 LDC 255
// 4 LDC 65535
// 4 LDC 4294967295

// "LDC 255" represent conversion from UByte to ULong
// "LDC 65535" is UShort to ULong
// "LDC 4294967295" is UInt to ULong