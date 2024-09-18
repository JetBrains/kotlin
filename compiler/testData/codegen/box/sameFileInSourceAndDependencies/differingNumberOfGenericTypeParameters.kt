// IGNORE_BACKEND: NATIVE, JS, JS_ES6, WASM
// IGNORE_BACKEND_K2: JVM_IR
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
