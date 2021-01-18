package p

public fun foo(a: Any) {
    a is <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Map<Int><!>
    a is <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Map<!>
    a is Map<out Any?, Any?>
    a is Map<*, *>
    a is Map<<!SYNTAX!><!>>
    a is List<Map>
    a is <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>List<!>
    a is Int

    (a as <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Map<!>) is Int
}