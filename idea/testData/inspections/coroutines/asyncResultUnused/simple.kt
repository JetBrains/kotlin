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

class User

interface DbHandler {
    fun getUser(id: Long): Deferred<User>
    fun doStuff(): Deferred<Unit>
}

fun DbHandler.test() {
    getUser(42L)
    val user = getUser(42L).await()
    doStuff()
    doStuff().await()
}

