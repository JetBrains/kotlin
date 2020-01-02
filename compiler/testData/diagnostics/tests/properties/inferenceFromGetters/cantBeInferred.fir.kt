// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE
val x get() = foo()
val y get() = bar()

fun <E> foo(): E = null!!
fun <E> bar(): List<E> = null!!
