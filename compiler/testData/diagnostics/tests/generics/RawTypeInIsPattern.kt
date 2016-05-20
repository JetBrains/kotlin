public fun foo(a: Any, <!UNUSED_PARAMETER!>b<!>: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Map<!>) {
    when (a) {
        is Map<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!> -> {}
        is <!NO_TYPE_ARGUMENTS_ON_RHS!>Map<!> -> {}
        is Map<out Any?, Any?> -> {}
        is <!DUPLICATE_LABEL_IN_WHEN!>Map<*, *><!> -> {}
        is Map<<!SYNTAX!><!>> -> {}
        is List<<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Map<!>> -> {}
        is <!NO_TYPE_ARGUMENTS_ON_RHS!>List<!> -> {}
        is Int -> {}
        else -> {}
    }
}