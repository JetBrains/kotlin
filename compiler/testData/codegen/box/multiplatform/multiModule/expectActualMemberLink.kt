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

expect class C1() {
    fun foo1(): String
}
expect class C2() {
    fun foo2(): String
}
expect class C3() {
    fun foo3(): String
}
expect class C4() {
    fun foo4(): String
}
expect class C5() {
    fun foo5(): String
}
expect class C6() {
    fun foo6(): String
}

actual class C1 {
    actual fun foo1() = "1"
}
actual class C2 {
    actual fun foo2() = "2"
}

fun use1() = C1().foo1()
fun use3() = C3().foo3()


// MODULE: B(A)
// FILE: b.kt
@file:Suppress("ACTUAL_WITHOUT_EXPECT")
package foo

actual class C3 {
    actual fun foo3() = "3"
}
actual class C4 {
    actual fun foo4() = "4"
}
actual class C5 {
    actual fun foo5() = "5"
}

fun use2() = C2().foo2()
fun use4() = C4().foo4()
fun use6() = C6().foo6()


// MODULE: C(B)
// FILE: c.kt
@file:Suppress("ACTUAL_WITHOUT_EXPECT")
package foo

actual class C6 {
    actual fun foo6() = "6"
}

fun use5() = C5().foo5()


// MODULE: main(C)
// FILE: main.kt
package main

import foo.*

fun box(): String {
    return if (use1() + use2() + use3() + use4() + use5() + use6() == "123456") "OK" else "FAIL"
}

