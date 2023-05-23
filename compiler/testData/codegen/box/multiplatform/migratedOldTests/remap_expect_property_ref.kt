// LANGUAGE: +MultiPlatformProjects
// !OPT_IN: kotlin.ExperimentalMultiplatform
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: WASM
// TARGET_BACKEND: JVM_IR

// IGNORE_BACKEND_K2: ANY

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
