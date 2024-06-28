// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// FULL_JDK
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
//   Ignore reason: expect/actual are not supported in K1 box tests
// ISSUE: KT-58030

// MODULE: common
// FILE: common.kt

expect open class CancellationException: Exception

expect class JobCancellationException: CancellationException

// MODULE: jvm()()(common)
// FILE: jvm.kt

actual open class CancellationException: Exception()

actual class JobCancellationException: CancellationException() {
    init {
        super.fillInStackTrace()
    }
}

fun box() = "OK"
