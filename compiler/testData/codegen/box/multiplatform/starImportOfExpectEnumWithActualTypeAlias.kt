// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_BACKEND_K1: WASM

// MODULE: lib-common
// FILE: common.kt

package test

expect enum class E

// MODULE: lib-jvm()()(lib-common)
// FILE: jvm.kt

package test

actual typealias E = F

enum class F {
    OK;
}

// MODULE: main(lib-jvm)
// FILE: jvm2.kt

import test.E.*

fun box(): String {
    return OK.name
}
