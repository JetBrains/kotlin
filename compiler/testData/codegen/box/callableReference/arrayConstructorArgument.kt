// WITH_RUNTIME
// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: PROPERTY_REFERENCES
fun foo(it: Int) = "O"[it]

val Int.foo: Char
    get() = "K"[this]

fun box(): String =
    CharArray(1, ::foo)[0].toString() + CharArray(1, Int::foo)[0].toString()
