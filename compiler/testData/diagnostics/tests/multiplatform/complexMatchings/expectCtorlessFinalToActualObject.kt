// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>E01<!>
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>E02<!>()
expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>E03<!>

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>E04<!> {
    constructor()
}

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>E05<!>(e: E01)
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>E06<!> {
    constructor(e: E02)
}

expect interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>I01<!>

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>M01<!> {
    fun foo()
}

expect enum class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>ENUM01<!>

expect annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>ANNO01<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual object E01
actual <!ACTUAL_WITHOUT_EXPECT!>object<!> E02
actual <!ACTUAL_WITHOUT_EXPECT!>object<!> E03

actual <!ACTUAL_WITHOUT_EXPECT!>object<!> E04

actual <!ACTUAL_WITHOUT_EXPECT!>object<!> E05
actual <!ACTUAL_WITHOUT_EXPECT!>object<!> E06

actual <!ACTUAL_WITHOUT_EXPECT!>object<!> I01

actual object M01 {
    actual fun foo() {}
}

actual <!ACTUAL_WITHOUT_EXPECT!>object<!> ENUM01

actual <!ACTUAL_WITHOUT_EXPECT!>object<!> ANNO01
