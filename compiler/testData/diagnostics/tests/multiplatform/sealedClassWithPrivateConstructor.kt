// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-58033
// MODULE: m1-common
// FILE: common.kt

expect sealed class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Frame<!> private constructor()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual sealed class Frame actual constructor()
