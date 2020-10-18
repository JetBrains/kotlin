// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: PROPERTY_REFERENCES
// !LANGUAGE: +InlineClasses
// WITH_RUNTIME
inline class Z(val x: Int)

class C(var z: Z)

fun box(): String {
    val x = C(Z(42))

    val ref = x::z

    if (ref.get().x != 42) throw AssertionError()

    ref.set(Z(1234))
    if (ref.get().x != 1234) throw AssertionError()

    return "OK"
}