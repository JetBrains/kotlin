// TARGET_BACKEND: JVM_IR, WASM_JS, WASM_WASI
//  ^Reason: JS doesn't support inheritance from FunctionN, Native doesn't support coroutine testing utilities
// WITH_STDLIB
// WITH_COROUTINES
// ISSUE: KT-82683

// MODULE: lib
// FILE: lib.kt
interface MyCallable<E> : () -> E

// MODULE: main(lib)
// FILE: test.kt

fun foo(x: suspend () -> Any?) {
    helpers.runBlocking(x)
}
fun bar(x: MyCallable<Any?>) {
    foo(x)
}

fun box(): String {
    var o = "fail"
    foo(object : MyCallable<Any?> {
        override fun invoke(): Any? {
            o = "OK"
            return null
        }
    })
    return o
}
