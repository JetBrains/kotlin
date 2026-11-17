// IGNORE_BACKEND: NATIVE, JS_IR, JS_IR_ES6, WASM_WASI, WASM_JS
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE, JS_IR
// ISSUE: KT-91509

// MODULE: lib
// FILE: lib.kt
open class Base<T>(val value: T) {
    fun foo(): T = value
}

// A chain of generic local classes capturing the enclosing inline function's TP
inline fun <R> test(r: R): R = object {
    fun run(): R {
        open class L1<T>(v: T) : Base<T>(v)
        class L2<T>(v: T) : L1<T>(v)
        return L2(r).foo()
    }
}.run()

// MODULE: main(lib)
// FILE: main.kt
fun box(): String = test("OK")

