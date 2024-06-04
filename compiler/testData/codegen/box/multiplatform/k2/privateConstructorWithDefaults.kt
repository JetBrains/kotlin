// IGNORE_BACKEND: WASM
//   Ignore wasm because KT-68828
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-68798

// MODULE: common
// FILE: common.kt

expect open class Frame private constructor(disposableHandle: CharSequence = "OK")

// MODULE: platform()()(common)
// FILE: platform.kt
actual open class Frame actual constructor(val disposableHandle: CharSequence) {
    class Break : Frame()
}

fun box() = Frame.Break().disposableHandle
