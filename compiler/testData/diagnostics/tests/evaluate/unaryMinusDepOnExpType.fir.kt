fun fooInt(p: Int) = p
fun fooLong(p: Long) = p
fun fooByte(p: Byte) = p
fun fooShort(p: Short) = p

fun test() {
    fooInt(-1)
    <!INAPPLICABLE_CANDIDATE!>fooInt<!>(-1111111111111111111)
    fooInt(-1.toInt())
    fooInt(-1.toByte())
    <!INAPPLICABLE_CANDIDATE!>fooInt<!>(-1.toLong())
    fooInt(-1.toShort())

    fooByte(-1)
    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(-1111111111111111111)
    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(-1.toInt())
    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(-1.toByte())
    fooByte((-1).toByte())
    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(-1.toLong())
    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(-1.toShort())

    fooLong(-1)
    fooLong(-1111111111111111111)
    <!INAPPLICABLE_CANDIDATE!>fooLong<!>(-1.toInt())
    <!INAPPLICABLE_CANDIDATE!>fooLong<!>(-1.toByte())
    fooLong(-1.toLong())
    <!INAPPLICABLE_CANDIDATE!>fooLong<!>(-1.toShort())

    fooShort(-1)
    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(-1111111111111111111)
    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(-1.toInt())
    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(-1.toByte())
    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(-1.toLong())
    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(-1.toShort())
    fooShort((-1).toShort())
}