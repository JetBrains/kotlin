fun fooInt(p: Int) = p
fun fooLong(p: Long) = p
fun fooByte(p: Byte) = p
fun fooShort(p: Short) = p

fun test() {
    fooInt(-1)
    fooInt(<!TYPE_MISMATCH!>-1111111111111111111<!>)
    fooInt(-1.toInt())
    fooInt(-1.toByte())
    fooInt(<!TYPE_MISMATCH!>-1.toLong()<!>)
    fooInt(-1.toShort())

    fooByte(-1)
    fooByte(<!TYPE_MISMATCH!>-1111111111111111111<!>)
    fooByte(<!TYPE_MISMATCH!>-1.toInt()<!>)
    fooByte(<!TYPE_MISMATCH!>-1.toByte()<!>)
    fooByte((-1).toByte())
    fooByte(<!TYPE_MISMATCH!>-1.toLong()<!>)
    fooByte(<!TYPE_MISMATCH!>-1.toShort()<!>)

    fooLong(-1)
    fooLong(-1111111111111111111)
    fooLong(<!TYPE_MISMATCH!>-1.toInt()<!>)
    fooLong(<!TYPE_MISMATCH!>-1.toByte()<!>)
    fooLong(-1.toLong())
    fooLong(<!TYPE_MISMATCH!>-1.toShort()<!>)

    fooShort(-1)
    fooShort(<!TYPE_MISMATCH!>-1111111111111111111<!>)
    fooShort(<!TYPE_MISMATCH!>-1.toInt()<!>)
    fooShort(<!TYPE_MISMATCH!>-1.toByte()<!>)
    fooShort(<!TYPE_MISMATCH!>-1.toLong()<!>)
    fooShort(<!TYPE_MISMATCH!>-1.toShort()<!>)
    fooShort((-1).toShort())
}