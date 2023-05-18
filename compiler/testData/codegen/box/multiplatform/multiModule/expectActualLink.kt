// !LANGUAGE: +MultiPlatformProjects
// KJS_WITH_FULL_RUNTIME
// EXPECT_ACTUAL_LINKER
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: WASM
// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JVM_IR
// DONT_TARGET_EXACT_BACKEND: ANDROID

// MODULE: A
// FILE: a.kt
@file:Suppress("NO_ACTUAL_FOR_EXPECT")
package foo

expect fun foo1(): String
expect fun foo2(): String
expect fun foo3(): String
expect fun foo4(): String
expect fun foo5(): String
expect fun foo6(): String

actual fun foo1() = "1"
actual fun foo2() = "2"

fun use1() = foo1()
fun use3() = foo3()


// MODULE: B(A)
// FILE: b.kt
@file:Suppress("ACTUAL_WITHOUT_EXPECT")
package foo

actual fun foo3() = "3"
actual fun foo4() = "4"
actual fun foo5() = "5"

fun use2() = foo2()
fun use4() = foo4()
fun use6() = foo6()


// MODULE: C(B)
// FILE: c.kt
@file:Suppress("ACTUAL_WITHOUT_EXPECT")
package foo

actual fun foo6() = "6"

fun use5() = foo5()


// MODULE: main(C)
// FILE: main.kt
package main

import foo.*

fun box(): String {
    return if (use1() + use2() + use3() + use4() + use5() + use6() == "123456") "OK" else "FAIL"
}

