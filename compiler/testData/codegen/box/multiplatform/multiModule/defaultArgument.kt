// !LANGUAGE: +MultiPlatformProjects
// KJS_WITH_FULL_RUNTIME
// EXPECT_ACTUAL_LINKER
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_BACKEND: NATIVE
// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JVM_IR
// KT-45542

// MODULE: E
// FILE: e.kt
@file:Suppress("NO_ACTUAL_FOR_EXPECT")

expect class Test() {
    fun size(): Int
    fun lastIndex(start: Int, end: Int = size())

    fun result(): String
}

// MODULE: A(E)
// FILE: a.kt
@file:Suppress("ACTUAL_WITHOUT_EXPECT")

actual class Test {

    var r = ""

    actual fun size(): Int = 3
    actual fun lastIndex(start: Int, end: Int) {
        r = "OK"
    }

    actual fun result(): String = r
}

// MODULE: U(E)
// FILE: u.kt

fun foo(): String {
    val t = Test()
    t.lastIndex(0)
    return t.result()
}

// MODULE: main(U, A)
// FILE: m.kt

fun box(): String {
    return foo()
}