// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: NULL_REF_CAST
// IGNORE_BACKEND_FIR: JVM_IR
fun String?.foo() = this ?: "OK"

fun foo(i: Int?): String {
    if (i == null) return i.foo()
    return "$i"
}

fun box() = foo(null)
