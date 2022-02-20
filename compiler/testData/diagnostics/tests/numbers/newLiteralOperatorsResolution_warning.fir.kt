// LANGUAGE: -ApproximateIntegerLiteralTypesInReceiverPosition
// WITH_STDLIB
// ISSUE: KT-38895

fun takeByte(b: Byte) {}
fun takeInt(b: Int) {}
fun takeLong(b: Long) {}

fun testByteBinaryOperators() {
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>2 + 1<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>2 - 1<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>2 * 1<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>2 / 1<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>2 % 1<!>)

    takeByte(<!ARGUMENT_TYPE_MISMATCH!>2.plus(1)<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>2.minus(1)<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>2.times(1)<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>2.div(1)<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>2.rem(1)<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>2 shl 1<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>2 shr 1<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>2 ushr 1<!>)

    takeByte(<!ARGUMENT_TYPE_MISMATCH!>2 and 1<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>2 or 1<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>2 xor 1<!>)
}

fun testByteUnaryOperators() {
    // Won't change
    takeByte(+1)
    takeByte(-1)

    // Will change
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>2.unaryPlus()<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>2.unaryMinus()<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>2.inv()<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>1.inc()<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>1.dec()<!>)
}

// all positive
fun testLongBinaryOperators() {
    takeLong(2 + 1)
    takeLong(2 - 1)
    takeLong(2 * 1)
    takeLong(2 / 1)
    takeLong(2 % 1)

    takeLong(2.plus(1))
    takeLong(2.minus(1))
    takeLong(2.times(1))
    takeLong(2.div(1))
    takeLong(2.rem(1))
    takeLong(2 shl 1)
    takeLong(2 shr 1)
    takeLong(2 ushr 1)

    takeLong(2 and 1)
    takeLong(2 or 1)
    takeLong(2 xor 1)

    takeLong(2 * 100000000000)
}

fun testLongUnaryOperators() {
    // Won't change
    takeLong(+1)
    takeLong(-1)
    takeLong(2.unaryPlus())
    takeLong(2.unaryMinus())
    takeLong(2.inv())

    // Will change
    takeLong(<!ARGUMENT_TYPE_MISMATCH!>1.inc()<!>)
    takeLong(<!ARGUMENT_TYPE_MISMATCH!>1.dec()<!>)
}

fun testIntBinaryOperators() {
    takeInt(2 + 1)
    takeInt(2 - 1)
    takeInt(2 * 1)
    takeInt(2 / 1)
    takeInt(2 % 1)

    takeInt(2.plus(1))
    takeInt(2.minus(1))
    takeInt(2.times(1))
    takeInt(2.div(1))
    takeInt(2.rem(1))
    takeInt(2 shl 1)
    takeInt(2 shr 1)
    takeInt(2 ushr 1)

    takeInt(2 and 1)
    takeInt(2 or 1)
    takeInt(2 xor 1)
}

fun testIntUnaryOperators() {
    takeInt(+1)
    takeInt(-1)

    takeInt(2.unaryPlus())
    takeInt(2.unaryMinus())
    takeInt(2.inv())
    takeInt(1.inc())
    takeInt(1.dec())
}

fun testNoOperators() {
    takeByte(1)
    takeInt(1)
    takeLong(1)
}
