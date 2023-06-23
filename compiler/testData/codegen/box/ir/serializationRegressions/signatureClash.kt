// https://youtrack.jetbrains.com/issue/KT-42020/Psi2ir-IllegalStateException-IrSimpleFunctionPublicSymbolImpl-for-public-...-is-already-bound-on-generic-function-whose
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_BACKEND_K1: NATIVE, JS_IR

// https://youtrack.jetbrains.com/issue/KT-59279/Psi2Ir-FIR2IR-Signature-clash-leads-to-wrong-method-resolve
// IGNORE_BACKEND: JS
// IGNORE_BACKEND_K2: NATIVE, JS_IR

// FIR status: validation failed. TODO decide if we want to fix KT-42020 for FIR as well
// IGNORE_BACKEND_K2: JVM_IR
// MODULE: lib
// FILE: lib.kt

open class Base<T> {
    open fun foo(p1: T): String { return "p1:$p1" }
    open fun foo(p2: String): String { return "p2:$p2" }
}
class Derived : Base<String>()



// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    val d = Derived()
    val foo42 = d.foo(p1 = "42")
    if (foo42 != "p1:42") return "FAIL1: foo42=$foo42"
    val foo24 = d.foo(p2 = "24")
    if (foo24 != "p2:24") return "FAIL2: foo24=$foo24"

    return "OK"
}