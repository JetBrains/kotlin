// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
class My {
    var my: String = "U"
        get() = { field }()
        set(arg) {
            class Local {
                fun foo() {
                    field = arg + "K"
                }
            }
            Local().foo()
        }
}

fun box(): String {
    val m = My()
    m.my = "O"
    return m.my
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
