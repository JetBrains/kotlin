// ISSUE: KT-89952
// IGNORE_BACKEND: JVM
// VALHALLA_VALUE_CLASSES
// LANGUAGE: +FullValueClasses
// WITH_REFLECT

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.javaMethod

value class Full(val a: Int, val b: Int)

@JvmInline
value class KInline(val x: Int)

suspend fun returnsFull(k: KInline): Full = Full(k.x, k.x)

fun box(): String {
    if (::returnsFull.javaMethod == null) return "Fail: javaMethod"
    var result: Any? = null
    suspend { ::returnsFull.callSuspend(KInline(5)) }.startCoroutine(Continuation(EmptyCoroutineContext) { result = it.getOrThrow() })
    if (result != Full(5, 5)) return "Fail: callSuspend: $result"
    return "OK"
}
