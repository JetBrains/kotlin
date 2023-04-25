// WITH_STDLIB
// IGNORE_BACKEND_K1: JVM, JVM_IR, JS, JS_IR, JS_IR_ES6, NATIVE, WASM
// KT-57181
//  JS IR & Wasm: https://youtrack.jetbrains.com/issue/KT-51225
// !LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-51156

// MODULE: common
// FILE: common.kt

expect class C(e: E = E.O) {
    enum class E {
        O, K
    }
}

// MODULE: platform()()(common)
// FILE: platform.kt

actual class C actual constructor(e: E) {
    val result = e.name

    actual enum class E {
        O, K
    }
}

// MODULE: main(platform)
// FILE: main.kt

fun box(): String {
    return C().result + C(C.E.K).result
}
