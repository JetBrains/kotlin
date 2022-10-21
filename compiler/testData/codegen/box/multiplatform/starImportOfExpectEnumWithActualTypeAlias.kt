// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND_K2: JVM_IR
// K2 status: caused by: java.lang.IllegalStateException: Should not be here!
// It will be fixed after merging of MPP branch

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
