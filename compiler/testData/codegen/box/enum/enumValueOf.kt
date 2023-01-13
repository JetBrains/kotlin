// KT-55828
// IGNORE_BACKEND_K2: NATIVE
// IGNORE_BACKEND: WASM
enum class E { OK }

fun <T> id(x: T) = x

fun box() = enumValueOf<E>(id("OK")).name
