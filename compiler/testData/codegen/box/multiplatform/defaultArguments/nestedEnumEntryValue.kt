// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM
//  JS IR & Wasm: https://youtrack.jetbrains.com/issue/KT-51225
// IGNORE_BACKEND_K2: JVM_IR, NATIVE
// FIR status: default argument mapping in MPP isn't designed yet
// !LANGUAGE: +MultiPlatformProjects
// MODULE: lib
// FILE: common.kt

// KT-51156

expect class C(e: E = E.O) {
    enum class E {
        O, K
    }
}

// FILE: platform.kt

actual class C actual constructor(e: E) {
    val result = e.name

    actual enum class E {
        O, K
    }
}

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    return C().result + C(C.E.K).result
}
