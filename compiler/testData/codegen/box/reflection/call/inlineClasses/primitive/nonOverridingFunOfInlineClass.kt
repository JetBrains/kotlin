// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

@JvmInline
value class Z(val x: Int) {
    fun test(a: Int, b: Z, c: Z?) = "$x$a${b.x}${c!!.x}"
}

@JvmInline
value class S(val x: String) {
    fun test(a: Int, b: Z, c: Z?) = "$x$a${b.x}${c!!.x}"
}

@JvmInline
value class A(val x: Any) {
    fun test(a: Int, b: Z, c: Z?) = "$x$a${b.x}${c!!.x}"
}

fun box(): String {
    val two = Z(2)
    val four = Z(4)

    assertEquals("0124", Z::test.call(Z(0), 1, two, four))
    assertEquals("0124", Z(0)::test.call(1, two, four))

    assertEquals("0124", S::test.call(S("0"), 1, two, four))
    assertEquals("0124", S("0")::test.call(1, two, four))

    assertEquals("0124", A::test.call(A(0), 1, two, four))
    assertEquals("0124", A(0)::test.call(1, two, four))

    return "OK"
}
