// TARGET_BACKEND: JS_IR
// ISSUE: KT-57988
// FIR_DUMP
// IGNORE_BACKEND_K2_MULTI_MODULE: JS_IR
// ^^^ Multi-module FIR dump has extra lines `Module: lib` and `Module: main`

// FILE: lib.kt
inline fun <T : Any> jso(): T = js("({})")
inline fun <T : Any> jso(block: T.() -> Unit): T = jso<T>().apply(block)

// FILE: main.kt
external interface Z {
    var a: dynamic
}

fun foo() {
    jso<Z>().apply {
        a = jso {
            this[foo.bar]
        }
    }
}

fun box() = "OK"