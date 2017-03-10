// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// WITH_COROUTINES
// FILE: promise.kt
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

class Promise<T>(private val executor: ((T) -> Unit) -> Unit) {
    private var value: Any? = null
    private var thenList: MutableList<(T) -> Unit>? = mutableListOf()

    init {
        executor {
            value = it
            for (resolve in thenList!!) {
                resolve(it)
            }
            thenList = null
        }
    }

    fun <S> then(onFulfilled: (T) -> S): Promise<S> {
        return Promise { resolve ->
            if (thenList != null) {
                thenList!!.add { resolve(onFulfilled(it)) }
            }
            else {
                resolve(onFulfilled(value as T))
            }
        }
    }
}

// FILE: queue.kt

private val queue = mutableListOf<() -> Unit>()

fun <T> postpone(computation: () -> T): Promise<T> {
    return Promise { resolve ->
        queue += {
            resolve(computation())
        }
    }
}

fun processQueue() {
    while (queue.isNotEmpty()) {
        queue.removeAt(0)()
    }
}

// FILE: await.kt
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

private var log = ""

private var inAwait = false

suspend fun <S> await(value: Promise<S>): S = suspendCoroutine { continuation ->
    if (inAwait) {
        throw IllegalStateException("Can't call await recursively")
    }
    inAwait = true
    postpone {
        value.then { result ->
            continuation.resume(result)
        }
    }
    inAwait = false
}

suspend fun <S> awaitAndLog(value: Promise<S>): S {
    log += "before await;"
    return await(value.then { result ->
        log += "after await: $result;"
        result
    })
}

fun <T> async(c: suspend () -> T): Promise<T> {
    return Promise { resolve ->
        c.startCoroutine(handleResultContinuation(resolve))
    }
}

fun <T> asyncOperation(resultSupplier: () -> T) = Promise<T> { resolve ->
    log += "before async;"
    postpone {
        val result = resultSupplier()
        log += "after async $result;"
        resolve(result)
    }
}

fun getLog() = log

// FILE: main.kt
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

private fun test() = async<String> {
    val o = await(asyncOperation { "O" })
    val k = awaitAndLog(asyncOperation { "K" })
    return@async o + k
}

fun box(): String {
    val resultPromise = test()
    var result: String? = null
    resultPromise.then { result = it }
    processQueue()

    if (result != "OK") return "fail1: $result"
    if (getLog() != "before async;after async O;before async;before await;after async K;after await: K;") return "fail2: ${getLog()}"

    return "OK"
}
