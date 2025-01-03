// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>CompatibleOverrides<!> {
    fun foo()

    @Ann
    fun foo(withArg: Any)
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual class CompatibleOverrides {
    actual fun foo() {}

    actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>foo<!>(withArg: Any) {}
}
