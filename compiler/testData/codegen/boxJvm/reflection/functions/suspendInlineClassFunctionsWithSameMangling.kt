// TARGET_BACKEND: JVM
// WITH_REFLECT
// WITH_COROUTINES

package test

import helpers.EmptyContinuation
import kotlin.coroutines.startCoroutine
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@JvmInline
value class V(val x: Long) {
    suspend fun times(scale: Int): V = V(x * scale)
    suspend fun times(scale: Double): V = V((x * scale).toLong())
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation())
}

fun box(): String {
    val both = V::class.members.filter { it.name == "times" }
    assertEquals(2, both.size)
    val int = both.single { it.parameters.last().type.classifier == Int::class }
    assertEquals("fun test.V.times(kotlin.Int): test.V", int.toString())
    assertTrue((int as KFunction<*>).isSuspend)
    val double = both.single { it.parameters.last().type.classifier == Double::class }
    assertEquals("fun test.V.times(kotlin.Double): test.V", double.toString())
    assertTrue((double as KFunction<*>).isSuspend)

    var result = ""
    builder {
        result += int.callSuspend(V(2L), 3)
        result += double.callSuspend(V(3L), 4.0)
    }
    assertEquals("V(x=6)V(x=12)", result)
    return "OK"
}
