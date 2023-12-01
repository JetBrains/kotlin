// TARGET_BACKEND: JVM
// WITH_REFLECT
// !JVM_DEFAULT_MODE: all
// IGNORE_BACKEND: ANDROID


import kotlin.test.assertEquals
import kotlin.reflect.full.instanceParameter


interface IIC {
    fun f(i1: Int = 1): Int

}

inline class IC(val x: Int) : IIC {
    override fun f(i1: Int) = x + i1
}

fun box(): String {
    val unbounded = IC::f
    assertEquals(3, unbounded.callBy(mapOf(unbounded.instanceParameter!! to IC(2))))
    assertEquals(7, unbounded.callBy(mapOf(unbounded.instanceParameter!! to IC(2), unbounded.parameters[1] to 5)))
    
    val bounded = IC(2)::f
    assertEquals(3, bounded.callBy(mapOf()))
    assertEquals(7, bounded.callBy(mapOf(bounded.parameters.first() to 5)))
    
    return "OK"
}
