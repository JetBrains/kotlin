fun testReturn() {
    <!UNREACHABLE_CODE!>return<!> todo()
}

fun todo(): Nothing = throw Exception()