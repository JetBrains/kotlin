// IGNORE_BACKEND_FIR: JVM_IR

var result = ""

class C(val token: String) {
    fun target(): Int {
        result += token
        return 42
    }
}

fun adapt(f: () -> Unit): Unit = f()

fun overload() {
    adapt(C("O")::target)
}

fun overload(unused: String) {
    adapt(C("K")::target)
}

fun box(): String {
    overload()
    overload("")
    return result
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: BINDING_RECEIVERS