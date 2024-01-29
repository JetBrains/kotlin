// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// LANGUAGE: +ValueClasses
// !JVM_DEFAULT_MODE: all


import kotlin.test.assertEquals
import kotlin.reflect.full.instanceParameter

interface IVC {
    fun f(i1: Int = 1): Int
}

@JvmInline
value class VC(val x: Int, val y: Int) : IVC {
    override fun f(i1: Int) = x + y + i1
}

interface Outer {
    @JvmInline
    value class DefaultImpls(val x: Int, val y: Int) {
        fun f(i1: Int = 1) = x + y + i1
    }
}


fun box(): String {
    val unbounded1 = VC::f
    assertEquals(8, unbounded1.callBy(mapOf(unbounded1.instanceParameter!! to VC(2, 5))))
    assertEquals(12, unbounded1.callBy(mapOf(unbounded1.instanceParameter!! to VC(2, 5), unbounded1.parameters[1] to 5)))

    val bounded1 = VC(2, 5)::f
    assertEquals(8, bounded1.callBy(mapOf()))
    assertEquals(12, bounded1.callBy(mapOf(bounded1.parameters.first() to 5)))


    val unbounded2 = Outer.DefaultImpls::f
    assertEquals(8, unbounded2.callBy(mapOf(unbounded2.instanceParameter!! to Outer.DefaultImpls(2, 5))))
    assertEquals(12, unbounded2.callBy(mapOf(unbounded2.instanceParameter!! to Outer.DefaultImpls(2, 5), unbounded2.parameters[1] to 5)))

    val bounded2 = Outer.DefaultImpls(2, 5)::f
    assertEquals(8, bounded2.callBy(mapOf()))
    assertEquals(12, bounded2.callBy(mapOf(bounded2.parameters.first() to 5)))

    return "OK"
}
