val p1 = -1
val p2 = -1.toLong()
val p3 = (-1).toByte()
val p4 = -1.toInt()
val p5 = (-1).toShort()
val p6 = -1111111111111111111

fun fooInt(p: Int) = p
fun fooLong(p: Long) = p
fun fooByte(p: Byte) = p
fun fooShort(p: Short) = p

fun test() {
    fooInt(p1)
    fooInt(<!TYPE_MISMATCH!>p2<!>)
    fooInt(<!TYPE_MISMATCH!>p3<!>)
    fooInt(p4)
    fooInt(<!TYPE_MISMATCH!>p5<!>)
    fooInt(<!TYPE_MISMATCH!>p6<!>)

    fooLong(<!TYPE_MISMATCH!>p1<!>)
    fooLong(p2)
    fooLong(<!TYPE_MISMATCH!>p3<!>)
    fooLong(<!TYPE_MISMATCH!>p4<!>)
    fooLong(<!TYPE_MISMATCH!>p5<!>)
    fooLong(p6)

    fooShort(<!TYPE_MISMATCH!>p1<!>)
    fooShort(<!TYPE_MISMATCH!>p2<!>)
    fooShort(<!TYPE_MISMATCH!>p3<!>)
    fooShort(<!TYPE_MISMATCH!>p4<!>)
    fooShort(p5)
    fooShort(<!TYPE_MISMATCH!>p6<!>)

    fooByte(<!TYPE_MISMATCH!>p1<!>)
    fooByte(<!TYPE_MISMATCH!>p2<!>)
    fooByte(p3)
    fooByte(<!TYPE_MISMATCH!>p4<!>)
    fooByte(<!TYPE_MISMATCH!>p5<!>)
    fooByte(<!TYPE_MISMATCH!>p6<!>)
}
