// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

fun foo(a: Int) {}
fun foo(a: String) {}

fun main() {
    <!NONE_APPLICABLE!>foo<!> { "A short lambda" }
    <!NONE_APPLICABLE!>foo<!> { "A very long lambda that makes the expression reach beyond 50 characters" }
    <!NONE_APPLICABLE!>foo<!> { a: Int ->
        "Another long lambda that spans multiple lines"
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, stringLiteral */
