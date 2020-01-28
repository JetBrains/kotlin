package kotlinx.coroutines

import kotlin.test.assertNotNull

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

operator fun Deferred<Int>.unaryPlus() = this
operator fun Deferred<Int>.plus(arg: Int) = this

fun moreFalsePositives() {
    +(async { 0 })
    async { -1 } + 1
}

suspend fun kt33741() {
    val d: Deferred<Int>? = async { 42 }
    assertNotNull(d)
    d.await()

    val d2: Deferred<Int>? = async { 42 }
    requireNotNull(d2)
    d2.await()

    val d3: Deferred<Int>? = GlobalScope.async { 42 }
    fooAsync(d3)
    println(d3?.await())
}

fun fooAsync(param: Deferred<Int>?): Deferred<Int>? {
    return param
}
