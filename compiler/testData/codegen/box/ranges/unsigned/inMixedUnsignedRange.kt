// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

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

fun ul_ub(x: ULong, a: UByte, b: UByte) = x in a..b
fun ul_us(x: ULong, a: UShort, b: UShort) = x in a..b
fun ul_ui(x: ULong, a: UInt, b: UInt) = x in a..b
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

fun n_ul_ub(x: ULong, a: UByte, b: UByte) = x !in a..b
fun n_ul_us(x: ULong, a: UShort, b: UShort) = x !in a..b
fun n_ul_ui(x: ULong, a: UInt, b: UInt) = x !in a..b
fun n_ul_ul(x: ULong, a: ULong, b: ULong) = x !in a..b

fun box(): String {

    // 'in' tests

    if (!ub_ub(1.toUByte(), 0.toUByte(), 2.toUByte())) throw AssertionError()
    if (!ub_ub(200.toUByte(), 10.toUByte(), 255.toUByte())) throw AssertionError()
    if (!ub_us(1.toUByte(), 0.toUShort(), 2.toUShort())) throw AssertionError()
    if (!ub_us(200.toUByte(), 10.toUShort(), 255.toUShort())) throw AssertionError()
    if (!ub_ui(1.toUByte(), 0.toUInt(), 2.toUInt())) throw AssertionError()
    if (!ub_ui(200.toUByte(), 10.toUInt(), 255.toUInt())) throw AssertionError()
    if (!ub_ul(1.toUByte(), 0.toULong(), 2.toULong())) throw AssertionError()
    if (!ub_ul(200.toUByte(), 10.toULong(), 255.toULong())) throw AssertionError()

    if (!us_ub(1.toUShort(), 0.toUByte(), 2.toUByte())) throw AssertionError()
    if (!us_ub(200.toUShort(), 10.toUByte(), 255.toUByte())) throw AssertionError()
    if (!us_us(1.toUShort(), 0.toUShort(), 2.toUShort())) throw AssertionError()
    if (!us_us(200.toUShort(), 10.toUShort(), 255.toUShort())) throw AssertionError()
    if (!us_us(60000.toUShort(), 10000.toUShort(), 65000.toUShort())) throw AssertionError()
    if (!us_ui(1.toUShort(), 0.toUInt(), 2.toUInt())) throw AssertionError()
    if (!us_ui(200.toUShort(), 10.toUInt(), 255.toUInt())) throw AssertionError()
    if (!us_ui(60000.toUShort(), 10000.toUInt(), 65000.toUInt())) throw AssertionError()
    if (!us_ul(1.toUShort(), 0.toULong(), 2.toULong())) throw AssertionError()
    if (!us_ul(200.toUShort(), 10.toULong(), 255.toULong())) throw AssertionError()
    if (!us_ul(60000.toUShort(), 10000.toULong(), 65000.toULong())) throw AssertionError()

    if (!ui_ub(1.toUInt(), 0.toUByte(), 2.toUByte())) throw AssertionError()
    if (!ui_ub(200.toUInt(), 10.toUByte(), 255.toUByte())) throw AssertionError()
    if (!ui_us(1.toUInt(), 0.toUShort(), 2.toUShort())) throw AssertionError()
    if (!ui_us(200.toUInt(), 10.toUShort(), 255.toUShort())) throw AssertionError()
    if (!ui_us(60000.toUInt(), 10000.toUShort(), 65000.toUShort())) throw AssertionError()
    if (!ui_ui(1.toUInt(), 0.toUInt(), 2.toUInt())) throw AssertionError()
    if (!ui_ui(200.toUInt(), 10.toUInt(), 255.toUInt())) throw AssertionError()
    if (!ui_ui(60000.toUInt(), 10000.toUInt(), 65000.toUInt())) throw AssertionError()
    if (!ui_ui(2200000000L.toUInt(), 2000000000L.toUInt(), 2400000000L.toUInt())) throw AssertionError()
    if (!ui_ul(1.toUInt(), 0.toULong(), 2.toULong())) throw AssertionError()
    if (!ui_ul(200.toUInt(), 10.toULong(), 255.toULong())) throw AssertionError()
    if (!ui_ul(60000.toUInt(), 10000.toULong(), 65000.toULong())) throw AssertionError()
    if (!ui_ul(2200000000L.toUInt(), 2000000000L.toULong(), 2400000000L.toULong())) throw AssertionError()

    if (!ul_ub(1.toULong(), 0.toUByte(), 2.toUByte())) throw AssertionError()
    if (!ul_ub(200.toULong(), 10.toUByte(), 255.toUByte())) throw AssertionError()
    if (!ul_us(1.toULong(), 0.toUShort(), 2.toUShort())) throw AssertionError()
    if (!ul_us(200.toULong(), 10.toUShort(), 255.toUShort())) throw AssertionError()
    if (!ul_us(60000.toULong(), 10000.toUShort(), 65000.toUShort())) throw AssertionError()
    if (!ul_ui(1.toULong(), 0.toUInt(), 2.toUInt())) throw AssertionError()
    if (!ul_ui(200.toULong(), 10.toUInt(), 255.toUInt())) throw AssertionError()
    if (!ul_ui(60000.toULong(), 10000.toUInt(), 65000.toUInt())) throw AssertionError()
    if (!ul_ui(2200000000L.toULong(), 2000000000L.toUInt(), 2400000000L.toUInt())) throw AssertionError()
    if (!ul_ul(1.toULong(), 0.toULong(), 2.toULong())) throw AssertionError()
    if (!ul_ul(200.toULong(), 10.toULong(), 255.toULong())) throw AssertionError()
    if (!ul_ul(60000.toULong(), 10000.toULong(), 65000.toULong())) throw AssertionError()
    if (!ul_ul(2200000000L.toULong(), 2000000000L.toULong(), 2400000000L.toULong())) throw AssertionError()
    if (!ul_ul(ULong.MAX_VALUE - 10.toULong(), UInt.MAX_VALUE.toULong(), ULong.MAX_VALUE - 1.toULong())) throw AssertionError()

    // '!in' tests

    if (n_ub_ub(1.toUByte(), 0.toUByte(), 2.toUByte())) throw AssertionError()
    if (n_ub_ub(200.toUByte(), 10.toUByte(), 255.toUByte())) throw AssertionError()
    if (n_ub_us(1.toUByte(), 0.toUShort(), 2.toUShort())) throw AssertionError()
    if (n_ub_us(200.toUByte(), 10.toUShort(), 255.toUShort())) throw AssertionError()
    if (n_ub_ui(1.toUByte(), 0.toUInt(), 2.toUInt())) throw AssertionError()
    if (n_ub_ui(200.toUByte(), 10.toUInt(), 255.toUInt())) throw AssertionError()
    if (n_ub_ul(1.toUByte(), 0.toULong(), 2.toULong())) throw AssertionError()
    if (n_ub_ul(200.toUByte(), 10.toULong(), 255.toULong())) throw AssertionError()

    if (n_us_ub(1.toUShort(), 0.toUByte(), 2.toUByte())) throw AssertionError()
    if (n_us_ub(200.toUShort(), 10.toUByte(), 255.toUByte())) throw AssertionError()
    if (n_us_us(1.toUShort(), 0.toUShort(), 2.toUShort())) throw AssertionError()
    if (n_us_us(200.toUShort(), 10.toUShort(), 255.toUShort())) throw AssertionError()
    if (n_us_us(60000.toUShort(), 10000.toUShort(), 65000.toUShort())) throw AssertionError()
    if (n_us_ui(1.toUShort(), 0.toUInt(), 2.toUInt())) throw AssertionError()
    if (n_us_ui(200.toUShort(), 10.toUInt(), 255.toUInt())) throw AssertionError()
    if (n_us_ui(60000.toUShort(), 10000.toUInt(), 65000.toUInt())) throw AssertionError()
    if (n_us_ul(1.toUShort(), 0.toULong(), 2.toULong())) throw AssertionError()
    if (n_us_ul(200.toUShort(), 10.toULong(), 255.toULong())) throw AssertionError()
    if (n_us_ul(60000.toUShort(), 10000.toULong(), 65000.toULong())) throw AssertionError()

    if (n_ui_ub(1.toUInt(), 0.toUByte(), 2.toUByte())) throw AssertionError()
    if (n_ui_ub(200.toUInt(), 10.toUByte(), 255.toUByte())) throw AssertionError()
    if (n_ui_us(1.toUInt(), 0.toUShort(), 2.toUShort())) throw AssertionError()
    if (n_ui_us(200.toUInt(), 10.toUShort(), 255.toUShort())) throw AssertionError()
    if (n_ui_us(60000.toUInt(), 10000.toUShort(), 65000.toUShort())) throw AssertionError()
    if (n_ui_ui(1.toUInt(), 0.toUInt(), 2.toUInt())) throw AssertionError()
    if (n_ui_ui(200.toUInt(), 10.toUInt(), 255.toUInt())) throw AssertionError()
    if (n_ui_ui(60000.toUInt(), 10000.toUInt(), 65000.toUInt())) throw AssertionError()
    if (n_ui_ui(2200000000L.toUInt(), 2000000000L.toUInt(), 2400000000L.toUInt())) throw AssertionError()
    if (n_ui_ul(1.toUInt(), 0.toULong(), 2.toULong())) throw AssertionError()
    if (n_ui_ul(200.toUInt(), 10.toULong(), 255.toULong())) throw AssertionError()
    if (n_ui_ul(60000.toUInt(), 10000.toULong(), 65000.toULong())) throw AssertionError()
    if (n_ui_ul(2200000000L.toUInt(), 2000000000L.toULong(), 2400000000L.toULong())) throw AssertionError()

    if (n_ul_ub(1.toULong(), 0.toUByte(), 2.toUByte())) throw AssertionError()
    if (n_ul_ub(200.toULong(), 10.toUByte(), 255.toUByte())) throw AssertionError()
    if (n_ul_us(1.toULong(), 0.toUShort(), 2.toUShort())) throw AssertionError()
    if (n_ul_us(200.toULong(), 10.toUShort(), 255.toUShort())) throw AssertionError()
    if (n_ul_us(60000.toULong(), 10000.toUShort(), 65000.toUShort())) throw AssertionError()
    if (n_ul_ui(1.toULong(), 0.toUInt(), 2.toUInt())) throw AssertionError()
    if (n_ul_ui(200.toULong(), 10.toUInt(), 255.toUInt())) throw AssertionError()
    if (n_ul_ui(60000.toULong(), 10000.toUInt(), 65000.toUInt())) throw AssertionError()
    if (n_ul_ui(2200000000L.toULong(), 2000000000L.toUInt(), 2400000000L.toUInt())) throw AssertionError()
    if (n_ul_ul(1.toULong(), 0.toULong(), 2.toULong())) throw AssertionError()
    if (n_ul_ul(200.toULong(), 10.toULong(), 255.toULong())) throw AssertionError()
    if (n_ul_ul(60000.toULong(), 10000.toULong(), 65000.toULong())) throw AssertionError()
    if (n_ul_ul(2200000000L.toULong(), 2000000000L.toULong(), 2400000000L.toULong())) throw AssertionError()
    if (n_ul_ul(ULong.MAX_VALUE - 10.toULong(), UInt.MAX_VALUE.toULong(), ULong.MAX_VALUE - 1.toULong())) throw AssertionError()

    return "OK"
}