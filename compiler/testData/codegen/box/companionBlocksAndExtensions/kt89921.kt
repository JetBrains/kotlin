// LANGUAGE: +CompanionBlocks
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI, NATIVE

// MODULE: lib
// FILE: lib.kt
class A {
    companion {
        inline fun noParameters(): String = "O"
    }
}

class B {
    companion {
        inline fun noParameters(): String = "K"
    }
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    return A.noParameters() + B.noParameters()
}
