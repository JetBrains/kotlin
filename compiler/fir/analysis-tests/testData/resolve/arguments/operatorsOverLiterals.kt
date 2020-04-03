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

fun Int.baz(): Int
fun Byte.baz(): Byte {}

fun test_3() {
    val x = 1.bar()
    val y = 1.baz()
}

fun takeByte(b: Byte) {}

fun test_4() {
    takeByte(1 + 1)
    <!INAPPLICABLE_CANDIDATE!>takeByte<!>(1 + 127)
    takeByte(1 - 1)
    <!INAPPLICABLE_CANDIDATE!>takeByte<!>(-100 - 100)
    takeByte(10 * 10)
    <!INAPPLICABLE_CANDIDATE!>takeByte<!>(100 * 100)
    <!UNRESOLVED_REFERENCE!>taleByte<!>(10 / 10)
    takeByte(100 % 10)
    takeByte(1000 % 10)
    takeByte(1000 and 100)
    <!INAPPLICABLE_CANDIDATE!>takeByte<!>(128 and 511)
    takeByte(100 or 100)
    <!INAPPLICABLE_CANDIDATE!>takeByte<!>(1000 or 0)
    takeByte(511 xor 511)
    <!INAPPLICABLE_CANDIDATE!>takeByte<!>(512 xor 511)
}

fun test_5() {
    takeByte(-1)
    takeByte(+1)
    takeByte(1.inv())
}

fun test_6() {
    <!INAPPLICABLE_CANDIDATE!>takeByte<!>(run { 127 + 1 })
    <!INAPPLICABLE_CANDIDATE!>takeByte<!>(1 + run { 1 })
    takeByte(run { 1 + 1 })
    1 + 1
    run { 1 }
    1 + run { 1 }
}

fun test_7(d: Double) {
    val x1 = 1 + d
    val x2 = d + 1
}