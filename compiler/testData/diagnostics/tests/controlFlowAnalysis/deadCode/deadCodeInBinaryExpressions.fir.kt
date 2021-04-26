fun testBinary1() {
    operator fun Int.times(s: String) {}

    todo() * ""
}
fun testBinary2() {
    "1" + todo()
}

fun testElvis1() {
    todo() <!USELESS_ELVIS!>?: ""<!>
}

fun testElvis2(s: String?) {
    s ?: todo()

    bar()
}

fun testAnd1(b: Boolean) {
    b && todo()

    bar()
}

fun testAnd2(b: Boolean) {
    todo() && b
}

fun returnInBinary1(): Boolean {
    (return true) && (return false)
}

fun returnInBinary2(): Boolean {
    (return true) || (return false)
}

fun todo(): Nothing = throw Exception()
fun bar() {}
