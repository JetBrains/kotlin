open class A
open class B: A()
open class D

open class Base<out T, out U>
open class Derived<out S>: Base<S, S>()

fun test(a: Base<D, B>) = a is <!CANNOT_CHECK_FOR_ERASED!>Derived<A><!>