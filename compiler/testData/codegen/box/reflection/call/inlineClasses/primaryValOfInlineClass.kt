// IGNORE_BACKEND: JS_IR, JS, NATIVE, WASM
// IGNORE_BACKEND: JS_IR_ES6
// WITH_REFLECT

import kotlin.test.assertEquals

inline class Z(val x: Int)
inline class Z2(val x: Z)

inline class L(val x: Long)
inline class L2(val x: L)

inline class A(val x: Any?)
inline class A2(val x: A)

fun box(): String {
    assertEquals(42, Z::x.call(Z(42)))
    assertEquals(42, Z(42)::x.call())

    assertEquals(1234L, L::x.call(L(1234L)))
    assertEquals(1234L, L(1234L)::x.call())

    assertEquals("abc", A::x.call(A("abc")))
    assertEquals("abc", A("abc")::x.call())
    assertEquals(null, A::x.call(A(null)))
    assertEquals(null, A(null)::x.call())

    assertEquals(Z(42), Z2::x.call(Z2(Z(42))))
    assertEquals(Z(42), Z2(Z(42))::x.call())

    assertEquals(L(1234L), L2::x.call(L2(L(1234L))))
    assertEquals(L(1234L), L2(L(1234L))::x.call())

    assertEquals(A("abc"), A2::x.call(A2(A("abc"))))
    assertEquals(A("abc"), A2(A("abc"))::x.call())
    assertEquals(A(null), A2::x.call(A2(A(null))))
    assertEquals(A(null), A2(A(null))::x.call())

    return "OK"
}