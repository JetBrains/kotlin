// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>SomeClass<!><T> {
    fun foo()
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual class <!ACTUAL_WITHOUT_EXPECT!>SomeClass<!> {
    actual fun foo() {}
}
