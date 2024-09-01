// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

@JvmInline
value class S(val x: String?)

class Outer(val z1: S, val z2: S?) {
    inner class Inner(val z3: S, val z4: S?) {
        val test = "$z1 $z2 $z3 $z4"
    }
}

@JvmInline
value class InlineNonNullOuter(val z1: S) {
    @Suppress("INNER_CLASS_INSIDE_VALUE_CLASS")
    inner class Inner(val z2: S, val z3: S?) {
        val test = "$z1 $z2 $z3"
    }
}

@JvmInline
value class InlineNullableOuter(val z1: S?) {
    @Suppress("INNER_CLASS_INSIDE_VALUE_CLASS")
    inner class Inner(val z2: S, val z3: S?) {
        val test = "$z1 $z2 $z3"
    }
}

fun box(): String {
    val z1 = S("1")
    val z2 = S("2")
    val z3 = S("3")
    val z4 = S("4")

    val outer = ::Outer.call(z1, z2)
    assertEquals(z1, outer.z1)
    assertEquals(z2, outer.z2)

    assertEquals("S(x=1) S(x=2) S(x=3) S(x=4)", Outer::Inner.call(outer, z3, z4).test)
    assertEquals("S(x=1) S(x=2) S(x=2) S(x=4)", outer::Inner.call(z2, z4).test)

    val inlineNonNullOuter = InlineNonNullOuter(z1)
    assertEquals("S(x=1) S(x=2) S(x=3)", InlineNonNullOuter::Inner.call(inlineNonNullOuter, z2, z3).test)
    assertEquals("S(x=1) S(x=2) S(x=2)", inlineNonNullOuter::Inner.call(z2, z2).test)

    val inlineNullableOuter = InlineNullableOuter(z1)
    assertEquals("S(x=1) S(x=2) S(x=3)", InlineNullableOuter::Inner.call(inlineNullableOuter, z2, z3).test)
    assertEquals("S(x=1) S(x=2) S(x=2)", inlineNullableOuter::Inner.call(z2, z2).test)

    return "OK"
}
