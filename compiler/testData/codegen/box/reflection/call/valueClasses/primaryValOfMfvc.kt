// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// LANGUAGE: +ValueClasses

import kotlin.test.assertEquals

@JvmInline
value class Z(val x1: UInt, val x2: Int) {
    var x3
        get() = x1
        set(value) = Unit
}
@JvmInline
value class Z2(val x1: Z, val x2: Z) {
    var x3
        get() = x1
        set(value) = Unit
}

@JvmInline
value class L(val x1: ULong, val x2: Long) {
    var x3
        get() = x1
        set(value) = Unit
}
@JvmInline
value class L2(val x1: L, val x2: L) {
    var x3
        get() = x1
        set(value) = Unit
}

@JvmInline
value class A1(val x1: Any?, val x2: Any?) {
    var x3
        get() = x1
        set(value) = Unit
}
@JvmInline
value class A1_2(val x1: A1, val x2: A1) {
    var x3
        get() = x1
        set(value) = Unit
}
@JvmInline
value class A2(val x1: Any, val x2: Any) {
    var x3
        get() = x1
        set(value) = Unit
}
@JvmInline
value class A2_2(val x1: A2, val x2: A2) {
    var x3
        get() = x1
        set(value) = Unit
}

fun box(): String {
    assertEquals(42U, Z::x1.call(Z(42U, 43)))
    assertEquals(43, Z::x2.call(Z(42U, 43)))
    assertEquals(42U, Z::x3.call(Z(42U, 43)))
    assertEquals(42U, Z::x3.getter.call(Z(42U, 43)))
    assertEquals(Unit, Z::x3.setter.call(Z(42U, 43), 42U))
    
    assertEquals(42U, Z(42U, 43)::x1.call())
    assertEquals(43, Z(42U, 43)::x2.call())
    assertEquals(42U, Z(42U, 43)::x3.call())
    assertEquals(42U, Z(42U, 43)::x3.getter.call())
    assertEquals(Unit, Z(42U, 43)::x3.setter.call(42U))

    assertEquals(1234UL, L::x1.call(L(1234UL, 5678L)))
    assertEquals(5678L, L::x2.call(L(1234UL, 5678L)))
    assertEquals(1234UL, L::x3.call(L(1234UL, 5678L)))
    assertEquals(1234UL, L::x3.getter.call(L(1234UL, 5678L)))
    assertEquals(Unit, L::x3.setter.call(L(1234UL, 5678L), 1234UL))
    
    assertEquals(1234UL, L(1234UL, 5678L)::x1.call())
    assertEquals(5678L, L(1234UL, 5678L)::x2.call())
    assertEquals(1234UL, L(1234UL, 5678L)::x3.call())
    assertEquals(1234UL, L(1234UL, 5678L)::x3.getter.call())
    assertEquals(Unit, L(1234UL, 5678L)::x3.setter.call(1234UL))

    assertEquals("abc", A1::x1.call(A1("abc", "def")))
    assertEquals("def", A1::x2.call(A1("abc", "def")))
    assertEquals("abc", A1::x3.call(A1("abc", "def")))
    assertEquals("abc", A1::x3.getter.call(A1("abc", "def")))
    assertEquals(Unit, A1::x3.setter.call(A1("abc", "def"), "abc"))
    
    assertEquals("abc", A1("abc", "def")::x1.call())
    assertEquals("def", A1("abc", "def")::x2.call())
    assertEquals("abc", A1("abc", "def")::x3.call())
    assertEquals("abc", A1("abc", "def")::x3.getter.call())
    assertEquals(Unit, A1("abc", "def")::x3.setter.call("abc"))
    
    assertEquals(null, A1::x1.call(A1(null, null)))
    assertEquals(null, A1::x2.call(A1(null, null)))
    assertEquals(null, A1::x3.call(A1(null, null)))
    assertEquals(null, A1::x3.getter.call(A1(null, null)))
    assertEquals(Unit, A1::x3.setter.call(A1(null, null), null))
    
    assertEquals(null, A1(null, null)::x1.call())
    assertEquals(null, A1(null, null)::x2.call())
    assertEquals(null, A1(null, null)::x3.call())
    assertEquals(null, A1(null, null)::x3.getter.call())
    assertEquals(Unit, A1(null, null)::x3.setter.call(null))
    
    assertEquals("abc", A2::x1.call(A2("abc", "def")))
    assertEquals("def", A2::x2.call(A2("abc", "def")))
    assertEquals("abc", A2::x3.call(A2("abc", "def")))
    assertEquals("abc", A2::x3.getter.call(A2("abc", "def")))
    assertEquals(Unit, A2::x3.setter.call(A2("abc", "def"), "abc"))
    
    assertEquals("abc", A2("abc", "def")::x1.call())
    assertEquals("def", A2("abc", "def")::x2.call())
    assertEquals("abc", A2("abc", "def")::x3.call())
    assertEquals("abc", A2("abc", "def")::x3.getter.call())
    assertEquals(Unit, A2("abc", "def")::x3.setter.call("abc"))

    assertEquals(Z(42U, 43), Z2::x1.call(Z2(Z(42U, 43), Z(44U, 45))))
    assertEquals(Z(44U, 45), Z2::x2.call(Z2(Z(42U, 43), Z(44U, 45))))
    assertEquals(Z(42U, 43), Z2::x3.call(Z2(Z(42U, 43), Z(44U, 45))))
    assertEquals(Z(42U, 43), Z2::x3.getter.call(Z2(Z(42U, 43), Z(44U, 45))))
    assertEquals(Unit, Z2::x3.setter.call(Z2(Z(42U, 43), Z(44U, 45)), Z(42U, 43)))
    
    assertEquals(Z(42U, 43), Z2(Z(42U, 43), Z(44U, 45))::x1.call())
    assertEquals(Z(44U, 45), Z2(Z(42U, 43), Z(44U, 45))::x2.call())
    assertEquals(Z(42U, 43), Z2(Z(42U, 43), Z(44U, 45))::x3.call())
    assertEquals(Z(42U, 43), Z2(Z(42U, 43), Z(44U, 45))::x3.getter.call())
    assertEquals(Unit, Z2(Z(42U, 43), Z(44U, 45))::x3.setter.call(Z(42U, 43)))

    assertEquals(L(1234UL, 5678L), L2::x1.call(L2(L(1234UL, 5678L), L(12340UL, -5678L))))
    assertEquals(L(12340UL, -5678L), L2::x2.call(L2(L(1234UL, 5678L), L(12340UL, -5678L))))
    assertEquals(L(1234UL, 5678L), L2::x3.call(L2(L(1234UL, 5678L), L(12340UL, -5678L))))
    assertEquals(L(1234UL, 5678L), L2::x3.getter.call(L2(L(1234UL, 5678L), L(12340UL, -5678L))))
    assertEquals(Unit, L2::x3.setter.call(L2(L(1234UL, 5678L), L(12340UL, -5678L)), L(1234UL, 5678L)))
    
    assertEquals(L(1234UL, 5678L), L2(L(1234UL, 5678L), L(12340UL, -5678L))::x1.call())
    assertEquals(L(12340UL, -5678L), L2(L(1234UL, 5678L), L(12340UL, -5678L))::x2.call())
    assertEquals(L(1234UL, 5678L), L2(L(1234UL, 5678L), L(12340UL, -5678L))::x3.call())
    assertEquals(L(1234UL, 5678L), L2(L(1234UL, 5678L), L(12340UL, -5678L))::x3.getter.call())
    assertEquals(Unit, L2(L(1234UL, 5678L), L(12340UL, -5678L))::x3.setter.call(L(1234UL, 5678L)))

    assertEquals(A1("abc", "def"), A1_2::x1.call(A1_2(A1("abc", "def"), A1("geh", "ijk"))))
    assertEquals(A1("geh", "ijk"), A1_2::x2.call(A1_2(A1("abc", "def"), A1("geh", "ijk"))))
    assertEquals(A1("abc", "def"), A1_2::x3.call(A1_2(A1("abc", "def"), A1("geh", "ijk"))))
    assertEquals(A1("abc", "def"), A1_2::x3.getter.call(A1_2(A1("abc", "def"), A1("geh", "ijk"))))
    assertEquals(Unit, A1_2::x3.setter.call(A1_2(A1("abc", "def"), A1("geh", "ijk")), A1("abc", "def")))
    
    assertEquals(A1("abc", "def"), A1_2(A1("abc", "def"), A1("geh", "ijk"))::x1.call())
    assertEquals(A1("geh", "ijk"), A1_2(A1("abc", "def"), A1("geh", "ijk"))::x2.call())
    assertEquals(A1("abc", "def"), A1_2(A1("abc", "def"), A1("geh", "ijk"))::x3.call())
    assertEquals(A1("abc", "def"), A1_2(A1("abc", "def"), A1("geh", "ijk"))::x3.getter.call())
    assertEquals(Unit, A1_2(A1("abc", "def"), A1("geh", "ijk"))::x3.setter.call(A1("abc", "def")))
    
    assertEquals(A1(null, null), A1_2::x1.call(A1_2(A1(null, null), A1(null, null))))
    assertEquals(A1(null, null), A1_2::x2.call(A1_2(A1(null, null), A1(null, null))))
    assertEquals(A1(null, null), A1_2::x3.call(A1_2(A1(null, null), A1(null, null))))
    assertEquals(A1(null, null), A1_2::x3.getter.call(A1_2(A1(null, null), A1(null, null))))
    assertEquals(Unit, A1_2::x3.setter.call(A1_2(A1(null, null), A1(null, null)), A1(null, null)))
    
    assertEquals(A1(null, null), A1_2(A1(null, null), A1(null, null))::x1.call())
    assertEquals(A1(null, null), A1_2(A1(null, null), A1(null, null))::x2.call())
    assertEquals(A1(null, null), A1_2(A1(null, null), A1(null, null))::x3.call())
    assertEquals(A1(null, null), A1_2(A1(null, null), A1(null, null))::x3.getter.call())
    assertEquals(Unit, A1_2(A1(null, null), A1(null, null))::x3.setter.call(A1(null, null)))
    
    assertEquals(A2("abc", "def"), A2_2::x1.call(A2_2(A2("abc", "def"), A2("geh", "ijk"))))
    assertEquals(A2("geh", "ijk"), A2_2::x2.call(A2_2(A2("abc", "def"), A2("geh", "ijk"))))
    assertEquals(A2("abc", "def"), A2_2::x3.call(A2_2(A2("abc", "def"), A2("geh", "ijk"))))
    assertEquals(A2("abc", "def"), A2_2::x3.getter.call(A2_2(A2("abc", "def"), A2("geh", "ijk"))))
    assertEquals(Unit, A2_2::x3.setter.call(A2_2(A2("abc", "def"), A2("geh", "ijk")), A2("abc", "def")))
    
    assertEquals(A2("abc", "def"), A2_2(A2("abc", "def"), A2("geh", "ijk"))::x1.call())
    assertEquals(A2("geh", "ijk"), A2_2(A2("abc", "def"), A2("geh", "ijk"))::x2.call())
    assertEquals(A2("abc", "def"), A2_2(A2("abc", "def"), A2("geh", "ijk"))::x3.call())
    assertEquals(A2("abc", "def"), A2_2(A2("abc", "def"), A2("geh", "ijk"))::x3.getter.call())
    assertEquals(Unit, A2_2(A2("abc", "def"), A2("geh", "ijk"))::x3.setter.call(A2("abc", "def")))

    return "OK"
}
