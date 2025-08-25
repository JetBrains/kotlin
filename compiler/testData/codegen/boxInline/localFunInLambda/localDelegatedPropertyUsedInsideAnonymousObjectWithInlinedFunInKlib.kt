// ISSUE: KT-77103
// WITH_STDLIB
// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization
// NO_CHECK_LAMBDA_INLINING

// IGNORE_IR_DESERIALIZATION_TEST: JS_IR, NATIVE

// IGNORE_BACKEND_K2: NATIVE, JS_IR, JS_IR_ES6, WASM
// ^^^ KT-77103: Generation of stubs for class org.jetbrains.kotlin.ir.symbols.impl.IrLocalDelegatedPropertySymbolImpl:Unbound private symbol org.jetbrains.kotlin.ir.symbols.impl.IrLocalDelegatedPropertySymbolImpl@1ed3c2e7 is not supported yet

// After all issues are fixed, please merge this test with `unboundReflectionTargetToLocalDelegatedProperty.kt`

// FILE: 1.kt
inline fun <T> foo(block: () -> T) = block()

fun app() : String {
    return foo {
        val localDelegatedProperty by lazy { "OK" }
        object {
            fun get() = localDelegatedProperty
        }.get()
    }
}

// FILE: 2.kt
fun box(): String {
    app()
    return "OK"
}
