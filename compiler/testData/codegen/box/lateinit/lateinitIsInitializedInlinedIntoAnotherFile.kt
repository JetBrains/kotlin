// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS,Wasm-JS:2.4

// KT-89375
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
