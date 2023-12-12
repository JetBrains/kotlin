// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

expect class Runnable

fun foo(arg: Runnable) {
    arg.hashCode()
}

// MODULE: main()()(common)
// FILE: test.kt

actual class Runnable

fun box() = "OK"
