fun fooInt(p: Int) = p
fun fooLong(p: Long) = p
fun fooByte(p: Byte) = p
fun fooShort(p: Short) = p

fun test() {
    fooInt(1 - 1)
    fooInt(1 - 1.toInt())
    fooInt(1 - 1.toByte())
    fooInt(<!TYPE_MISMATCH!>1 - 1.toLong()<!>)
    fooInt(1 - 1.toShort())

    fooByte(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 - 1<!>)
    fooByte(<!TYPE_MISMATCH!>1 - 1.toInt()<!>)
    fooByte(<!TYPE_MISMATCH!>1 - 1.toByte()<!>)
    fooByte(<!TYPE_MISMATCH!>1 - 1.toLong()<!>)
    fooByte(<!TYPE_MISMATCH!>1 - 1.toShort()<!>)

    fooLong(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 - 1<!>)
    fooLong(<!TYPE_MISMATCH!>1 - 1.toInt()<!>)
    fooLong(<!TYPE_MISMATCH!>1 - 1.toByte()<!>)
    fooLong(1 - 1.toLong())
    fooLong(<!TYPE_MISMATCH!>1 - 1.toShort()<!>)

    fooShort(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 - 1<!>)
    fooShort(<!TYPE_MISMATCH!>1 - 1.toInt()<!>)
    fooShort(<!TYPE_MISMATCH!>1 - 1.toByte()<!>)
    fooShort(<!TYPE_MISMATCH!>1 - 1.toLong()<!>)
    fooShort(<!TYPE_MISMATCH!>1 - 1.toShort()<!>)
}
