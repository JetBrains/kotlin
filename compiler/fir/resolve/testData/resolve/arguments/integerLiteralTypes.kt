fun takeInt(x: Int) {}
fun takeLong(x: Long) {}
fun takeByte(x: Byte) {}
fun takeAny(x: Any) {}
fun takeString(x: String) {}

fun test_0() {
    1l
    1
    10000000000
}

fun test_1() {
    takeInt(1)
    takeByte(1)
    takeLong(1)
}

fun test_2() {
    <!INAPPLICABLE_CANDIDATE!>takeInt<!>(10000000000)
    takeLong(10000000000)
    <!INAPPLICABLE_CANDIDATE!>takeByte<!>(1000)
}

fun test_3() {
    takeInt(run { 1 })
    takeByte(run { 1 })
    takeLong(run { 1 })
}


fun test_4() {
    takeAny(1)
    takeAny(run { 1 })
}

fun test_5() {
    <!INAPPLICABLE_CANDIDATE!>takeString<!>(1)
    <!INAPPLICABLE_CANDIDATE!>takeString<!>(run { 1 })
}

annotation class Ann(val x: Byte)

@Ann(10)
fun test_6() {
    @Ann(300)
    val x = ""
}
