// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-57391
// WITH_STDLIB

// KT-57391: CONFLICTING_OVERLOADS
// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6, WASM
// KT-57391: java.lang.IllegalStateException: IrSimpleFunctionSymbolImpl for /main|main(){}[0] is already bound
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6, WASM

// KT-57391: Backend Internal error: Exception during psi2ir
// in K1/NATIVE/ONE_STAGE_MULTI_MODULE, native testsystem ignores fun `main` in `common` module, hence `common` module is not compiled at all.
// IGNORE_NATIVE_K1: mode=TWO_STAGE_MULTI_MODULE
// KT-57391: error: platform declaration clash: The following functions have the same IR signature
// IGNORE_BACKEND_K2: NATIVE

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
