// !LANGUAGE: +DefinitelyNonNullableTypes

fun <T : Comparable<T & Any>> sort1() {}
fun <T : Comparable<T & Any>?> sort2() {}

class A1<K : Comparable<K & Any>>
class A2<K : Comparable<K & Any>?>

fun <R : T & Any, T> bar() {}

fun <<!CYCLIC_GENERIC_UPPER_BOUND!>E : E  & Any<!>> baz() {}
