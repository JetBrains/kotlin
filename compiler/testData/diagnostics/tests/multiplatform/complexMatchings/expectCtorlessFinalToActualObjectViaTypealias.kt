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

object ActualObject {
    fun foo() {}
}

actual typealias E01 = ActualObject
actual typealias <!ACTUAL_WITHOUT_EXPECT!>E02<!> = ActualObject
actual typealias <!ACTUAL_WITHOUT_EXPECT!>E03<!> = ActualObject

actual typealias <!ACTUAL_WITHOUT_EXPECT!>E04<!> = ActualObject

actual typealias <!ACTUAL_WITHOUT_EXPECT!>E05<!> = ActualObject
actual typealias <!ACTUAL_WITHOUT_EXPECT!>E06<!> = ActualObject

actual typealias <!ACTUAL_WITHOUT_EXPECT!>I01<!> = ActualObject

actual typealias M01 = ActualObject

actual typealias <!ACTUAL_WITHOUT_EXPECT!>ENUM01<!> = ActualObject

actual typealias <!ACTUAL_WITHOUT_EXPECT!>ANNO01<!> = ActualObject
