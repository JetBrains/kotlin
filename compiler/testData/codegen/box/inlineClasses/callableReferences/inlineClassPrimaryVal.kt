// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: PROPERTY_REFERENCES
// !LANGUAGE: +InlineClasses
// WITH_RUNTIME

inline class Z(val x: Int)
inline class L(val x: Long)
inline class S(val x: String)

fun box(): String {
    if ((Z::x).get(Z(42)) != 42) throw AssertionError()
    if ((L::x).get(L(1234L)) != 1234L) throw AssertionError()
    if ((S::x).get(S("abcdef")) != "abcdef") throw AssertionError()

    return "OK"
}