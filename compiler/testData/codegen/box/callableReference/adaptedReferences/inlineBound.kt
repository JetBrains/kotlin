// IGNORE_BACKEND_FIR: JVM_IR

inline fun foo(x: () -> Unit): String {
    x()
    return "OK"
}

fun String.id(s: String = this, vararg xs: Int): String = s

fun box(): String = foo("Fail"::id)

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: BINDING_RECEIVERS
