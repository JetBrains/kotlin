// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

inline class Z(val x: Int) {
    fun test(a: Int, b: Z, c: Z?) = "$x$a${b.x}${c!!.x}"
}

inline class S(val x: String) {
    fun test(a: Int, b: Z, c: Z?) = "$x$a${b.x}${c!!.x}"
}

inline class A(val x: Any) {
    fun test(a: Int, b: Z, c: Z?) = "$x$a${b.x}${c!!.x}"
}

fun box(): String {
    val two = Z(2)
    val four = Z(4)

    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals("0124", Z::test.call(Z(0), 1, two, four))
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals("0124", Z(0)::test.call(1, two, four))
    }

    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals("0124", S::test.call(S("0"), 1, two, four))
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals("0124", S("0")::test.call(1, two, four))
    }

    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals("0124", A::test.call(A(0), 1, two, four))
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals("0124", A(0)::test.call(1, two, four))
    }

    return "OK"
}
