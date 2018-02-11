// !WITH_NEW_INFERENCE
// !LANGUAGE: +ExpectedTypeFromCast

fun <T> foo(): T = TODO()

class A {
    fun <T> fooA(): T = TODO()
}

fun <V> id(value: V) = value

val asA = <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>().<!NI;UNRESOLVED_REFERENCE, OI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>fooA<!>() as A

val receiverParenthesized = (<!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>()).<!NI;UNRESOLVED_REFERENCE, OI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>fooA<!>() as A
val no2A = A().<!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>fooA<!>().<!NI;UNRESOLVED_REFERENCE, OI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>fooA<!>() as A

val correct1 = A().fooA() as A
val correct2 = foo<A>().fooA() as A
val correct3 = A().fooA<A>().fooA() as A

