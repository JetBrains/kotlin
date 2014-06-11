fun testReturn() {
    <!UNREACHABLE_CODE!>return<!> todo()
}

fun todo() = throw Exception()