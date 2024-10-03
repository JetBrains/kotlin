// LANGUAGE: +MultiPlatformProjects
// MODULE: common
// FILE: common.kt

package test

expect fun Int.foo(): String

expect fun <T> T.bar(): T

// MODULE: platform()()(common)
// FILE: platform.kt

package test

actual fun Int.foo(): String {
    return "O"
}

actual fun <T> T.bar():T {
    return this
}

fun box(): String =
    1.foo() + "K".bar()


