// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JS_IR

// MODULE: common
// FILE: common.kt

package test

expect enum class E

// MODULE: jvm1(common)
// FILE: jvm.kt

package test

actual typealias E = F

enum class F {
    OK;
}

// MODULE: main(jvm1)
// FILE: jvm2.kt

import test.E.*

fun box(): String {
    return OK.name
}