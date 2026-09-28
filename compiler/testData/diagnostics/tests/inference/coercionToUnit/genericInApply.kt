// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB

fun <N : Number> addNumber(n: N) = n

fun foo() = 42

fun test() {
    foo().apply(::addNumber)
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, integerLiteral, typeConstraint, typeParameter */
