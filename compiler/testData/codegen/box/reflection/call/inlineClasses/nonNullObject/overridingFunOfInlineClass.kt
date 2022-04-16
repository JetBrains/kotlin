// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

interface ITest {
    fun test(a: String, b: S, c: S?): String
}

inline class S(val x: String) : ITest {
    override fun test(a: String, b: S, c: S?) = "$x$a${b.x}${c!!.x}"
}

inline class Z(val x: Int) : ITest {
    override fun test(a: String, b: S, c: S?) = "$x$a${b.x}${c!!.x}"
}

inline class A(val x: Any) : ITest {
    override fun test(a: String, b: S, c: S?) = "$x$a${b.x}${c!!.x}"
}

fun box(): String {
    val plus = S("+")
    val aster = S("*")

    assertEquals("42-+*", S::test.call(S("42"), "-", plus, aster))
    assertEquals("42-+*", S("42")::test.call("-", plus, aster))

    assertEquals("42-+*", Z::test.call(Z(42), "-", plus, aster))
    assertEquals("42-+*", Z(42)::test.call("-", plus, aster))

    assertEquals("42-+*", A::test.call(A("42"), "-", plus, aster))
    assertEquals("42-+*", A("42")::test.call("-", plus, aster))

    return "OK"
}
