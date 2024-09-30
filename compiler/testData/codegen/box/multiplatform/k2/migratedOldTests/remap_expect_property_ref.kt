// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR, JS_IR, WASM, NATIVE
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalMultiplatform

// MODULE: common
// FILE: common.kt

expect class Foo {
    val p: Int
    fun bar(r: () -> Int = this::p): Int
}

// MODULE: actual()()(common)
// FILE: actual.kt

actual class Foo {
    actual val p = 42
    actual fun bar(r: () -> Int) = r()
}
fun box(): String {
    val bar = Foo().bar()
    if (bar != 42)
        return "bar is wrongly $bar"

    return "OK"
}
