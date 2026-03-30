// RUN_PIPELINE_TILL: BACKEND
fun testReturn() {
    <!UNREACHABLE_CODE!>return<!> todo()
}

fun todo(): Nothing = throw Exception()

/* GENERATED_FIR_TAGS: functionDeclaration */
