// !LANGUAGE: +DefinitelyNonNullableTypes

fun <T : <!INCORRECT_LEFT_COMPONENT_OF_INTERSECTION!>Comparable<T & Any><!>> sort1() {}
fun <T : Comparable<T & Any>?> sort2() {}

class A1<K : Comparable<<!INCORRECT_LEFT_COMPONENT_OF_INTERSECTION!>K<!> & Any>>
class A2<K : Comparable<<!INCORRECT_LEFT_COMPONENT_OF_INTERSECTION!>K<!> & Any>?>

fun <R : T & Any, T> bar() {}

fun <<!CYCLIC_GENERIC_UPPER_BOUND!>E : <!INCORRECT_LEFT_COMPONENT_OF_INTERSECTION!>E & Any<!><!>> baz1() {}
fun <<!CYCLIC_GENERIC_UPPER_BOUND!>E : <!INCORRECT_LEFT_COMPONENT_OF_INTERSECTION!>E & Any<!><!>> E?.baz2() {}
