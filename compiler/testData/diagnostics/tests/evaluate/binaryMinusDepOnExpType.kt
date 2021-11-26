// !LANGUAGE: -ApproximateIntegerLiteralTypesInReceiverPosition
fun fooInt(p: Int) = p
fun fooLong(p: Long) = p
fun fooByte(p: Byte) = p
fun fooShort(p: Short) = p

fun test() {
    fooInt(1 - 1)
    fooInt(1 - 1.toInt())
    fooInt(1 - 1.toByte())
    fooInt(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE, TYPE_MISMATCH!>1 - 1.toLong()<!>)
    fooInt(1 - 1.toShort())

    fooByte(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 - 1<!>)
    fooByte(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE, TYPE_MISMATCH!>1 - 1.toInt()<!>)
    fooByte(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE, TYPE_MISMATCH!>1 - 1.toByte()<!>)
    fooByte(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE, TYPE_MISMATCH!>1 - 1.toLong()<!>)
    fooByte(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE, TYPE_MISMATCH!>1 - 1.toShort()<!>)

    fooLong(1 - 1)
    fooLong(<!TYPE_MISMATCH!>1 - 1.toInt()<!>)
    fooLong(<!TYPE_MISMATCH!>1 - 1.toByte()<!>)
    fooLong(1 - 1.toLong())
    fooLong(<!TYPE_MISMATCH!>1 - 1.toShort()<!>)

    fooShort(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 - 1<!>)
    fooShort(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE, TYPE_MISMATCH!>1 - 1.toInt()<!>)
    fooShort(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE, TYPE_MISMATCH!>1 - 1.toByte()<!>)
    fooShort(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE, TYPE_MISMATCH!>1 - 1.toLong()<!>)
    fooShort(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE, TYPE_MISMATCH!>1 - 1.toShort()<!>)
}
