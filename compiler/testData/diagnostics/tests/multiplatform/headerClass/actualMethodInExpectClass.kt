// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> {
    actual fun <!ACTUAL_WITHOUT_EXPECT{JVM}!>bar<!>()
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

actual class Foo {
    actual fun bar() {}
}
