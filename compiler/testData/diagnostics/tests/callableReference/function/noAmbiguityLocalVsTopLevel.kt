// RUN_PIPELINE_TILL: CODEGEN
// DIAGNOSTICS: -UNUSED_EXPRESSION
fun bar() = 42

fun main() {
    fun bar() = 239

    ::bar
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, integerLiteral, localFunction */
