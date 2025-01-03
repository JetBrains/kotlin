// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
// ISSUE: KT-68648
abstract class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>BaseClass<!>(private val x: Int)

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>ExpectClass<!> : BaseClass {}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual class ExpectClass(val x: Int) : BaseClass(x)
