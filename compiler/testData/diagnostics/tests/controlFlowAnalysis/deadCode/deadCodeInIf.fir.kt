// RUN_PIPELINE_TILL: BACKEND
// WITH_EXTRA_CHECKERS
fun testIf() {
    if (todo()) <!UNREACHABLE_CODE, UNUSED_EXPRESSION!>1<!> else <!UNREACHABLE_CODE, UNUSED_EXPRESSION!>2<!>
}

fun testIf1(b: Boolean) {
    if (b) todo() else <!UNUSED_EXPRESSION!>1<!>

    bar()
}

fun todo(): Nothing = throw Exception()
fun bar() {}