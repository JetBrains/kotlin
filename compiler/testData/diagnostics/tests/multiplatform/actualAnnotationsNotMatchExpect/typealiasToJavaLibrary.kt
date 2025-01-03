// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
@Retention(AnnotationRetention.SOURCE)
expect annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>MyDeprecatedNotMatch<!>

@Retention(AnnotationRetention.RUNTIME)
expect annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>MyDeprecatedMatch<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>MyDeprecatedNotMatch<!> = java.lang.Deprecated

actual typealias MyDeprecatedMatch = java.lang.Deprecated
