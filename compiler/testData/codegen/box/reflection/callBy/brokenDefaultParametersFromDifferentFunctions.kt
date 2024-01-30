// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// LANGUAGE: +ValueClasses
// !JVM_DEFAULT_MODE: disable

import kotlin.test.assertEquals
import kotlin.reflect.full.instanceParameter

interface I1 {
    fun f(i1: Int = 1, i2: Int): Int
}

interface I2 {
    fun f(i1: Int, i2: Int = 2): Int
}

data class DC(val x: Int, val y: Int) : I1, I2 {
    override fun f(i1: Int, i2: Int) = x + y + i1
}

fun dataClass() {
    val unbounded = DC::f
    assertEquals(111, unbounded.callBy(mapOf(unbounded.instanceParameter!! to DC(10, 100))))

    val bounded = DC(10, 100)::f
    assertEquals(111, bounded.callBy(mapOf()))
}

@JvmInline
value class VC(val x: Int, val y: Int) : I1, I2 {
    override fun f(i1: Int, i2: Int) = x + y + i1
}

fun valueClass() {
    val unbounded = VC::f
    assertEquals(111, unbounded.callBy(mapOf(unbounded.instanceParameter!! to VC(10, 100))))

    val bounded = VC(10, 100)::f
    assertEquals(111, bounded.callBy(mapOf()))
}

@JvmInline
value class IC(val x: Int) : I1, I2 {
    override fun f(i1: Int, i2: Int) = x + i1
}

fun inlineClass() {
    val unbounded = IC::f
    assertEquals(11, unbounded.callBy(mapOf(unbounded.instanceParameter!! to IC(10))))

    val bounded = IC(10)::f
    assertEquals(11, bounded.callBy(mapOf()))
}

fun box(): String {
    dataClass()
    inlineClass()
    valueClass()
    return "OK"
}
