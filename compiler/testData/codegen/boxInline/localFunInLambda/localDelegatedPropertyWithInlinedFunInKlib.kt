// ISSUE: KT-77103
// WITH_STDLIB
// LANGUAGE: +IrInlinerBeforeKlibSerialization
// NO_CHECK_LAMBDA_INLINING

// IGNORE_IR_DESERIALIZATION_TEST: JS_IR, NATIVE
// ^^^ KT-77103: expected: ARG 3: RICH_PROPERTY_REFERENCE[294, 310] ... reflectionTarget='val localDelegatedProperty: kotlin.Boolean by (...)'
//               actual:   ARG 3: RICH_PROPERTY_REFERENCE[294, 310] ... reflectionTarget='UNBOUND IrLocalDelegatedPropertySymbolImpl'
// IGNORE_BACKEND_K2: NATIVE, JS_IR, JS_IR_ES6
// ^^^ KT-77103: Generation of stubs for class org.jetbrains.kotlin.ir.symbols.impl.IrLocalDelegatedPropertySymbolImpl:Unbound private symbol org.jetbrains.kotlin.ir.symbols.impl.IrLocalDelegatedPropertySymbolImpl@1ed3c2e7 is not supported yet

// After all issues are fixed, please merge this test with `unboundReflectionTargetToLocalDelegatedProperty.kt`

// FILE: 1.kt
inline fun foo(block: () -> Unit) {}

fun app() {
    foo {
        fun bar() {
            val localDelegatedProperty by lazy { false }
        }
    }
}

// FILE: 2.kt
fun box(): String {
    app()
    return "OK"
}
