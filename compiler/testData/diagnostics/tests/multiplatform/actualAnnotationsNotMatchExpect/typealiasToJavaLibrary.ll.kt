// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
@Retention(AnnotationRetention.SOURCE)
expect annotation class MyDeprecatedNotMatch

@Retention(AnnotationRetention.RUNTIME)
expect annotation class MyDeprecatedMatch

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual typealias MyDeprecatedNotMatch = java.lang.Deprecated

actual typealias MyDeprecatedMatch = java.lang.Deprecated
