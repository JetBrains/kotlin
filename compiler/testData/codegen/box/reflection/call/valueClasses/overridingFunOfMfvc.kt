// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// LANGUAGE: +ValueClasses

import kotlin.test.assertEquals

interface ITest {
    fun test(a: Int, b: Z, c: Z?): String
}

@JvmInline
value class Z(val x1: UInt, val x2: Int) : ITest {
    override fun test(a: Int, b: Z, c: Z?) = "$x1$x2$a${b.x1}${b.x2}${c!!.x1}${c!!.x2}"
}

@JvmInline
value class S(val x1: String, val x2: String) : ITest {
    override fun test(a: Int, b: Z, c: Z?) = "$x1$x2$a${b.x1}${b.x2}${c!!.x1}${c!!.x2}"
}

@JvmInline
value class A(val x1: Any, val x2: Any) : ITest {
    override fun test(a: Int, b: Z, c: Z?) = "$x1$x2$a${b.x1}${b.x2}${c!!.x1}${c!!.x2}"
}

fun box(): String {
    val two = Z(2U, 3)
    val four = Z(4U, 5)

    assertEquals("0912345", Z::test.call(Z(0U, 9), 1, two, four))
    assertEquals("0912345", Z(0U, 9)::test.call(1, two, four))

    assertEquals("0912345", S::test.call(S("0", "9"), 1, two, four))
    assertEquals("0912345", S("0", "9")::test.call(1, two, four))

    assertEquals("0912345", A::test.call(A(0U, 9), 1, two, four))
    assertEquals("0912345", A(0U, 9)::test.call(1, two, four))

    return "OK"
}
