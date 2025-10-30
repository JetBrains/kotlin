// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// JVM_DEFAULT_MODE: disable

import kotlin.test.assertEquals
import kotlin.reflect.full.instanceParameter

interface IIC {
    fun f(i1: Int = 1): Int
}

@JvmInline
value class IC(val x: String) : IIC {
    override fun f(i1: Int) = x.length + i1
}

interface Outer {
    @JvmInline
    value class DefaultImpls(val x: String) {
        fun f(i1: Int = 1) = x.length + i1
    }
}

fun box(): String {
    val unbounded1 = IC::f
    assertEquals(IC("ab").f(), unbounded1.callBy(mapOf(unbounded1.instanceParameter!! to IC("ab"))))
    assertEquals(IC("ab").f(5), unbounded1.callBy(mapOf(unbounded1.instanceParameter!! to IC("ab"), unbounded1.parameters[1] to 5)))

    val bounded1 = IC("ab")::f
    assertEquals(IC("ab").f(), bounded1.callBy(mapOf()))
    assertEquals(IC("ab").f(5), bounded1.callBy(mapOf(bounded1.parameters.first() to 5)))


    val unbounded2 = Outer.DefaultImpls::f
    assertEquals(Outer.DefaultImpls("ab").f(), unbounded2.callBy(mapOf(unbounded2.instanceParameter!! to Outer.DefaultImpls("ab"))))
    assertEquals(Outer.DefaultImpls("ab").f(5), unbounded2.callBy(mapOf(unbounded2.instanceParameter!! to Outer.DefaultImpls("ab"), unbounded2.parameters[1] to 5)))

    val bounded2 = Outer.DefaultImpls("ab")::f
    assertEquals(Outer.DefaultImpls("ab").f(), bounded2.callBy(mapOf()))
    assertEquals(Outer.DefaultImpls("ab").f(5), bounded2.callBy(mapOf(bounded2.parameters.first() to 5)))

    return "OK"
}