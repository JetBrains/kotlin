// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
open class SomeClass(val some: Double, val other: Int, vararg val args: String) {
    fun result() = args[1]
}

fun box(): String {
    return object : SomeClass(3.14, 42, "No", "OK", "Yes") {
    }.result()
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: WASM_ARRAYS_UNSUPPORTED
