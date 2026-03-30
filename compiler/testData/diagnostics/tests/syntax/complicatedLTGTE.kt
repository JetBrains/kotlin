// RUN_PIPELINE_TILL: BACKEND
// SKIP_TXT
// ISSUE: KT-52684

fun test(x: Int, y: Int) {
    if (x < (if (y >= 115) 1 else 2)) {
        Unit
    }
}

/* GENERATED_FIR_TAGS: comparisonExpression, functionDeclaration, ifExpression, integerLiteral */
