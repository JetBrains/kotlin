// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

expect interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Interface<!>

expect annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Anno<!>(val prop: String)

expect object <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Object<!>

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Class<!>

expect enum class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>En<!> { ENTRY }

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual interface Interface

actual annotation class Anno actual constructor(actual val prop: String)

actual object Object

actual class Class

actual enum class En { ENTRY }
