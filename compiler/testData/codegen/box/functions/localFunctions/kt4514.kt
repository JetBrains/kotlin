// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    fun String.f() = this
    val vf: String.() -> String = { this }

    val localExt = "O".f() + "K"?.f()
    if (localExt != "OK") return "localExt $localExt"

    return "O".vf() + "K"?.vf()
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
