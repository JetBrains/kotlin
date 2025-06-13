// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76766

fun test(e: Exception) {
    if (<!USELESS_IS_CHECK!>e is Error<!>) {
        throw e
    }
    if (<!USELESS_IS_CHECK!>e is AssertionError<!>) {
        throw e
    }
    if (<!USELESS_IS_CHECK!>e is NotImplementedError<!>) {
        throw e
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, intersectionType, isExpression, smartcast */
