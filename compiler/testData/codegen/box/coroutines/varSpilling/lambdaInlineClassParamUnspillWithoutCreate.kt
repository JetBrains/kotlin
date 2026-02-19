// WITH_STDLIB
// FULL_JDK

// TARGET_BACKEND: JVM

import kotlin.coroutines.*

@JvmInline
value class MyValueClass(val value: Any?)

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

var c: Continuation<Unit>? = null

suspend fun foo(block: suspend (MyValueClass, Unit, Unit) -> Unit) {
    block(MyValueClass("OK"), Unit, Unit)
}

fun box(): String {
    var res = "FAIL"
    builder {
        foo { it, _, _ ->
            suspendCoroutine { c = it }
            res = it.value as String
        }
    }
    c?.resume(Unit)
    return res
}
