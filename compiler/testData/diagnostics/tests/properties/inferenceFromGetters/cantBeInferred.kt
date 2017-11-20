// !WITH_NEW_INFERENCE
val x get() = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>()
val y get() = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!>()

fun <E> foo(): E = null!!
fun <E> bar(): List<E> = null!!
