// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76766

fun test(e: Exception) {
    if (e is Error) {
        throw e
    }
    if (e is AssertionError) {
        throw e
    }
    if (<!USELESS_IS_CHECK!>e is NotImplementedError<!>) {
        throw e
    }
}
