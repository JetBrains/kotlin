// !DIAGNOSTICS: -UNUSED_PARAMETER

suspend fun wrapUp2() {
    withContext<Unit> {
        other()
    }
}
suspend fun <T> withContext(block: suspend () -> T) {}
suspend fun <R> other(): R = TODO()