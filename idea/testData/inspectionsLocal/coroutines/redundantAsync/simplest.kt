// WITH_RUNTIME

package kotlinx.coroutines.experimental

interface Deferred<T> {
    suspend fun await(): T
}

interface CoroutineContext

object DefaultDispatcher : CoroutineContext

enum class CoroutineStart {
    DEFAULT,
    LAZY,
    ATOMIC,
    UNDISPATCHED
}

interface Job

fun <T> async(
    context: CoroutineContext = DefaultDispatcher,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    parent: Job? = null,
    f: suspend () -> T
): Deferred<T> {
    TODO()
}

fun <T> runBlocking(
    context: CoroutineContext = DefaultDispatcher,
    f: suspend () -> T
) {
    TODO()
}

suspend fun <T> withContext(
    context: CoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    f: suspend () -> T
) {
    TODO()
}

suspend fun test() {
    <caret>async { 42 }.await()
}