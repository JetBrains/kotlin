// RUN_PIPELINE_TILL: BACKEND
// LL_FIR_DIVERGENCE
// AA doesn't run the compilation, so no metadata is present
// See KmpCompilationMode.LOW_LEVEL_API
// LL_FIR_DIVERGENCE
// MODULE: commonwasm
// FILE: commonwasm.kt

expect interface ExternalInterface

external fun externalFunction(arg: <!WRONG_JS_INTEROP_TYPE!>ExternalInterface<!>)

// MODULE: wasm()()(commonwasm)
// FILE: wasm.kt

actual external interface ExternalInterface
