// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common_1
// FILE: common1.kt
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>A<!>() {
    fun foo(): String
}

// MODULE: common_2
// FILE: common2.kt
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>A<!>() {
    fun bar(): String
}

// MODULE: main()()(common_1, common_2)
// FILE: test.kt
actual class <!AMBIGUOUS_EXPECTS, PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!> actual constructor() {
    actual fun foo(): String = "O"
    actual fun <!ACTUAL_WITHOUT_EXPECT!>bar<!>(): String = "K"
}
