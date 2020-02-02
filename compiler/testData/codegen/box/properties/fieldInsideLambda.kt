// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
val my: String = "O"
    get() = { field }() + "K"

fun box() = my
