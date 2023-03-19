// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// LANGUAGE: +ValueClasses

import kotlin.test.assertEquals

@JvmInline
value class Z(internal val x1: UInt, internal val x2: Int)
@JvmInline
value class Z2(internal val x1: Z, internal val x2: Z)

@JvmInline
value class L(internal val x1: ULong, internal val x2: Long)
@JvmInline
value class L2(internal val x1: L, internal val x2: L)

@JvmInline
value class A1(internal val x1: Any?, internal val x2: Any?)
@JvmInline
value class A1_2(internal val x1: A1, internal val x2: A1)
@JvmInline
value class A2(internal val x1: Any, internal val x2: Any)
@JvmInline
value class A2_2(internal val x1: A2, internal val x2: A2)

fun box(): String {
    assertEquals(42U, Z::x1.call(Z(42U, 43)))
    assertEquals(43, Z::x2.call(Z(42U, 43)))
    assertEquals(42U, Z(42U, 43)::x1.call())
    assertEquals(43, Z(42U, 43)::x2.call())

    assertEquals(1234UL, L::x1.call(L(1234UL, 5678L)))
    assertEquals(5678L, L::x2.call(L(1234UL, 5678L)))
    assertEquals(1234UL, L(1234UL, 5678L)::x1.call())
    assertEquals(5678L, L(1234UL, 5678L)::x2.call())

    assertEquals("abc", A1::x1.call(A1("abc", "def")))
    assertEquals("def", A1::x2.call(A1("abc", "def")))
    assertEquals("abc", A1("abc", "def")::x1.call())
    assertEquals("def", A1("abc", "def")::x2.call())
    assertEquals(null, A1::x1.call(A1(null, null)))
    assertEquals(null, A1::x2.call(A1(null, null)))
    assertEquals(null, A1(null, null)::x1.call())
    assertEquals(null, A1(null, null)::x2.call())
    assertEquals("abc", A2::x1.call(A2("abc", "def")))
    assertEquals("def", A2::x2.call(A2("abc", "def")))
    assertEquals("abc", A2("abc", "def")::x1.call())
    assertEquals("def", A2("abc", "def")::x2.call())

    assertEquals(Z(42U, 43), Z2::x1.call(Z2(Z(42U, 43), Z(44U, 45))))
    assertEquals(Z(44U, 45), Z2::x2.call(Z2(Z(42U, 43), Z(44U, 45))))
    assertEquals(Z(42U, 43), Z2(Z(42U, 43), Z(44U, 45))::x1.call())
    assertEquals(Z(44U, 45), Z2(Z(42U, 43), Z(44U, 45))::x2.call())

    assertEquals(L(1234UL, 5678L), L2::x1.call(L2(L(1234UL, 5678L), L(12340UL, -5678L))))
    assertEquals(L(12340UL, -5678L), L2::x2.call(L2(L(1234UL, 5678L), L(12340UL, -5678L))))
    assertEquals(L(1234UL, 5678L), L2(L(1234UL, 5678L), L(12340UL, -5678L))::x1.call())
    assertEquals(L(12340UL, -5678L), L2(L(1234UL, 5678L), L(12340UL, -5678L))::x2.call())

    assertEquals(A1("abc", "def"), A1_2::x1.call(A1_2(A1("abc", "def"), A1("geh", "ijk"))))
    assertEquals(A1("geh", "ijk"), A1_2::x2.call(A1_2(A1("abc", "def"), A1("geh", "ijk"))))
    assertEquals(A1("abc", "def"), A1_2(A1("abc", "def"), A1("geh", "ijk"))::x1.call())
    assertEquals(A1("geh", "ijk"), A1_2(A1("abc", "def"), A1("geh", "ijk"))::x2.call())
    assertEquals(A1(null, null), A1_2::x1.call(A1_2(A1(null, null), A1(null, null))))
    assertEquals(A1(null, null), A1_2::x2.call(A1_2(A1(null, null), A1(null, null))))
    assertEquals(A1(null, null), A1_2(A1(null, null), A1(null, null))::x1.call())
    assertEquals(A1(null, null), A1_2(A1(null, null), A1(null, null))::x2.call())
    assertEquals(A2("abc", "def"), A2_2::x1.call(A2_2(A2("abc", "def"), A2("geh", "ijk"))))
    assertEquals(A2("geh", "ijk"), A2_2::x2.call(A2_2(A2("abc", "def"), A2("geh", "ijk"))))
    assertEquals(A2("abc", "def"), A2_2(A2("abc", "def"), A2("geh", "ijk"))::x1.call())
    assertEquals(A2("geh", "ijk"), A2_2(A2("abc", "def"), A2("geh", "ijk"))::x2.call())

    return "OK"
}
