// RUN_PIPELINE_TILL: BACKEND
fun foo(x: (String) -> Int) {}


fun bar(y: Any): Int = 1
fun bar(x: String): Int = 1

fun main() {
    foo(::bar)
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, integerLiteral */
