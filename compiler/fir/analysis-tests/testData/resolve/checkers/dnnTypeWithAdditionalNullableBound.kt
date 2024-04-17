// ISSUE: KT-51742

// This test mostly makes sure that frontend is not failing with an exception as in K1

fun <T> f(x: <!INCORRECT_LEFT_COMPONENT_OF_INTERSECTION!>T<!> & Any) where T : CharSequence, T : Any? {}

class A<T>(val x: <!INCORRECT_LEFT_COMPONENT_OF_INTERSECTION!>T<!> & Any) where T : CharSequence, T : Any? {}
