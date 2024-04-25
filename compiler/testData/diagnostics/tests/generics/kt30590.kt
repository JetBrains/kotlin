// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
// NI_EXPECTED_FILE

interface A
fun <T: A, R: T> emptyStrangeMap(): Map<T, R> = TODO()
fun test7() : Map<A, A> = emptyStrangeMap()

fun test() = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyStrangeMap<!>()
