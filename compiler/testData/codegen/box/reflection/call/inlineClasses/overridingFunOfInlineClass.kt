// IGNORE_BACKEND: JS_IR, JS, NATIVE, JVM_IR
// WITH_REFLECT
import kotlin.test.assertEquals

interface ITest {
    fun test(a: String, b: S): String
}

inline class Z(val x: Int) : ITest {
    override fun test(a: String, b: S) = "$x$a${b.x}"
}

inline class L(val x: Long) : ITest {
    override fun test(a: String, b: S) = "$x$a${b.x}"
}

inline class S(val x: String) : ITest {
    override fun test(a: String, b: S) = "$x$a${b.x}"
}

inline class A(val x: Any) : ITest {
    override fun test(a: String, b: S) = "$x$a${b.x}"
}

fun box(): String {
    assertEquals("42-+", Z::test.call(Z(42), "-", S("+")))
    assertEquals("42-+", Z(42)::test.call("-", S("+")))

    assertEquals("42-+", L::test.call(L(42L), "-", S("+")))
    assertEquals("42-+", L(42L)::test.call("-", S("+")))

    assertEquals("42-+", S::test.call(S("42"), "-", S("+")))
    assertEquals("42-+", S("42")::test.call("-", S("+")))

    assertEquals("42-+", A::test.call(A("42"), "-", S("+")))
    assertEquals("42-+", A("42")::test.call("-", S("+")))

    return "OK"
}
