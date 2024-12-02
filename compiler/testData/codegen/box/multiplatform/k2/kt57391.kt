// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-57391
// WITH_STDLIB

// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM
// KT-57391: java.lang.IllegalStateException: IrSimpleFunctionSymbolImpl for /main|main(){}[0] is already bound

// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// IGNORE_BACKEND: NATIVE
// KT-57391: error: platform declaration clash: The following functions have the same IR signature

// MODULE: common
// FILE: common.kt
fun main() {
    println("Hello, Foo")
}

// MODULE: platform()()(common)
// FILE: platform.kt
fun main() {
    println("Hello, Kotlin/Native!")
}

fun box() = "OK"
