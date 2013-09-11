public fun foo(a: Any, <!UNUSED_PARAMETER!>b<!>: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Map<!>) {
    when (a) {
        is Map<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!> -> {}
        is <!NO_TYPE_ARGUMENTS_ON_RHS_OF_IS_EXPRESSION!>Map<!> -> {}
        is Map<out Any?, Any?> -> {}
        is Map<*, *> -> {}
        is Map<<!SYNTAX!><!>> -> {}
        is List<<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Map<!>> -> {}
        is <!NO_TYPE_ARGUMENTS_ON_RHS_OF_IS_EXPRESSION!>List<!> -> {}
        is Int -> {}
        else -> {}
    }
}