// LANGUAGE_VERSION: 1.2
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// IGNORE_BACKEND: JS
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED

fun runCustomLambdaAsCoroutine(e: Throwable? = null, x: (Continuation<String>) -> Any?): String {
    var result = "fail"
    var wasIntercepted = false
    val c = (x as suspend () -> String).createCoroutine(object: helpers.ContinuationAdapter<String>() {
        override fun resumeWithException(exception: Throwable) {
            throw exception
        }

        override val context: CoroutineContext
            get() = object: ContinuationInterceptor {
                override fun <R> fold(initial: R, operation: (R, CoroutineContext.Element) -> R): R {
                    throw IllegalStateException()
                }

                override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? {
                    if (key == ContinuationInterceptor.Key) {
                        return this as E
                    }
                    return null
                }

                override fun <T> interceptContinuation(continuation: Continuation<T>) = object : helpers.ContinuationAdapter<T>() {
                    override val context: CoroutineContext
                        get() = continuation.context

                    override fun resume(value: T) {
                        wasIntercepted = true
                        continuation.resume(value)
                    }

                    override fun resumeWithException(exception: Throwable) {
                        wasIntercepted = true
                        continuation.resumeWithException(exception)
                    }
                }

                override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext {
                    throw IllegalStateException()
                }

                override fun plus(context: CoroutineContext): CoroutineContext {
                    throw IllegalStateException()
                }

                override val key: CoroutineContext.Key<*>
                    get() = ContinuationInterceptor.Key
            }

        override fun resume(value: String) {
            result = value
        }
    })

    if (e != null)
        c.resumeWithException(e)
    else
        c.resume(Unit)

    if (!wasIntercepted) return "was not intercepted"

    return result
}

fun box(): String {
    val x = runCustomLambdaAsCoroutine {
        it.resume("OK")
        COROUTINE_SUSPENDED
    }

    if (x != "OK") return "fail 1: $x"

    val y = runCustomLambdaAsCoroutine {
        "OK"
    }

    if (y != "OK") return "fail 2: $x"


    try {
        runCustomLambdaAsCoroutine(RuntimeException("OK")) {
            throw RuntimeException("fail 3")
        }
    } catch(e: Exception) {
        return e.message!!
    }

    return "fail 3"
}
