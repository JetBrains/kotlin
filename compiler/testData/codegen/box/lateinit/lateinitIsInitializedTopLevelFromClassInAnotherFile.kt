// KT-89375
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI
// ^^^ KT-91246: JS and Wasm initialize the file of a top-level lateinit only from its own top-level declarations
// Like lateinitIsInitializedTopLevelFromClass.kt, but the check is triggered from another file.

// FILE: lib.kt
private lateinit var late: String
private val init = initLate()

private fun initLate() {
    late = "late"
}

class TopLevelLateinitObserver {
    fun check() = ::late.isInitialized
}

// FILE: main.kt
fun box(): String {
    if (!TopLevelLateinitObserver().check()) return "FAIL"
    return "OK"
}
