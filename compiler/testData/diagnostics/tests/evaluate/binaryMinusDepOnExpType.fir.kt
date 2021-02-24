fun fooInt(p: Int) = p
fun fooLong(p: Long) = p
fun fooByte(p: Byte) = p
fun fooShort(p: Short) = p

fun test() {
    fooInt(1 - 1)
    fooInt(1 - 1.toInt())
    fooInt(1 - 1.toByte())
    <!INAPPLICABLE_CANDIDATE!>fooInt<!>(1 - 1.toLong())
    fooInt(1 - 1.toShort())

    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(1 - 1)
    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(1 - 1.toInt())
    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(1 - 1.toByte())
    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(1 - 1.toLong())
    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(1 - 1.toShort())

    <!INAPPLICABLE_CANDIDATE!>fooLong<!>(1 - 1)
    <!INAPPLICABLE_CANDIDATE!>fooLong<!>(1 - 1.toInt())
    <!INAPPLICABLE_CANDIDATE!>fooLong<!>(1 - 1.toByte())
    fooLong(1 - 1.toLong())
    <!INAPPLICABLE_CANDIDATE!>fooLong<!>(1 - 1.toShort())

    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(1 - 1)
    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(1 - 1.toInt())
    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(1 - 1.toByte())
    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(1 - 1.toLong())
    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(1 - 1.toShort())
}
