public fun foo(a: Any, b: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Map<!>) {
    when (a) {
        is <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Map<Int><!> -> {}
        is <!NO_TYPE_ARGUMENTS_ON_RHS!>Map<!> -> {}
        is Map<out Any?, Any?> -> {}
        is Map<*, *> -> {}
        is Map<<!SYNTAX!><!>> -> {}
        is List<Map> -> {}
        is <!NO_TYPE_ARGUMENTS_ON_RHS!>List<!> -> {}
        is Int -> {}
        else -> {}
    }
}