// FIR_IDENTICAL
// SKIP_TXT

fun <X> select(vararg x: X): X = x[0]
fun <E> myE(): Out<E>? =  null

interface Out<out T>

fun foo(w: Out<*>) {
    // While it's controversial that we used Any? as a type argument here
    // There's no much sense fixing it soon, as it's a rather reguler use case (when Out is List an myE() is something like emptyList())
    //
    // What matters here is that due to some implementation details that has no sense diving it, to make it work both instances
    // of `w` expressions should be captured to a different captured types.
    // That is why this tests has been attached to the commit.
    select(w, myE() /* type argument is inferred to Any? */ ?: w)
}
