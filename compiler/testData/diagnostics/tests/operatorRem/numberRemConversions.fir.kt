// DIAGNOSTICS: -UNUSED_PARAMETER

fun fooInt(p: Int) = p
fun fooLong(p: Long) = p
fun fooByte(p: Byte) = p
fun fooShort(p: Short) = p

fun test() {
    fooInt(1 % 1)
    fooByte(<!ARGUMENT_TYPE_MISMATCH!>1 % 1<!>)
    fooLong(1 % 1)
    fooShort(<!ARGUMENT_TYPE_MISMATCH!>1 % 1<!>)
}

public operator fun Int.rem(other: Int): Int = 0
