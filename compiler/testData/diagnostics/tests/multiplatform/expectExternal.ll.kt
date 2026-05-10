// LL_FIR_DIVERGENCE
// WasmWasi errors are additionally reported compared to the compiler test data. WasmWasi checkers run in LL because `m1-common` is a common
// module. In compiler mode, all modules in the test have the same target platform, so `m1-common` is a JVM module there.
// LL_FIR_DIVERGENCE
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

<!WASI_EXTERNAL_FUNCTION_WITHOUT_IMPORT!>expect <!EXPECTED_EXTERNAL_DECLARATION!>external<!> fun foo()<!>
expect fun bar()

<!WASI_EXTERNAL_NOT_TOP_LEVEL_FUNCTION!>expect <!EXPECTED_EXTERNAL_DECLARATION, WRONG_MODIFIER_TARGET, WRONG_MODIFIER_TARGET!>external<!> var prop: String<!>

<!WASI_EXTERNAL_NOT_TOP_LEVEL_FUNCTION!>expect var getAndSet: String
    <!EXPECTED_EXTERNAL_DECLARATION!>external<!> get
    <!EXPECTED_EXTERNAL_DECLARATION!>external<!> set<!>

<!WASI_EXTERNAL_NOT_TOP_LEVEL_FUNCTION!><!EXPECTED_EXTERNAL_DECLARATION, WRONG_MODIFIER_TARGET, WRONG_MODIFIER_TARGET!>external<!> expect val explicitGetter: String
    <!EXPECTED_EXTERNAL_DECLARATION!>external<!> get<!>

<!WASI_EXTERNAL_NOT_TOP_LEVEL_FUNCTION!>expect <!EXPECTED_EXTERNAL_DECLARATION, WRONG_MODIFIER_TARGET, WRONG_MODIFIER_TARGET!>external<!> class A {
    <!EXPECTED_EXTERNAL_DECLARATION!>external<!> fun foo()
    fun bar()
}<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual external fun foo()
actual external fun bar()

actual <!WRONG_MODIFIER_TARGET!>external<!> var prop: String

actual var getAndSet: String
    external get
    external set

actual <!WRONG_MODIFIER_TARGET!>external<!> val explicitGetter: String
    external get

actual class A {
    actual external fun foo()
    actual external fun bar()
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, external, functionDeclaration, propertyDeclaration */
