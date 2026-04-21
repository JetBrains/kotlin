// RUN_PIPELINE_TILL: FRONTEND
fun f(a: Int, b: Int, c: Int) {}

fun main() {
    <!NO_VALUE_FOR_PARAMETER!>f<!>(c = 3, (<!SYNTAX!><!>), a = 1)
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral */
