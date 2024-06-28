// LL_FIR_DIVERGENCE
// ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT reported because in LL we have access to the stdlib sources,
// thus can check SOURCE-retention annotations.
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
