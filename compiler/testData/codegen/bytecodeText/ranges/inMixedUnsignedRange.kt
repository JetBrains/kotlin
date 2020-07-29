// IGNORE_BACKEND: JVM_IR
// TODO KT-36829 Optimize 'in' expressions in JVM_IR

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

fun ul_ub(x: ULong, a: UByte, b: UByte) = x in a..b         // ULong in range of UInt, uses non-intrinsic 'contains'
fun ul_us(x: ULong, a: UShort, b: UShort) = x in a..b       // ULong in range of UInt, uses non-intrinsic 'contains'
fun ul_ui(x: ULong, a: UInt, b: UInt) = x in a..b           // ULong in range of UInt, uses non-intrinsic 'contains'
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

fun n_ul_ub(x: ULong, a: UByte, b: UByte) = x !in a..b      // ULong in range of UInt, uses non-intrinsic 'contains'
fun n_ul_us(x: ULong, a: UShort, b: UShort) = x !in a..b    // ULong in range of UInt, uses non-intrinsic 'contains'
fun n_ul_ui(x: ULong, a: UInt, b: UInt) = x !in a..b        // ULong in range of UInt, uses non-intrinsic 'contains'
fun n_ul_ul(x: ULong, a: ULong, b: ULong) = x !in a..b

// 6 contains
// 13 IFLE
// 13 IFLT
// 13 IFGE
// 13 IFGT
// 0 L2I
// 22 SIPUSH 255
// 24 LDC 65535
// 2 LDC 4294967295
