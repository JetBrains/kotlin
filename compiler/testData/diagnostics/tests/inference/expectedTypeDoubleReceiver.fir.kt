// !LANGUAGE: +ExpectedTypeFromCast

fun <T> foo(): T = TODO()

class A {
    fun <T> fooA(): T = TODO()
}

fun <V> id(value: V) = value

val asA = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>().<!UNRESOLVED_REFERENCE!>fooA<!>() as A

val receiverParenthesized = (<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>()).<!UNRESOLVED_REFERENCE!>fooA<!>() as A
val no2A = A().<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>fooA<!>().<!UNRESOLVED_REFERENCE!>fooA<!>() as A

val correct1 = A().fooA() as A
val correct2 = foo<A>().fooA() as A
val correct3 = A().fooA<A>().fooA() as A
