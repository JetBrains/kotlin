package p

public fun foo(a: Any) {
    a is Map<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>
    a is <!NO_TYPE_ARGUMENTS_ON_RHS_OF_IS_EXPRESSION!>Map<!>
    a is Map<out Any?, Any?>
    a is Map<*, *>
    a is Map<<!SYNTAX!><!>>
    a is List<<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Map<!>>
    a is <!NO_TYPE_ARGUMENTS_ON_RHS_OF_IS_EXPRESSION!>List<!>
    a is Int

    (a <!USELESS_CAST!>as<!> <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Map<!>) is Int
}


