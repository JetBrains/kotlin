// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

@JvmInline
value class Z(val x: Int)

class Outer(val z1: Z, val z2: Z?) {
    inner class Inner(val z3: Z, val z4: Z?) {
        val test = "$z1 $z2 $z3 $z4"
    }
}

@JvmInline
value class InlineNonNullOuter(val z1: Z) {
    @Suppress("INNER_CLASS_INSIDE_VALUE_CLASS")
    inner class Inner(val z2: Z, val z3: Z?) {
        val test = "$z1 $z2 $z3"
    }
}

@JvmInline
value class InlineNullableOuter(val z1: Z?) {
    @Suppress("INNER_CLASS_INSIDE_VALUE_CLASS")
    inner class Inner(val z2: Z, val z3: Z?) {
        val test = "$z1 $z2 $z3"
    }
}

fun box(): String {
    val z1 = Z(1)
    val z2 = Z(2)
    val z3 = Z(3)
    val z4 = Z(4)

    val outer = ::Outer.call(z1, z2)
    assertEquals(z1, outer.z1)
    assertEquals(z2, outer.z2)

    assertEquals("Z(x=1) Z(x=2) Z(x=3) Z(x=4)", Outer::Inner.call(outer, z3, z4).test)
    assertEquals("Z(x=1) Z(x=2) Z(x=2) Z(x=4)", outer::Inner.call(z2, z4).test)

    val inlineNonNullOuter = InlineNonNullOuter(z1)
    assertEquals("Z(x=1) Z(x=2) Z(x=3)", InlineNonNullOuter::Inner.call(inlineNonNullOuter, z2, z3).test)
    assertEquals("Z(x=1) Z(x=2) Z(x=2)", inlineNonNullOuter::Inner.call(z2, z2).test)

    val inlineNullableOuter = InlineNullableOuter(z1)
    assertEquals("Z(x=1) Z(x=2) Z(x=3)", InlineNullableOuter::Inner.call(inlineNullableOuter, z2, z3).test)
    assertEquals("Z(x=1) Z(x=2) Z(x=2)", inlineNullableOuter::Inner.call(z2, z2).test)

    return "OK"
}
