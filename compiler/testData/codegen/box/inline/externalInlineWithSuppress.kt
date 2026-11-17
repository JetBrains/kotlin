// TARGET_BACKEND: JS_IR, JS_IR_ES6, WASM_JS
// FILE: main.kt
@file:Suppress(
    "WRONG_BODY_OF_EXTERNAL_DECLARATION",
    "INLINE_EXTERNAL_DECLARATION",
)
package foo


@JsName("Object")
external interface Foo {
    companion object {
        inline fun test(): String = "OK"
    }
}

fun box(): String {
    return Foo.test()
}

// FILE: lib.js
function Foo() {}
