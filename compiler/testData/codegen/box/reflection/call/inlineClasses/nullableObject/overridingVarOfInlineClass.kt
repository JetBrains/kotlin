// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

var global = S("")

interface ITest {
    var nonNullTest: S
    var nullableTest: S?
}

inline class S(val x: String?) : ITest {
    override var nonNullTest: S
        get() = S("${global.x}$x")
        set(value) {
            global = S("${value.x}$x")
        }

    override var nullableTest: S?
        get() = S("${global.x}$x")
        set(value) {
            global = S("${value!!.x}$x")
        }
}

inline class Z(val x: Int) : ITest {
    override var nonNullTest: S
        get() = S("${global.x}$x")
        set(value) {
            global = S("${value.x}$x")
        }

    override var nullableTest: S?
        get() = S("${global.x}$x")
        set(value) {
            global = S("${value!!.x}$x")
        }
}

inline class A(val x: Any) : ITest {
    override var nonNullTest: S
        get() = S("${global.x}$x")
        set(value) {
            global = S("${value.x}$x")
        }

    override var nullableTest: S?
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
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(S("42"), S::nullableTest.call(S("42")))
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(S("42"), S("42")::nullableTest.call())
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(S("42"), S::nullableTest.getter.call(S("42")))
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(S("42"), S("42")::nullableTest.getter.call())
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        S::nullableTest.setter.call(S("42"), S("S-"))
        assertEquals(S("S-42"), global)
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        S("42")::nullableTest.setter.call(S("S+"))
        assertEquals(S("S+42"), global)
    }

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
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(S("42"), Z::nullableTest.call(Z(42)))
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(S("42"), Z(42)::nullableTest.call())
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(S("42"), Z::nullableTest.getter.call(Z(42)))
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(S("42"), Z(42)::nullableTest.getter.call())
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        Z::nullableTest.setter.call(Z(42), S("Z-"))
        assertEquals(S("Z-42"), global)
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        Z(42)::nullableTest.setter.call(S("Z+"))
        assertEquals(S("Z+42"), global)
    }

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
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(S("42"), A::nullableTest.call(A(42)))
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(S("42"), A(42)::nullableTest.call())
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(S("42"), A::nullableTest.getter.call(A(42)))
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(S("42"), A(42)::nullableTest.getter.call())
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        A::nullableTest.setter.call(A(42), S("A-"))
        assertEquals(S("A-42"), global)
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        A(42)::nullableTest.setter.call(S("A+"))
        assertEquals(S("A+42"), global)
    }

    return "OK"
}
