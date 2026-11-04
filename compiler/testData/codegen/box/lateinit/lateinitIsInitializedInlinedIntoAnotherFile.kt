// KT-89375
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI
// ^^^ KT-91246: JS and Wasm initialize the file of a top-level lateinit only from its own top-level declarations
// The read is inlined into another file, where nothing initializes the declaring file.

// FILE: lib.kt
lateinit var late: String
private val init = initLate()

private fun initLate() {
    late = "late"
}

inline fun probe(): Boolean {
    val o = object {
        val z = ::late.isInitialized
    }
    return o.z
}

// FILE: main.kt
fun box(): String {
    if (!probe()) return "FAIL"
    return "OK"
}
