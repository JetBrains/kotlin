// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM
// WITH_STDLIB

/*
 * This test is used for general testing of how compiler BB tests for HMPP projects works
 * Js backend is ignored because they use old test infrastructure which doesn't support HMPP
 */

// MODULE: common
// FILE: common.kt
expect open class A()

// MODULE: intermediate()()(common)
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
