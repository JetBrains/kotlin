// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun testReturn() {
    <!UNREACHABLE_CODE!>return<!> todo()
}

fun todo(): Nothing = throw Exception()