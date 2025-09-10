// WITH_STDLIB
// FULL_JDK
// TARGET_BACKEND: JVM
// DISABLE_IR_VISIBILITY_CHECKS: JVM_IR

import kotlin.coroutines.*

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
inline fun f(a: Int): Int = a

fun box(): String {
    var res = "FAIL"
    suspend {
        f(0)
        res = "OK"
        suspendCoroutine<Unit> {}
    }.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
    return res
}