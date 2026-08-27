// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-64219
// MODULE: commonwasm
// FILE: commonwasm.kt

expect interface ExternalInterface

interface <!NON_EXTERNAL_TYPE_EXTENDS_EXTERNAL_TYPE!>A<!> : ExternalInterface

expect interface B : ExternalInterface

// MODULE: wasm()()(commonwasm)
// FILE: wasm.kt

actual external interface ExternalInterface

actual external interface B : ExternalInterface
