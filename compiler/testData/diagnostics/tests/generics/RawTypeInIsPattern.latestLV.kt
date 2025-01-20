// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
public fun foo(a: Any, b: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Map<!>) {
    when (a) {
        is Map<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!> -> {}
        is Map -> {}
        is Map<out Any?, Any?> -> {}
        is <!DUPLICATE_BRANCH_CONDITION_IN_WHEN!>Map<*, *><!> -> {}
        is Map<<!SYNTAX!><!>> -> {}
        is List<<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Map<!>> -> {}
        is List -> {}
        is Int -> {}
        else -> {}
    }
}
