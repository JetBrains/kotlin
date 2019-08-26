// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
package p

private class C(val y: Int) {
    val initChild = { ->
        object {
            override fun toString(): String {
                return "child" + y
            }
        }
    }
}

fun box(): String {
    val c = C(3).initChild
    val x = c().toString()
    return if (x == "child3") "OK" else x
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
