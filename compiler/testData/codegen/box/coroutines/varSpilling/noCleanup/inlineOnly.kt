// WITH_STDLIB
// FULL_JDK
// TARGET_BACKEND: JVM
// API_VERSION: LATEST
// PREFER_IN_TEST_OVER_STDLIB

// FILE: Spilling.kt

package kotlin.coroutines.jvm.internal

@Suppress("UNUSED_PARAMETER", "unused")
internal fun nullOutSpilledVariable(value: Any?): Any? = value

// FILE: test.kt


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