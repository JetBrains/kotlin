// MODULE: commonwasm
// TARGET_PLATFORM: Wasm
// FILE: commonwasm.kt

expect interface <!NO_ACTUAL_FOR_EXPECT!>ExternalInterface<!>

external fun externalFunction(<!WRONG_JS_INTEROP_TYPE!>arg: ExternalInterface<!>)

// MODULE: wasm()()(commonwasm)
// TARGET_PLATFORM: Wasm
// FILE: wasm.kt

actual external interface ExternalInterface
