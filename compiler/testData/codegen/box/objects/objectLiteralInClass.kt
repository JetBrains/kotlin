// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR

class C {

    val s = "OK"

    private val localObject = object {
        fun getS(): String {
            return s
        }
    }

    fun ok(): String =
        33.let { localObject.getS() }
}

fun box() = C().ok()

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ let 
