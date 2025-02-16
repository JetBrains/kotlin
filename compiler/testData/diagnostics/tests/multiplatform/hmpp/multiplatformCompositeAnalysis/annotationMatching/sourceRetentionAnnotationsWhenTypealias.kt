// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
// MODULE: common
expect annotation class Ann() // No @Retention SOURCE set

@Ann
expect annotation class CommonVolatile

// MODULE: main()()(common)
@Retention(AnnotationRetention.SOURCE)
actual annotation class Ann

actual typealias CommonVolatile = kotlin.jvm.Volatile
