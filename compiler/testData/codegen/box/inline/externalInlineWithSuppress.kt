// IGNORE_BACKEND: JVM, JVM_IR, WASM_WASI, NATIVE
// FILE: main.kt
@file:Suppress(
    "WRONG_BODY_OF_EXTERNAL_DECLARATION",
    "INLINE_EXTERNAL_DECLARATION",
    "NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE",
    "DECLARATION_CANT_BE_INLINED",
)
package foo


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
