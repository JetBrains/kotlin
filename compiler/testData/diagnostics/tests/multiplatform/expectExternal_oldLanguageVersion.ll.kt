// LL_FIR_DIVERGENCE
// WasmWasi errors are additionally reported compared to the compiler test data. WasmWasi checkers run in LL because `m1-common` is a common
// module. In compiler mode, all modules in the test have the same target platform, so `m1-common` is a JVM module there.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -MultiplatformRestrictions
// MODULE: m1-common
// FILE: common.kt

<!WASI_EXTERNAL_FUNCTION_WITHOUT_IMPORT!>expect external fun foo()<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual external fun foo()

/* GENERATED_FIR_TAGS: actual, expect, external, functionDeclaration */
