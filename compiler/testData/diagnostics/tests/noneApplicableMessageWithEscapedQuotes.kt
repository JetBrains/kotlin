// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

fun foo(a: Int) {}
fun foo(a: String) {}

fun main() {
    <!NONE_APPLICABLE!>foo<!>('a')
}

/* GENERATED_FIR_TAGS: functionDeclaration */
