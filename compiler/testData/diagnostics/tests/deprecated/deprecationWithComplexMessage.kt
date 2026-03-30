// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTICS_FULL_TEXT
const val MSG = "Hello" + " World"

@Deprecated(MSG)
fun deprecated() {}

fun test() {
    <!DEPRECATION!>deprecated<!>()
}

/* GENERATED_FIR_TAGS: const, functionDeclaration, propertyDeclaration, stringLiteral */
