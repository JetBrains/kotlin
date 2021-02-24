// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: PROPERTY_REFERENCES
// !LANGUAGE: +InlineClasses
// WITH_RUNTIME
import kotlin.test.assertEquals

inline class Z(internal val x: Int)
inline class L(internal val x: Long)
inline class S(internal val x: String)

fun box(): String {
    assertEquals(42, Z::x.get(Z(42)))
    assertEquals(1234L, L::x.get(L(1234L)))
    assertEquals("abc", S::x.get(S("abc")))

    assertEquals(42, Z::x.invoke(Z(42)))
    assertEquals(1234L, L::x.invoke(L(1234L)))
    assertEquals("abc", S::x.invoke(S("abc")))

    return "OK"
}