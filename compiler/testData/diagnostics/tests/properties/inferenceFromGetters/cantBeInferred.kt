// !WITH_NEW_INFERENCE
val <!NI;IMPLICIT_NOTHING_PROPERTY_TYPE!>x<!> get() = <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>()
val y get() = <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!>()

fun <E> foo(): E = null!!
fun <E> bar(): List<E> = null!!
