// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTION_INHERITANCE
// KJS_WITH_FULL_RUNTIME
// DONT_TARGET_EXACT_BACKEND: NATIVE

class A : ArrayList<String>() {
    override val size: Int get() = super.size + 56
}

fun box(): String {
    val a = A()
    if (a.size != 56) return "fail: ${a.size}"

    return "OK"
}
