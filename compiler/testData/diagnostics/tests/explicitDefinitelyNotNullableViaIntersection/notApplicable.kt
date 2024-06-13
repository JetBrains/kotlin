// LANGUAGE: +DefinitelyNonNullableTypes

fun <T : Any> foo(x: <!INCORRECT_LEFT_COMPONENT_OF_INTERSECTION!>T<!> & Any, y: <!INCORRECT_LEFT_COMPONENT_OF_INTERSECTION!>List<<!INCORRECT_LEFT_COMPONENT_OF_INTERSECTION!>String<!> & Any><!> & Any) {}

fun <F> bar1(x: <!INCORRECT_LEFT_COMPONENT_OF_INTERSECTION!>F?<!> & Any) {}
fun <F> bar2(x: F & <!INCORRECT_RIGHT_COMPONENT_OF_INTERSECTION!>Any?<!>) {}
fun <F> bar3(x: <!INCORRECT_LEFT_COMPONENT_OF_INTERSECTION!>(F?)<!> & Any) {}
fun <F> bar4(x: <!NULLABLE_ON_DEFINITELY_NOT_NULLABLE!>(F & Any)?<!>) {}

fun <F> bar5(x: F & <!INCORRECT_RIGHT_COMPONENT_OF_INTERSECTION!>String<!>) {}

fun <F> bar6(x: F & <!INCORRECT_RIGHT_COMPONENT_OF_INTERSECTION!>(F & Any)<!>) {}
fun <F> bar7(x: <!INCORRECT_LEFT_COMPONENT_OF_INTERSECTION!>(F & Any)<!> & Any) {}
fun <F> bar8(x: (F & Any).() -> Unit) {}
fun <F> (F & Any).bar9(x: () -> Unit) {}

fun <F> bar10(x: F & <!INCORRECT_LEFT_COMPONENT_OF_INTERSECTION!>Any<!> & String) {}
fun <F> bar11(x: <!INCORRECT_LEFT_COMPONENT_OF_INTERSECTION!>Double<!> & <!INCORRECT_LEFT_COMPONENT_OF_INTERSECTION!>Any<!> & String) {}
