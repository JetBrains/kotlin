// IGNORE_BACKEND: JS_IR, JS, NATIVE
// IGNORE_BACKEND: JS_IR_ES6
// WITH_REFLECT
import kotlin.test.assertEquals

var global = S("")

inline class Z(val x: Int) {
    var test: S
        get() = S("${global.x}$x")
        set(value) {
            global = S("${value.x}$x")
        }
}

inline class L(val x: Long) {
    var test: S
        get() = S("${global.x}$x")
        set(value) {
            global = S("${value.x}$x")
        }
}

inline class S(val x: String) {
    var test: S
        get() = S("${global.x}$x")
        set(value) {
            global = S("${value.x}$x")
        }
}

inline class A(val x: Any) {
    var test: S
        get() = S("${global.x}$x")
        set(value) {
            global = S("${value.x}$x")
        }
}

fun box(): String {
    global = S("")
    assertEquals(S("42"), Z::test.call(Z(42)))
    assertEquals(S("42"), Z(42)::test.call())
    assertEquals(S("42"), Z::test.getter.call(Z(42)))
    assertEquals(S("42"), Z(42)::test.getter.call())
    Z::test.setter.call(Z(42), S("Z-"))
    assertEquals(S("Z-42"), global)
    Z(42)::test.setter.call(S("Z+"))
    assertEquals(S("Z+42"), global)

    global = S("")
    assertEquals(S("42"), L::test.call(L(42L)))
    assertEquals(S("42"), L(42L)::test.call())
    assertEquals(S("42"), L::test.getter.call(L(42L)))
    assertEquals(S("42"), L(42L)::test.getter.call())
    L::test.setter.call(L(42L), S("L-"))
    assertEquals(S("L-42"), global)
    L(42L)::test.setter.call(S("L+"))
    assertEquals(S("L+42"), global)

    global = S("")
    assertEquals(S("42"), S::test.call(S("42")))
    assertEquals(S("42"), S("42")::test.call())
    assertEquals(S("42"), S::test.getter.call(S("42")))
    assertEquals(S("42"), S("42")::test.getter.call())
    S::test.setter.call(S("42"), S("S-"))
    assertEquals(S("S-42"), global)
    S("42")::test.setter.call(S("S+"))
    assertEquals(S("S+42"), global)

    global = S("")
    assertEquals(S("42"), A::test.call(A(42)))
    assertEquals(S("42"), A(42)::test.call())
    assertEquals(S("42"), A::test.getter.call(A(42)))
    assertEquals(S("42"), A(42)::test.getter.call())
    A::test.setter.call(A(42), S("A-"))
    assertEquals(S("A-42"), global)
    A(42)::test.setter.call(S("A+"))
    assertEquals(S("A+42"), global)

    return "OK"
}
