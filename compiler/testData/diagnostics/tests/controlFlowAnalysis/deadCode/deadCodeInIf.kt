fun testIf() {
    if (todo()) <!UNREACHABLE_CODE!>1<!> else <!UNREACHABLE_CODE!>2<!>
}

fun testIf1(b: Boolean) {
    if (b) todo() else <!UNUSED_EXPRESSION!>1<!>

    bar()
}

fun todo() = throw Exception()
fun bar() {}