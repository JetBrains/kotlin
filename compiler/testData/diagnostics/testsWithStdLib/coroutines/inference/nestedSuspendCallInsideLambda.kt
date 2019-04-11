// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

suspend fun wrapUp2() {
    withContext<Unit> {
        <!NI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>other<!>()
    }
}
suspend fun <T> withContext(block: suspend () -> T) {}
suspend fun <R> other(): R = TODO()