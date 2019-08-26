// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

object ExtProvider {
    operator fun Long.get(i: Int) = this
    operator fun Long.set(i: Int, newValue: Long) {}
}

fun box(): String {
    with (ExtProvider) {
        var x = 0L
        val y = x[0]++
        return if (y == 0L) "OK" else "Failed, y=$y"
    }
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ with 
