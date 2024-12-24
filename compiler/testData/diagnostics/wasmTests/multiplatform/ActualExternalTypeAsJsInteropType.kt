// MODULE: commonwasm
// FILE: commonwasm.kt

expect interface ExternalInterface

external fun externalFunction(arg: ExternalInterface)

// MODULE: wasm()()(commonwasm)
// FILE: wasm.kt

actual external interface ExternalInterface
