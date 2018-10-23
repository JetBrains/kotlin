fun fooInt(p: Int) = p
fun fooLong(p: Long) = p
fun fooByte(p: Byte) = p
fun fooShort(p: Short) = p

fun test() {
    fooInt(1.plus(1))
    fooByte(1.plus(1))
    fooLong(1.plus(1))
    fooShort(1.plus(1))

    fooInt(1.times(1))
    fooByte(1.times(1))
    fooLong(1.times(1))
    fooShort(1.times(1))

    fooInt(1.div(1))
    fooByte(1.div(1))
    fooLong(1.div(1))
    fooShort(1.div(1))

    fooInt(1.<!DEPRECATION_ERROR!>mod<!>(1))
    fooByte(1.<!DEPRECATION_ERROR!>mod<!>(1))
    fooLong(1.<!DEPRECATION_ERROR!>mod<!>(1))
    fooShort(1.<!DEPRECATION_ERROR!>mod<!>(1))

    fooInt(1.rem(1))
    fooByte(1.rem(1))
    fooLong(1.rem(1))
    fooShort(1.rem(1))
}