// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76766

fun test(e: Exception) {
    if (<!IMPOSSIBLE_IS_CHECK_ERROR!>e is Error<!>) {
        throw e
    }
    if (<!IMPOSSIBLE_IS_CHECK_ERROR!>e is AssertionError<!>) {
        throw e
    }
    if (<!IMPOSSIBLE_IS_CHECK_ERROR!>e is NotImplementedError<!>) {
        throw e
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, intersectionType, isExpression, smartcast */
