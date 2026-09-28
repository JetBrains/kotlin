// RUN_PIPELINE_TILL: CODEGEN
fun testReturn() {
    <!UNREACHABLE_CODE!>return<!> todo()
}

fun todo(): Nothing = throw Exception()

/* GENERATED_FIR_TAGS: functionDeclaration */
