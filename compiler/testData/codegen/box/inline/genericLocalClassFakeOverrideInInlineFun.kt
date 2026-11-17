// IGNORE_BACKEND: JS_IR, JS_IR_ES6, NATIVE, WASM_JS, WASM_WASI
// MODULE: lib
// FILE: lib.kt
open class Base<T>(val value: T) {
    fun foo(): T = value
    val bar: T get() = value
}

// Local classes are not allowed directly in inline functions, but are allowed inside objects declared in them.
inline fun test1(): String = object {
    fun run(): String {
        class Local<T>(value: T) : Base<T>(value)
        return Local("O").foo() + Local("K").bar
    }
}.run()

// Local class TPs mapped to the supertype in a non-trivial way
inline fun test2(): String = object {
    fun run(): String {
        class Local<A, B>(val a: A, b: B) : Base<B>(b)
        return Local(1, "O").foo() + Local(2, "K").bar
    }
}.run()

// Local class TP combined with the enclosing inline function's TP
inline fun <R> test3(r: R): R = object {
    fun run(): R {
        class Local<T>(value: T) : Base<T>(value)
        return Local(r).foo()
    }
}.run()

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    if (test1() != "OK") return "FAIL1: ${test1()}"
    if (test2() != "OK") return "FAIL2: ${test2()}"
    if (test3("OK") != "OK") return "FAIL3"
    return "OK"
}
