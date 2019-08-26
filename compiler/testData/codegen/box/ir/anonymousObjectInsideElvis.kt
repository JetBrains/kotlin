// IGNORE_BACKEND: WASM
fun box(): String {
    return (object { val r = "OK" } ?: null)!!.r
}
