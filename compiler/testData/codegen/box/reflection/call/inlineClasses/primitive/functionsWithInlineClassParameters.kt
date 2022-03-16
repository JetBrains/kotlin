// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

inline class S(val value: Int) {
    operator fun plus(other: S): S = S(this.value + other.value)
}

class C {
    fun member(x: S, y: Int, z: S?): S = x + S(y) + z!!
}

fun topLevel(x: Int, y: S, z: S?): S = S(x) + y + z!!

fun S.extension1(y: S, z: S?): S = this + y + z!!

fun S?.extension2(y: S, z: S?) = this!! + y + z!!

fun S.extension3(): Int = value

fun S?.extension4(): Int = this!!.value

fun box(): String {
    val zero = S(0)
    val one = S(1)
    val two = S(2)
    val four = S(4)
    val seven = S(7)

    assertEquals(seven, C::member.call(C(), one, 2, four))
    assertEquals(seven, ::topLevel.call(1, two, four))
    assertEquals(seven, S::extension1.call(one, two, four))
    assertEquals(seven, S::extension2.call(one, two, four))
    assertEquals(0, S::extension3.call(zero))
    assertEquals(0, S?::extension4.call(zero))

    assertEquals(seven, C()::member.call(one, 2, four))
    assertEquals(seven, one::extension1.call(two, four))
    assertEquals(seven, one::extension2.call(two, four))
    assertEquals(0, zero::extension3.call())
    assertEquals(0, zero::extension4.call())

    return "OK"
}
