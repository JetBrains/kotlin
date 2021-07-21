// !DIAGNOSTICS: -UNUSED_PARAMETER

fun fooInt(p: Int) = p
fun fooLong(p: Long) = p
fun fooByte(p: Byte) = p
fun fooShort(p: Short) = p

fun test() {
    fooInt(1 % 1)
    fooByte(<!TYPE_MISMATCH!>1 % 1<!>)
    fooLong(<!TYPE_MISMATCH!>1 % 1<!>)
    fooShort(<!TYPE_MISMATCH!>1 % 1<!>)
}

public operator fun Int.<!EXTENSION_SHADOWED_BY_MEMBER!>rem<!>(other: Int): Int = 0
