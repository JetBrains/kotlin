// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-60854
// WITH_STDLIB
// FULL_JDK

// MODULE: common
// FILE: common.kt

expect open class CancellationException(message: String?) : IllegalStateException

class TimeoutCancellationException(message: String) : CancellationException(message)

// MODULE: platform()()(common)
// FILE: platform.kt

public actual typealias CancellationException = java.util.concurrent.CancellationException

fun box(): String = "OK"
