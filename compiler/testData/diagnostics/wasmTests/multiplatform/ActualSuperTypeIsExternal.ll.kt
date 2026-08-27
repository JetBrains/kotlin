// LL_FIR_DIVERGENCE
// LL diagnostic is not reported in common source set
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-64219
// MODULE: commonwasm
// FILE: commonwasm.kt

expect interface ExternalInterface

interface A : ExternalInterface

expect interface B : ExternalInterface

// MODULE: wasm()()(commonwasm)
// FILE: wasm.kt

actual external interface ExternalInterface

actual external interface B : ExternalInterface
