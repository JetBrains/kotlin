package kotlinx.coroutines.experimental

interface Deferred<T> {
    suspend fun await(): T
}

interface CoroutineContext

object DefaultContext : CoroutineContext

enum class CoroutineStart {
    DEFAULT,
    LAZY,
    ATOMIC,
    UNDISPATCHED
}

interface Job

fun <T> async(
    context: CoroutineContext = DefaultContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    parent: Job? = null,
    f: suspend () -> T
): Deferred<T> {
    TODO()
}

fun test() {
    async { 42 }
}

fun useIt(d: Deferred<Int>) {}

fun falsePositives() {
    async { 3 }.await()
    val res = async { 13 }
    useIt(async { 7 })
}
