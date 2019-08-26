// IGNORE_BACKEND: WASM
val foo: ((String) -> String) = run {
    { it }
}

fun box() = foo("OK")