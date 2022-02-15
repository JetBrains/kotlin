// FIR_IDENTICAL
// FIR_IDE_IGNORE
/*
 * This test is used for general testing of how compiler diagnostics tests for HMPP projects works
 * Js backend is ignored because they use old test infrastructure which doesn't support HMPP
 */


// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// MODULE: common
// TARGET_PLATFORM: Common
expect open class A()

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
class B : A() {
    fun foo(): String = "O"
}

fun getB(): B = B()

// MODULE: main()()(intermediate)
actual open class A actual constructor() {
    fun bar(): String = "K"
}

fun box(): String {
    val b = getB()
    return b.foo() + b.bar()
}
