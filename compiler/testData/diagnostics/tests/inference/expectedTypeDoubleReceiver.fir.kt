// !WITH_NEW_INFERENCE
// !LANGUAGE: +ExpectedTypeFromCast

fun <T> foo(): T = TODO()

class A {
    fun <T> fooA(): T = TODO()
}

fun <V> id(value: V) = value

val asA = foo().<!UNRESOLVED_REFERENCE!>fooA<!>() as A

val receiverParenthesized = (foo()).<!UNRESOLVED_REFERENCE!>fooA<!>() as A
val no2A = A().fooA().<!UNRESOLVED_REFERENCE!>fooA<!>() as A

val correct1 = A().fooA() as A
val correct2 = foo<A>().fooA() as A
val correct3 = A().fooA<A>().fooA() as A

