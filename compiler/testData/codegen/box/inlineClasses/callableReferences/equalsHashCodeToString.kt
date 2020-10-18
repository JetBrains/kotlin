// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6, NATIVE
// WITH_REFLECT
import kotlin.test.*

inline class Z(val s: String)

fun box(): String {
    val a = Z("a")
    val b = Z("b")

    val equals = Z::equals
    assertTrue(equals.call(a, a))
    assertFalse(equals.call(a, b))

    val hashCode = Z::hashCode
    assertEquals(a.s.hashCode(), hashCode.call(a))

    val toString = Z::toString
    assertEquals("Z(s=${a.s})", toString.call(a))

    return "OK"
}