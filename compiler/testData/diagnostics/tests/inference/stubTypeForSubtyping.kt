// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-48110

fun test_1(s: String?, flag: Boolean): Int? {
    return run {
        when (flag) {
            true -> s?.let {
                return@run 42
            }
            false -> s?.let {
                return@run 24
            }
        }
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, lambdaLiteral, nullableType, safeCall,
smartcast, whenExpression, whenWithSubject */
