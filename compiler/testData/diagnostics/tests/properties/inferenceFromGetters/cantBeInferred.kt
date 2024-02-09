// NI_EXPECTED_FILE
val x get() = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>()
val y get() = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!>()

fun <E> foo(): E = null!!
fun <E> bar(): List<E> = null!!
