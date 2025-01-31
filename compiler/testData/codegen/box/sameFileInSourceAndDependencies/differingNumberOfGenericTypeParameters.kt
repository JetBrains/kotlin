// IGNORE_BACKEND: NATIVE, JS_IR, JS_IR_ES6, WASM
// IGNORE_BACKEND_K2: JVM_IR
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR, NATIVE
// ^^^ UNRESOLVED_REFERENCE_WRONG_RECEIVER: Unresolved reference. None of the following candidates is applicable because of a receiver type mismatch:
//     fun B<*>.foo(): Unit at 2.kt:(86,89)
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
