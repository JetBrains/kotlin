// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

var global = S("")

inline class S(val x: String?) {
    var nonNullTest: S
        get() = S("${global.x}$x")
        set(value) {
            global = S("${value.x}$x")
        }

    var nullableTest: S?
        get() = S("${global.x}$x")
        set(value) {
            global = S("${value!!.x}$x")
        }
}

inline class Z(val x: Int) {
    var nonNullTest: S
        get() = S("${global.x}$x")
        set(value) {
            global = S("${value.x}$x")
        }

    var nullableTest: S?
        get() = S("${global.x}$x")
        set(value) {
            global = S("${value!!.x}$x")
        }
}

inline class A(val x: Any) {
    var nonNullTest: S
        get() = S("${global.x}$x")
        set(value) {
            global = S("${value.x}$x")
        }

    var nullableTest: S?
        get() = S("${global.x}$x")
        set(value) {
            global = S("${value!!.x}$x")
        }
}

fun box(): String {
    global = S("")
    assertEquals(S("42"), S::nonNullTest.call(S("42")))
    assertEquals(S("42"), S("42")::nonNullTest.call())
    assertEquals(S("42"), S::nonNullTest.getter.call(S("42")))
    assertEquals(S("42"), S("42")::nonNullTest.getter.call())
    S::nonNullTest.setter.call(S("42"), S("S-"))
    assertEquals(S("S-42"), global)
    S("42")::nonNullTest.setter.call(S("S+"))
    assertEquals(S("S+42"), global)

    global = S("")
    assertEquals(S("42"), S::nullableTest.call(S("42")))
    assertEquals(S("42"), S("42")::nullableTest.call())
    assertEquals(S("42"), S::nullableTest.getter.call(S("42")))
    assertEquals(S("42"), S("42")::nullableTest.getter.call())
    S::nullableTest.setter.call(S("42"), S("S-"))
    assertEquals(S("S-42"), global)
    S("42")::nullableTest.setter.call(S("S+"))
    assertEquals(S("S+42"), global)

    global = S("")
    assertEquals(S("42"), Z::nonNullTest.call(Z(42)))
    assertEquals(S("42"), Z(42)::nonNullTest.call())
    assertEquals(S("42"), Z::nonNullTest.getter.call(Z(42)))
    assertEquals(S("42"), Z(42)::nonNullTest.getter.call())
    Z::nonNullTest.setter.call(Z(42), S("Z-"))
    assertEquals(S("Z-42"), global)
    Z(42)::nonNullTest.setter.call(S("Z+"))
    assertEquals(S("Z+42"), global)

    global = S("")
    assertEquals(S("42"), Z::nullableTest.call(Z(42)))
    assertEquals(S("42"), Z(42)::nullableTest.call())
    assertEquals(S("42"), Z::nullableTest.getter.call(Z(42)))
    assertEquals(S("42"), Z(42)::nullableTest.getter.call())
    Z::nullableTest.setter.call(Z(42), S("Z-"))
    assertEquals(S("Z-42"), global)
    Z(42)::nullableTest.setter.call(S("Z+"))
    assertEquals(S("Z+42"), global)

    global = S("")
    assertEquals(S("42"), A::nonNullTest.call(A(42)))
    assertEquals(S("42"), A(42)::nonNullTest.call())
    assertEquals(S("42"), A::nonNullTest.getter.call(A(42)))
    assertEquals(S("42"), A(42)::nonNullTest.getter.call())
    A::nonNullTest.setter.call(A(42), S("A-"))
    assertEquals(S("A-42"), global)
    A(42)::nonNullTest.setter.call(S("A+"))
    assertEquals(S("A+42"), global)

    global = S("")
    assertEquals(S("42"), A::nullableTest.call(A(42)))
    assertEquals(S("42"), A(42)::nullableTest.call())
    assertEquals(S("42"), A::nullableTest.getter.call(A(42)))
    assertEquals(S("42"), A(42)::nullableTest.getter.call())
    A::nullableTest.setter.call(A(42), S("A-"))
    assertEquals(S("A-42"), global)
    A(42)::nullableTest.setter.call(S("A+"))
    assertEquals(S("A+42"), global)

    return "OK"
}
