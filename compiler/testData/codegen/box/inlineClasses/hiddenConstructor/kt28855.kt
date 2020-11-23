// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNSIGNED_ARRAYS
// !LANGUAGE: +InlineClasses
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND_FIR: JVM_IR

class C<T>(val x: T, vararg ys: UInt) {
    val y0 = ys[0]
}

fun box(): String {
    val c = C("a", 42u)
    if (c.y0 != 42u) throw AssertionError()

    return "OK"
}