// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76766

fun test(e: Exception) {
    if (<!IMPOSSIBLE_IS_CHECK_WARNING!>e is Error<!>) {
        throw e
    }
    if (<!IMPOSSIBLE_IS_CHECK_WARNING!>e is AssertionError<!>) {
        throw e
    }
    if (<!IMPOSSIBLE_IS_CHECK_WARNING!>e is NotImplementedError<!>) {
        throw e
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, intersectionType, isExpression, smartcast */
