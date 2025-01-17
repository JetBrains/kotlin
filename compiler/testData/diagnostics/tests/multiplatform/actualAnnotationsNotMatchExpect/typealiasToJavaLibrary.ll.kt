// LL_FIR_DIVERGENCE
// Not a real LL divergence, it's just tiered runners reporting errors from `BACKEND`
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
@Retention(AnnotationRetention.SOURCE)
expect annotation class MyDeprecatedNotMatch

@Retention(AnnotationRetention.RUNTIME)
expect annotation class MyDeprecatedMatch

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>MyDeprecatedNotMatch<!> = java.lang.Deprecated

actual typealias MyDeprecatedMatch = java.lang.Deprecated
