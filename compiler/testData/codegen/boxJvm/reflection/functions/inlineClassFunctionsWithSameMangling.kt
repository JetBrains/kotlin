// TARGET_BACKEND: JVM
// WITH_REFLECT

package test

import kotlin.test.assertEquals

@JvmInline
value class V(val x: Long) {
    operator fun times(scale: Int): V = V(x * scale)
    operator fun times(scale: Double): V = V((x * scale).toLong())
}

fun box(): String {
    val both = V::class.members.filter { it.name == "times" }
    val int = both.single { it.parameters.last().type.classifier == Int::class }
    assertEquals("fun test.V.times(kotlin.Int): test.V", int.toString())
    assertEquals(V(6L), int.call(V(2L), 3))
    val double = both.single { it.parameters.last().type.classifier == Double::class }
    assertEquals("fun test.V.times(kotlin.Double): test.V", double.toString())
    assertEquals(V(12L), double.call(V(3L), 4.0))
    return "OK"
}
