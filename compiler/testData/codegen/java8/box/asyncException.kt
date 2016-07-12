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

    java.lang.Thread.sleep(1000)

    return result
}

fun <T> async(coroutine c: FutureController<T>.() -> Continuation<Unit>): CompletableFuture<T> {
    val controller = FutureController<T>()
    c(controller).resume(Unit)
    return controller.future
}

class FutureController<T> {
    val future = CompletableFuture<T>()


    suspend fun <V> await(f: CompletableFuture<V>, machine: Continuation<V>) {
        f.whenComplete { value, throwable ->
            try {
                if (throwable == null)
                    machine.resume(value)
                else
                    machine.resumeWithException(throwable)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
    }

    operator fun handleResult(value: T, c: Continuation<Nothing>) {
        future.complete(value)
    }

    fun handleException(t: Throwable, c: Continuation<Nothing>) {
        future.completeExceptionally(t)
    }
}
