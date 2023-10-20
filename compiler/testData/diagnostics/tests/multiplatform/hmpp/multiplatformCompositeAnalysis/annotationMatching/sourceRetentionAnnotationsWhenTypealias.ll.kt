// LL_FIR_DIVERGENCE
// Bug KT-62886
// LL_FIR_DIVERGENCE
// FIR_IDENTICAL
// WITH_STDLIB
// MODULE: common
// TARGET_PLATFORM: Common
expect annotation class Ann() // No @Retention SOURCE set

@Ann
expect annotation class CommonVolatile

// MODULE: main()()(common)
// TARGET_PLATFORM: JVM
@Retention(AnnotationRetention.SOURCE)
actual annotation class Ann

actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>CommonVolatile<!> = kotlin.jvm.Volatile
