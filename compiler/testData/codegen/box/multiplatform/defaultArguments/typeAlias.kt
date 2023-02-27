// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K2: ANY
// FIR status: outdated code (expect and actual in the same module)
// WITH_STDLIB

// FILE: common.kt

expect annotation class Foo(val z: String = "OK")

// FILE: platform.kt

actual typealias Foo = Foo2

annotation class Foo2 (val z: String = "OK")

@Foo
fun test() {}

fun box(): String {
    test()

    return "OK"
}