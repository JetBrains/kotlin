// IGNORE_BACKEND_FIR: JVM_IR
// SKIP_JDK6
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// COMMON_COROUTINES_TEST
// FULL_JDK
// WITH_COROUTINES

import java.util.concurrent.CompletableFuture
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

fun exception(v: String): CompletableFuture<String> = CompletableFuture.supplyAsync { throw RuntimeException(v) }

fun foobar(x: String, y: String) = x + y

fun box(): String {
    var result = ""

    val future = async<String>() {
        try {
            await(exception("OK"))
        } catch (e: Exception) {
            result = e.cause?.message!!
        }
        "56"
    }

    future.join()

    if (future.get() != "56") return "fail: ${future.get()}"

    if (result != "OK") return "fail notOk"

    val future2 = async<String>() {
        await(exception("OK"))
        "fail"
    }

    try {
        future2.get()
    } catch (e: Exception) {
        if (e.cause!!.message != "OK") return "fail message: ${e.cause!!.message}"
        return "OK"
    }

    return "No exception"
}

fun <T> async(c: suspend () -> T): CompletableFuture<T> {
    val future = CompletableFuture<T>()
    c.startCoroutine(object : helpers.ContinuationAdapter<T>() {
        override val context = EmptyCoroutineContext

        override fun resume(data: T) {
            future.complete(data)
        }

        override fun resumeWithException(exception: Throwable) {
            future.completeExceptionally(exception)
        }
    })
    return future
}

suspend fun <V> await(f: CompletableFuture<V>) = suspendCoroutineUninterceptedOrReturn<V> { machine ->
    f.whenComplete { value, throwable ->
        if (throwable == null)
            machine.resume(value)
        else
            machine.resumeWithException(throwable)
    }
    COROUTINE_SUSPENDED
}
