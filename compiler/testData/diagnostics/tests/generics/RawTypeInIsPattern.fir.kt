public fun foo(a: Any, b: Map) {
    when (a) {
        is Map<Int> -> {}
        is <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Map<!> -> {}
        is Map<out Any?, Any?> -> {}
        is Map<*, *> -> {}
        is Map<<!SYNTAX!><!>> -> {}
        is List<Map> -> {}
        is <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>List<!> -> {}
        is Int -> {}
        else -> {}
    }
}