// FIR_IDENTICAL
// ENABLE_IR_FAKE_OVERRIDE_GENERATION
// TARGET_BACKEND: JVM

open class Base
class Child: Base()

interface I {
    fun foo(): Base
}

abstract class J {
    abstract fun foo(): Child
}

abstract class A : I, J()