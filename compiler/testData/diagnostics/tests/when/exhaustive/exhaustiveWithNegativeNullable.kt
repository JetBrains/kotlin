// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +DataFlowBasedExhaustiveness

fun foo(b: Boolean?): Int {
    if (b == null) return 1
    return when (<!DEBUG_INFO_SMARTCAST!>b<!>) {
        true -> 2
        false -> 3
    }
}

fun bar(b: Boolean?): Int {
    if (b != null) return 1
    return <!NO_ELSE_IN_WHEN!>when<!> (<!DEBUG_INFO_CONSTANT!>b<!>) {
        null -> 2
    }
}
