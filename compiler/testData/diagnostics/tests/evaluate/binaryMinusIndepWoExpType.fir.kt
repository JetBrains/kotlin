val p1 = 1 - 1
val p2 = 1 - 1.toLong()
val p3 = 1 - 1.toByte()
val p4 = 1 - 1.toInt()
val p5 = 1 - 1.toShort()

fun fooInt(p: Int) = p
fun fooLong(p: Long) = p
fun fooByte(p: Byte) = p
fun fooShort(p: Short) = p

fun test() {
    fooInt(p1)
    <!INAPPLICABLE_CANDIDATE!>fooInt<!>(p2)
    fooInt(p3)
    fooInt(p4)
    fooInt(p5)

    <!INAPPLICABLE_CANDIDATE!>fooLong<!>(p1)
    fooLong(p2)
    <!INAPPLICABLE_CANDIDATE!>fooLong<!>(p3)
    <!INAPPLICABLE_CANDIDATE!>fooLong<!>(p4)
    <!INAPPLICABLE_CANDIDATE!>fooLong<!>(p5)

    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(p1)
    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(p2)
    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(p3)
    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(p4)
    <!INAPPLICABLE_CANDIDATE!>fooShort<!>(p5)

    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(p1)
    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(p2)
    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(p3)
    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(p4)
    <!INAPPLICABLE_CANDIDATE!>fooByte<!>(p5)
}