// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

inline class S(val x: String?) {
    fun test(a: String, b: S, c: S?) = "$x$a${b.x}${c!!.x}"
}

inline class Z(val x: Int) {
    fun test(a: String, b: S, c: S?) = "$x$a${b.x}${c!!.x}"
}

inline class A(val x: Any) {
    fun test(a: String, b: S, c: S?) = "$x$a${b.x}${c!!.x}"
}

fun box(): String {
    val plus = S("+")
    val aster = S("*")

    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals("42-+*", S::test.call(S("42"), "-", plus, aster))
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals("42-+*", S("42")::test.call("-", plus, aster))
    }

    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals("42-+*", Z::test.call(Z(42), "-", plus, aster))
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals("42-+*", Z(42)::test.call("-", plus, aster))
    }

    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals("42-+*", A::test.call(A("42"), "-", plus, aster))
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals("42-+*", A("42")::test.call("-", plus, aster))
    }

    return "OK"
}
