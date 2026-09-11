// RUN_PIPELINE_TILL: BACKEND
// ISSUES: KT-87442

tailrec fun countDown(n: Int): Int {
    if (n <= 0) return 0
    run {
        return countDown(n - 1)
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, comparisonExpression, functionDeclaration, ifExpression, integerLiteral,
lambdaLiteral, tailrec */
