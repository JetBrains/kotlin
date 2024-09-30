// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// ISSUE: KT-58030
// WITH_STDLIB
// FULL_JDK

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
