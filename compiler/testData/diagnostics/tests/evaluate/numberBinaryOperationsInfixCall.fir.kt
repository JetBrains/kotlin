fun fooInt(p: Int) = p
fun fooLong(p: Long) = p
fun fooByte(p: Byte) = p
fun fooShort(p: Short) = p

fun test() {
    fooInt(1 plus 1)
    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(1 plus 1)
    <!INAPPLICABLE_CANDIDATE!>fooLong<!>(1 plus 1)
    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(1 plus 1)

    fooInt(1 times 1)
    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(1 times 1)
    <!INAPPLICABLE_CANDIDATE!>fooLong<!>(1 times 1)
    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(1 times 1)

    fooInt(1 div 1)
    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(1 div 1)
    <!INAPPLICABLE_CANDIDATE!>fooLong<!>(1 div 1)
    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(1 div 1)

    fooInt(1 rem 1)
    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(1 rem 1)
    <!INAPPLICABLE_CANDIDATE!>fooLong<!>(1 rem 1)
    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(1 rem 1)
}
