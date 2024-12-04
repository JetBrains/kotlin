// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// LANGUAGE: +ValueClasses

import kotlin.test.assertEquals


@JvmInline
value class Z(val x1: UInt, val x2: Int)

class Outer(val z1: Z, val z2: Z?) {
    inner class Inner(val z3: Z, val z4: Z?) {
        val test = "$z1 $z2 $z3 $z4"
    }
}

@JvmInline
value class ValueNonNullOuter(val z11: Z, val z12: Z) {
    class Inner(val t: ValueNonNullOuter, val z2: Z, val z3: Z?) {
        val test = "${t.z11} ${t.z12} $z2 $z3"
    }
}

@JvmInline
value class ValueNullableOuter(val z11: Z?, val z12: Z?) {
    class Inner(val t: ValueNullableOuter, val z2: Z, val z3: Z?) {
        val test = "${t.z11} ${t.z12} $z2 $z3"
    }
}

fun box(): String {
    val z1 = Z(1U, -1)
    val z2 = Z(2U, -2)
    val z3 = Z(3U, -3)
    val z4 = Z(4U, -4)

    val outer = ::Outer.call(z1, z2)
    assertEquals(z1, outer.z1)
    assertEquals(z2, outer.z2)

    assertEquals("Z(x1=1, x2=-1) Z(x1=2, x2=-2) Z(x1=3, x2=-3) Z(x1=4, x2=-4)", Outer::Inner.call(outer, z3, z4).test)
    assertEquals("Z(x1=1, x2=-1) Z(x1=2, x2=-2) Z(x1=2, x2=-2) Z(x1=4, x2=-4)", outer::Inner.call(z2, z4).test)

    val valueNonNullOuter = ValueNonNullOuter(z1, z4)
    assertEquals("Z(x1=1, x2=-1) Z(x1=4, x2=-4) Z(x1=2, x2=-2) Z(x1=3, x2=-3)", ValueNonNullOuter::Inner.call(valueNonNullOuter, z2, z3).test)

    val valueNullableOuter = ValueNullableOuter(z1, z4)
    assertEquals("Z(x1=1, x2=-1) Z(x1=4, x2=-4) Z(x1=2, x2=-2) Z(x1=3, x2=-3)", ValueNullableOuter::Inner.call(valueNullableOuter, z2, z3).test)

    return "OK"
}
