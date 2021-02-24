// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: PROPERTY_REFERENCES
// !LANGUAGE: +InlineClasses
// WITH_RUNTIME
import kotlin.test.assertEquals

inline class Z(private val x: Int) {
    companion object {
        val xref = Z::x
    }
}

inline class L(private val x: Long) {
    companion object {
        val xref = L::x
    }
}

inline class S(private val x: String) {
    companion object {
        val xref = S::x
    }
}

fun box(): String {
    assertEquals(42, Z.xref.get(Z(42)))
    assertEquals(1234L, L.xref.get(L(1234L)))
    assertEquals("abc", S.xref.get(S("abc")))

    assertEquals(42, Z.xref.invoke(Z(42)))
    assertEquals(1234L, L.xref.invoke(L(1234L)))
    assertEquals("abc", S.xref.invoke(S("abc")))

    return "OK"
}