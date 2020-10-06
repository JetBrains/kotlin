fun fooInt(p: Int) = p
fun fooLong(p: Long) = p
fun fooByte(p: Byte) = p
fun fooShort(p: Short) = p

fun test() {
    fooInt(1 + 1)
    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(1 + 1)
    <!INAPPLICABLE_CANDIDATE!>fooLong<!>(1 + 1)
    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(1 + 1)

    fooInt(1 * 1)
    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(1 * 1)
    <!INAPPLICABLE_CANDIDATE!>fooLong<!>(1 * 1)
    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(1 * 1)

    fooInt(1 / 1)
    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(1 / 1)
    <!INAPPLICABLE_CANDIDATE!>fooLong<!>(1 / 1)
    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(1 / 1)

    fooInt(1 % 1)
    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(1 % 1)
    <!INAPPLICABLE_CANDIDATE!>fooLong<!>(1 % 1)
    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(1 % 1)
}
