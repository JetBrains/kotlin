// FIR_IDENTICAL
// SKIP_TXT

fun <X> select(vararg x: X): X = x[0]
fun <E> myE(): Out<E>? =  null

interface Out<out T>

fun foo(w: Out<*>) {
    // To have inference working we need both instances of `w` expressions captured to a different captured types instances
    // that return `false` when comparing them via `equals`.
    //
    // Otherwise, due to some complicated inferences implementation details that are not really relevant,
    // we've got NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER here.
    //
    // Anyway, this code should be definitely green and this test ensures it.
    select(w, myE() ?: w)
}
