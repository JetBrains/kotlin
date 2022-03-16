// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

inline class S(val value: String?) {
    operator fun plus(other: S): S = S(this.value + other.value)
}

class C {
    fun member(x: S, y: String, z: S?): S = x + S(y) + z!!
}

fun topLevel(x: String, y: S, z: S?): S = S(x) + y + z!!

fun S.extension1(y: S, z: S?): S = this + y + z!!

fun S?.extension2(y: S, z: S?) = this!! + y + z!!

fun S.extension3(): String = value!!

fun S?.extension4(): String = this!!.value!!

fun box(): String {
    assertEquals(S("abc"), C::member.call(C(), S("a"), "b", S("c")))
    assertEquals(S("def"), ::topLevel.call("d", S("e"), S("f")))
    assertEquals(S("ghi"), S::extension1.call(S("g"), S("h"), S("i")))
    assertEquals(S("jkl"), S::extension2.call(S("j"), S("k"), S("l")))
    assertEquals("_", S::extension3.call(S("_")))
    assertEquals("_", S?::extension4.call(S("_")))

    assertEquals(S("mno"), C()::member.call(S("m"), "n", S("o")))
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(S("pqr"), S("p")::extension1.call(S("q"), "r"))
    }
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals(S("stu"), S("s")::extension2.call(S("t"), "u"))
    }
    assertEquals("_", S("_")::extension3.call())
    assertFailsWith<IllegalArgumentException>("Remove assertFailsWith and try again, as this problem may have been fixed.") {
        assertEquals("_", S("_")::extension4.call(S("_")))
    }

    return "OK"
}
