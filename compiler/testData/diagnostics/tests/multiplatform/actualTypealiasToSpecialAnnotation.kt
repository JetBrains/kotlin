// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
expect annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>TypealiasToKotlinPkg<!>

internal expect annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>TypealiasToInternalPkg<!>

expect annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>TypealiasToAnnotationPkg<!>

expect annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>TypealiasToPlatformPkg<!>

expect enum class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>TypealiasNotToAnnotation<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual typealias TypealiasToKotlinPkg = <!ACTUAL_TYPEALIAS_TO_SPECIAL_ANNOTATION!>kotlin.Deprecated<!>

@Suppress("INVISIBLE_REFERENCE")
internal actual typealias TypealiasToInternalPkg = <!ACTUAL_TYPEALIAS_TO_SPECIAL_ANNOTATION!>kotlin.internal.RequireKotlin<!>

actual typealias TypealiasToAnnotationPkg = <!ACTUAL_TYPEALIAS_TO_SPECIAL_ANNOTATION!>kotlin.annotation.Target<!>

actual typealias TypealiasToPlatformPkg = kotlin.jvm.Synchronized

typealias NonActualTypealias = kotlin.Deprecated

actual typealias TypealiasNotToAnnotation = kotlin.DeprecationLevel
