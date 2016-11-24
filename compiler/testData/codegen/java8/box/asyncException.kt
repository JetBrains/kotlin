// WITH_RUNTIME
// FULL_JDK

import java.util.concurrent.CompletableFuture

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

fun <T> async(coroutine c: FutureController<T>.() -> Continuation<Unit>): CompletableFuture<T> {
    val controller = FutureController<T>()
    c(controller).resume(Unit)
    return controller.future
}

class FutureController<T> {
    val future = CompletableFuture<T>()

    suspend fun <V> await(f: CompletableFuture<V>) = suspendWithCurrentContinuation<V> { machine ->
        f.whenComplete { value, throwable ->
            if (throwable == null)
                machine.resume(value)
            else
                machine.resumeWithException(throwable)
        }
    }

    operator fun handleResult(value: T, c: Continuation<Nothing>) {
        future.complete(value)
    }

    operator fun handleException(t: Throwable, c: Continuation<Nothing>) {
        future.completeExceptionally(t)
    }
}
