// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// LANGUAGE: +ValueClasses


import kotlin.test.assertEquals
import kotlin.reflect.full.instanceParameter

interface IVC {
    fun f(i1: Int = 1): Int

}

@JvmInline
value class VC(val x: Int, val y: Int) : IVC {
    override fun f(i1: Int) = x + y + i1
}

fun box(): String {
    val unbounded = VC::f
    assertEquals(8, unbounded.callBy(mapOf(unbounded.instanceParameter!! to VC(2, 5))))
    assertEquals(12, unbounded.callBy(mapOf(unbounded.instanceParameter!! to VC(2, 5), unbounded.parameters[1] to 5)))
    
    val bounded = VC(2, 5)::f
    assertEquals(8, bounded.callBy(mapOf()))
    assertEquals(12, bounded.callBy(mapOf(bounded.parameters.first() to 5)))
    
    return "OK"
}
