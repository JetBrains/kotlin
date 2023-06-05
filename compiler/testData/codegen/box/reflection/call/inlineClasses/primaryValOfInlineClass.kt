// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

inline class Z(val x: Int)
inline class Z2(val x: Z)

inline class L(val x: Long)
inline class L2(val x: L)

inline class A1(val x: Any?)
inline class A1_2(val x: A1)

inline class A2(val x: Any)
inline class A2_2(val x: A2)

fun box(): String {
    assertEquals(42, Z::x.call(Z(42)))
    assertEquals(42, Z(42)::x.call())

    assertEquals(1234L, L::x.call(L(1234L)))
    assertEquals(1234L, L(1234L)::x.call())

    assertEquals("abc", A1::x.call(A1("abc")))
    assertEquals("abc", A1("abc")::x.call())
    assertEquals(null, A1::x.call(A1(null)))
    assertEquals(null, A1(null)::x.call())

    assertEquals(Z(42), Z2::x.call(Z2(Z(42))))
    assertEquals(Z(42), Z2(Z(42))::x.call())

    assertEquals(L(1234L), L2::x.call(L2(L(1234L))))
    assertEquals(L(1234L), L2(L(1234L))::x.call())

    assertEquals(A1("abc"), A1_2::x.call(A1_2(A1("abc"))))
    assertEquals(A1("abc"), A1_2(A1("abc"))::x.call())
    assertEquals(A1(null), A1_2::x.call(A1_2(A1(null))))
    assertEquals(A1(null), A1_2(A1(null))::x.call())
    assertEquals(A2("abc"), A2_2::x.call(A2_2(A2("abc"))))
    assertEquals(A2("abc"), A2_2(A2("abc"))::x.call())

    return "OK"
}
