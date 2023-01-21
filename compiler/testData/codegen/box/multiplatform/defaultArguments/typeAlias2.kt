// !LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_K2: NATIVE

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