// FIR_IDENTICAL
fun <X> mySuspendable(block: (MyContinuation<X>) -> Unit): X = TODO()

interface MyContinuation<in Y> {
    fun cancel(): Unit
}

fun <Z> MyContinuation<Z>.resume(value: Z) {}

fun cancelOrProceed(handler: (cancel: () -> Unit, proceed: () -> Unit) -> Unit) {
    mySuspendable { x ->
        // Was exception: Expected expression 'FirCallableReferenceAccessImpl' to be resolved
        handler(x::cancel) { x.resume("") }
    }.length
}
