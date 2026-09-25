// IGNORE_BACKEND: JVM
// VALHALLA_VALUE_CLASSES
// LANGUAGE: +FullValueClasses
// WITH_REFLECT

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaMethod

value class Full(val a: Int, val b: Int)

@JvmInline
value class WrapFull(val f: Full)

@JvmInline
value class KInline(val x: Int)

fun withDefault(w: WrapFull = WrapFull(Full(1, 2))): Int = w.f.a

class WithDefault(val w: WrapFull = WrapFull(Full(3, 4)), val n: Int = 0)

suspend fun returnsFull(k: KInline): Full = Full(k.x, k.x)

fun box(): String {
    if (::withDefault.callBy(emptyMap()) != 1) return "withDefault"
    val constructor = WithDefault::class.primaryConstructor!!
    if (constructor.callBy(mapOf(constructor.parameters[1] to 5)).w != WrapFull(Full(3, 4))) return "WithDefault"
    if (::returnsFull.javaMethod == null) return "javaMethod"
    var result: Any? = null
    suspend { ::returnsFull.callSuspend(KInline(5)) }.startCoroutine(Continuation(EmptyCoroutineContext) { result = it.getOrThrow() })
    if (result != Full(5, 5)) return "callSuspend: $result"
    return "OK"
}
