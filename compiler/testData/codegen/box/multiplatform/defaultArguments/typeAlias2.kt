// !LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// IGNORE_BACKEND_K1: JVM, JVM_IR, JS, JS_IR, JS_IR_ES6, WASM
// IGNORE_NATIVE_K1: mode=ONE_STAGE_MULTI_MODULE

// MODULE: common
// FILE: common.kt
expect annotation class Foo(val z: String = "OK")

// MODULE: main()()(common)
// FILE: main.kt
actual typealias Foo = Foo2

annotation class Foo2 (val z: String = "OK")

@Foo
fun test() {}

fun box(): String {
    test()

    return "OK"
}