fun fooInt(p: Int) = p
fun fooLong(p: Long) = p
fun fooByte(p: Byte) = p
fun fooShort(p: Short) = p

fun test() {
    fooInt(-1)
    fooInt(<!ARGUMENT_TYPE_MISMATCH!>-1111111111111111111<!>)
    fooInt(-1.toInt())
    fooInt(-1.toByte())
    fooInt(<!ARGUMENT_TYPE_MISMATCH!>-1.toLong()<!>)
    fooInt(-1.toShort())

    fooByte(-1)
    fooByte(<!ARGUMENT_TYPE_MISMATCH!>-1111111111111111111<!>)
    fooByte(<!ARGUMENT_TYPE_MISMATCH!>-1.toInt()<!>)
    fooByte(<!ARGUMENT_TYPE_MISMATCH!>-1.toByte()<!>)
    fooByte((-1).toByte())
    fooByte(<!ARGUMENT_TYPE_MISMATCH!>-1.toLong()<!>)
    fooByte(<!ARGUMENT_TYPE_MISMATCH!>-1.toShort()<!>)

    fooLong(-1)
    fooLong(-1111111111111111111)
    fooLong(<!ARGUMENT_TYPE_MISMATCH!>-1.toInt()<!>)
    fooLong(<!ARGUMENT_TYPE_MISMATCH!>-1.toByte()<!>)
    fooLong(-1.toLong())
    fooLong(<!ARGUMENT_TYPE_MISMATCH!>-1.toShort()<!>)

    fooShort(-1)
    fooShort(<!ARGUMENT_TYPE_MISMATCH!>-1111111111111111111<!>)
    fooShort(<!ARGUMENT_TYPE_MISMATCH!>-1.toInt()<!>)
    fooShort(<!ARGUMENT_TYPE_MISMATCH!>-1.toByte()<!>)
    fooShort(<!ARGUMENT_TYPE_MISMATCH!>-1.toLong()<!>)
    fooShort(<!ARGUMENT_TYPE_MISMATCH!>-1.toShort()<!>)
    fooShort((-1).toShort())
}