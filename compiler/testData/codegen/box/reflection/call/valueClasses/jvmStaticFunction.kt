// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// WITH_REFLECT
// LANGUAGE: +ValueClasses

import kotlin.reflect.KFunction
import kotlin.test.assertEquals

@JvmInline
value class Z(val value1: UInt, val value2: Int) {
    operator fun plus(other: Z): Z = Z(this.value1 + other.value1, this.value2 + other.value2)
}

object C {
    @JvmStatic
    fun foo(x: Z, y1: UInt, y2: Int, z: Z?): Z = x + Z(y1, y2) + z!!
}

interface I {
    companion object {
        @JvmStatic
        fun bar(x1: UInt, x2: Int, y: Z, z: Z?): Z = Z(x1, x2) + y + z!!
    }
}

fun box(): String {
    val one = Z(1U, -1)
    val two = Z(2U, -2)
    val four = Z(4U, -4)
    val seven = Z(7U, -7)

    assertEquals(seven, C::foo.call(one, 2U, -2, four))
    assertEquals(seven, (I)::bar.call(1U, -1, two, four))

    val unboundFoo = C::class.members.single { it.name == "foo" } as KFunction<*>
    assertEquals(seven, unboundFoo.call(C, one, 2U, -2, four))

    val unboundBar = I.Companion::class.members.single { it.name == "bar" } as KFunction<*>
    assertEquals(seven, unboundBar.call(I, 1U, -1, two, four))

    return "OK"
}
