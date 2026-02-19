// LANGUAGE: +MultiPlatformProjects
// MODULE: common
// FILE: common.kt

package test

expect class A {
    class B
}

expect fun A.B.test(): String

expect val A.B.bar: String

// MODULE: platform()()(common)
// FILE: platform.kt

package test

actual class A {
    actual class B
}

actual fun A.B.test(): String {
    return "O"
}

actual val A.B.bar: String
    get() = "K"

fun box(): String = A.B().test() + A.B().bar