// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: expect/actual in the same module (ACTUAL_WITHOUT_EXPECT)
// TARGET_BACKEND: JVM
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
