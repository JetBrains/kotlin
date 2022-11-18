// TARGET_BACKEND: JVM
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect interface S1
expect interface S2

expect class S

open class A : S1, S2

class B : A()

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt

actual interface S1 {
    fun o(): S = "O"
    val p: Boolean
        get() = true
}

actual interface S2 {
    fun k() = "K"
}

actual typealias S = String

fun box(): String {
    val b = B()
    return if (b.p) {
        b.o() + b.k()
    } else {
        "FAIL"
    }
}