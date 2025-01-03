// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -MultiplatformRestrictions
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
expect annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>TypealiasToKotlinPkg<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual typealias TypealiasToKotlinPkg = kotlin.Deprecated

