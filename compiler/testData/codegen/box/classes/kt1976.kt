// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
class A {
    public val f : ()->String = {"OK"}
}

fun box(): String {
    val a = A()
    return a.f() // does not work: (in runtime) ClassCastException: A cannot be cast to kotlin.Function0
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
