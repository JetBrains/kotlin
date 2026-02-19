// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-68798

// MODULE: common
// FILE: common.kt

expect open class Frame private constructor(disposableHandle: String = "OK")

// MODULE: platform()()(common)
// FILE: platform.kt
actual open class Frame actual constructor(val disposableHandle: String) {
    class Break : Frame()
}

fun box() = Frame.Break().disposableHandle
