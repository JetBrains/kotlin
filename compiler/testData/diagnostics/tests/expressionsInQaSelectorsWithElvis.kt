// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-64891
// FIR_DUMP

fun test(a: (Int.() -> Int)?, b: Int.() -> Int) {
    2.(a ?: b)()
}

/* GENERATED_FIR_TAGS: elvisExpression, functionDeclaration, functionalType, integerLiteral, nullableType,
typeWithExtension */
