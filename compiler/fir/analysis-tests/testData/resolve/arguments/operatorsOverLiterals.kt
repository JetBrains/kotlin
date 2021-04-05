fun foo(x: Int) {}
fun foo(x: Byte) {}

fun test_0() {
    foo(1)
}

fun test_1() {
    val x1 = 1 + 1
    val x2 = 1.plus(1)
    1 + 1
    127 + 1
    val x3 = 2000000000 * 4
}

fun test_2(n: Int) {
    val x = 1 + n
    val y = n + 1
}

fun Int.bar(): Int {}

fun Int.baz(): Int {}
fun Byte.baz(): Byte {}

fun test_3() {
    val x = 1.bar()
    val y = 1.baz()
}

fun takeByte(b: Byte) {}

fun test_4() {
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>1 + 1<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>1 + 127<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>1 - 1<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>-100 - 100<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>10 * 10<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>100 * 100<!>)
    <!UNRESOLVED_REFERENCE!>taleByte<!>(10 / 10)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>100 % 10<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>1000 % 10<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>1000 and 100<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>128 and 511<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>100 or 100<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>1000 or 0<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>511 xor 511<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>512 xor 511<!>)
}

fun test_5() {
    takeByte(-1)
    takeByte(+1)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>1.inv()<!>)
}

fun test_6() {
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>run { 127 + 1 }<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>1 + run { 1 }<!>)
    takeByte(<!ARGUMENT_TYPE_MISMATCH!>run { 1 + 1 }<!>)
    1 + 1
    run { 1 }
    1 + run { 1 }
}

fun test_7(d: Double) {
    val x1 = 1 + d
    val x2 = d + 1
}
