// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
val foo: ((String) -> String) = run {
    { it }
}

fun box() = foo("OK")