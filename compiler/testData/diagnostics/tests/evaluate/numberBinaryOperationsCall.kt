// !LANGUAGE: -ApproximateIntegerLiteralTypesInReceiverPosition
fun fooInt(p: Int) = p
fun fooLong(p: Long) = p
fun fooByte(p: Byte) = p
fun fooShort(p: Short) = p

fun test() {
    fooInt(1.plus(1))
    fooByte(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1.plus(1)<!>)
    fooLong(1.plus(1))
    fooShort(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1.plus(1)<!>)

    fooInt(1.times(1))
    fooByte(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1.times(1)<!>)
    fooLong(1.times(1))
    fooShort(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1.times(1)<!>)

    fooInt(1.div(1))
    fooByte(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1.div(1)<!>)
    fooLong(1.div(1))
    fooShort(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1.div(1)<!>)

    fooInt(1.rem(1))
    fooByte(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1.rem(1)<!>)
    fooLong(1.rem(1))
    fooShort(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1.rem(1)<!>)
}
