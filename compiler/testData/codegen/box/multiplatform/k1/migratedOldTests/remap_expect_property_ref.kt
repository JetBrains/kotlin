// LANGUAGE: +MultiPlatformProjects
// OPT_IN: kotlin.ExperimentalMultiplatform
// TARGET_BACKEND: JVM_IR, JS_IR, WASM, NATIVE

// FILE: common.kt

expect class Foo {
    val p: Int
    fun bar(r: () -> Int = this::p): Int
}

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
