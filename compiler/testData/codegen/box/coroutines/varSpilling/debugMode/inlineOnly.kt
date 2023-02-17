// WITH_STDLIB
// FULL_JDK
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: ANDROID

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