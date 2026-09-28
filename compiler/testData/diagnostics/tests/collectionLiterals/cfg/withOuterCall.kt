// DUMP_CFG
// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB

fun foo(s: Set<*>) {
}

fun test() {
    foo([1, 2, 3])
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, starProjection */
