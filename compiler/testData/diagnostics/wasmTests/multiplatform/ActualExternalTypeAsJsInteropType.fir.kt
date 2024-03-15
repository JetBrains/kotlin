// MODULE: commonwasm
// TARGET_PLATFORM: Wasm
// FILE: commonwasm.kt

expect interface ExternalInterface

external fun externalFunction(arg: <!WRONG_JS_INTEROP_TYPE{METADATA}!>ExternalInterface<!>)

// MODULE: wasm()()(commonwasm)
// TARGET_PLATFORM: Wasm
// FILE: wasm.kt

actual external interface ExternalInterface
