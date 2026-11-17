// IGNORE_BACKEND: JS_IR_ES6
// ISSUE: KT-91509

// MODULE: lib
// FILE: lib.kt
open class Base<T>(val value: T) {
    fun foo(): T = value
}

fun <R> test(r: R): R = object {
    fun run(): R {
        open class L1<T>(v: T) : Base<T>(v)
        class L2<T>(v: T) : L1<T>(v)
        return L2(r).foo()
    }
}.run()

// MODULE: main(lib)
// FILE: main.kt
fun box(): String = test("OK")
