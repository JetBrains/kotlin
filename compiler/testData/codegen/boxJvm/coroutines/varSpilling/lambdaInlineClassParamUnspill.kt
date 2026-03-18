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

suspend fun foo(block: suspend (MyValueClass) -> Unit) {
    block(MyValueClass("OK"))
}

fun box(): String {
    var res = "FAIL"
    builder {
        foo {
            suspendCoroutine { c = it }
            res = it.value as String
        }
    }
    c?.resume(Unit)
    return res
}
