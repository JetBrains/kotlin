// TARGET_BACKEND: JVM
// WITH_STDLIB

import kotlin.coroutines.*

object MyObject2 {
    @JvmStatic
    suspend fun enable(o: Any) {
        if (o.hashCode() != 0) {
            suspendCoroutine<Any> {}
        }
    }
}

fun go(block: suspend () -> Unit) {
    block.startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })
}

fun box(): String {
    go {
        MyObject2.enable("")
    }
    return "OK"
}
