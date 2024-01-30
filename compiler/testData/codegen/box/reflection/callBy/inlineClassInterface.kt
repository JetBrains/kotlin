// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// !JVM_DEFAULT_MODE: disable


import kotlin.test.assertEquals
import kotlin.reflect.full.instanceParameter


interface IIC {
    fun f(i1: Int = 1): Int
}

inline class IC(val x: Int) : IIC {
    override fun f(i1: Int) = x + i1
}

interface Outer {
    @JvmInline
    value class DefaultImpls(val x: Int) {
        fun f(i1: Int = 1) = x + i1
    }
}

fun box(): String {
    val unbounded1 = IC::f
    assertEquals(3, unbounded1.callBy(mapOf(unbounded1.instanceParameter!! to IC(2))))
    assertEquals(7, unbounded1.callBy(mapOf(unbounded1.instanceParameter!! to IC(2), unbounded1.parameters[1] to 5)))

    val bounded1 = IC(2)::f
    assertEquals(3, bounded1.callBy(mapOf()))
    assertEquals(7, bounded1.callBy(mapOf(bounded1.parameters.first() to 5)))


    val unbounded2 = Outer.DefaultImpls::f
    assertEquals(3, unbounded2.callBy(mapOf(unbounded2.instanceParameter!! to Outer.DefaultImpls(2))))
    assertEquals(7, unbounded2.callBy(mapOf(unbounded2.instanceParameter!! to Outer.DefaultImpls(2), unbounded2.parameters[1] to 5)))

    val bounded2 = Outer.DefaultImpls(2)::f
    assertEquals(3, bounded2.callBy(mapOf()))
    assertEquals(7, bounded2.callBy(mapOf(bounded2.parameters.first() to 5)))

    return "OK"
}
