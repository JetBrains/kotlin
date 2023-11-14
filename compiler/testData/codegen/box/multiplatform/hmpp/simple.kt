/*
 * This test is used for general testing of how compiler BB tests for HMPP projects works
 * Js backend is ignored because they use old test infrastructure which doesn't support HMPP
 */

// WITH_STDLIB
// !LANGUAGE: +MultiPlatformProjects
// IGNORE_CODEGEN_WITH_IR_FAKE_OVERRIDE_GENERATION: KT-62535
// IGNORE_BACKEND_K2: JVM_IR
//   Ignore reason: (TODO) how should generate IR for common f/o after actualization?

// TARGET_BACKEND: JVM
// MODULE: common
// TARGET_PLATFORM: Common

// FILE: common.kt
expect open class A()

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: JVM, JS

// FILE: intermediate.kt
class B : A() {
    fun foo(): String = "O"
}

fun getB(): B = B()

// MODULE: main()()(intermediate)
// FILE: main.kt
actual open class A actual constructor() {
    fun bar(): String = "K"
}

fun box(): String {
    val b = getB()
    return b.foo() + b.bar()
}
