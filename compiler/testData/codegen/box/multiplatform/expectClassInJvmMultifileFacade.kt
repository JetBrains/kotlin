// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K2: JVM_IR, JS_IR
// IGNORING_WASM_FOR_K2
// IGNORE_BACKEND: WASM
// FIR status: expect/actual in the same module (ACTUAL_WITHOUT_EXPECT)
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K2: ANY
// FIR status: outdated code (expect and actual in the same module)
// WITH_STDLIB
// FILE: common.kt

@file:JvmMultifileClass
@file:JvmName("Test")
package test

expect class Foo {
    val value: String
}

// FILE: jvm.kt

@file:JvmMultifileClass
@file:JvmName("Test")
package test

actual class Foo(actual val value: String)

fun box(): String {
    return Foo("OK").value
}
