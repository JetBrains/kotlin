// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
class My {
    companion object {
        private val my: String = "O"
            get() = { field }() + "K"

        fun getValue() = my
    }
}

fun box() = My.getValue()
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
