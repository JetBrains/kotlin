// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: UNSIGNED_ARRAYS
// WITH_RUNTIME

class C<T>(val x: T, vararg ys: UInt) {
    val y0 = ys[0]
}

fun box(): String {
    val c = C("a", 42u)
    if (c.y0 != 42u) throw AssertionError()

    return "OK"
}