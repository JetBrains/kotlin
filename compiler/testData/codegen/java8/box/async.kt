// WITH_RUNTIME
// FULL_JDK

import java.util.concurrent.CompletableFuture

fun foo(): CompletableFuture<String> = CompletableFuture.supplyAsync { "foo" }
fun bar(v: String): CompletableFuture<String> = CompletableFuture.supplyAsync { "bar with $v" }
fun exception(v: String): CompletableFuture<String> = CompletableFuture.supplyAsync { throw RuntimeException(v) }

fun foobar(x: String, y: String) = x + y

fun box(): String {
    var result = ""
    fun log(x: String) {
        if (result.isNotEmpty()) result += "\n"
        result += x
    }

    val future = async<String> {
        log("start")
        val x = await(foo())
        log("got '$x'")
        val y = foobar("123 ", await(bar(x)))
        log("got '$y' after '$x'")
        y
    }

    future.whenComplete { value, t ->
        log("completed with '$value'")
    }.join()

    val expectedResult =
    """
    |start
    |got 'foo'
    |got '123 bar with foo' after 'foo'
    |completed with '123 bar with foo'""".trimMargin().trim('\n', ' ')

    if (expectedResult != result) return result

    return "OK"
}

// LIBRARY CODE

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

    fun handleException(t: Throwable, c: Continuation<Nothing>) {
        future.completeExceptionally(t)
    }
}
