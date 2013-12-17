class MMap<K, out V>

public fun foo(a: Any, <!UNUSED_PARAMETER!>b<!>: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Map<!>) {
    when (a) {
        is Map<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!> -> {}
        is <!NO_TYPE_ARGUMENTS_ON_RHS!>Map<!> -> {}
        is MMap<out Any?, Any?> -> {}
        is MMap<*, *> -> {}
        is Map<<!SYNTAX!><!>> -> {}
        is List<<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Map<!>> -> {}
        is <!NO_TYPE_ARGUMENTS_ON_RHS!>List<!> -> {}
        is Int -> {}
        else -> {}
    }
}