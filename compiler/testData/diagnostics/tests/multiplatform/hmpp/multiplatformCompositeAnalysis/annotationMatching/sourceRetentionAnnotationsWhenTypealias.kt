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

actual typealias CommonVolatile = kotlin.jvm.Volatile
