// IGNORE_BACKEND: NATIVE, JS_IR, WASM
// IGNORE_BACKEND_FIR: JVM_IR
// MODULE: lib
// FILE: 1.kt
interface B<X>

object T : B<String>

fun B<*>.foo() {}

// MODULE: main(lib)
// FILE: 2.kt
interface B

fun box(): String {
    T.foo()
    return "OK"
}
