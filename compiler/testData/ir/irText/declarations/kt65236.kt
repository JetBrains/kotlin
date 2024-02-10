// FIR_IDENTICAL
// ENABLE_IR_FAKE_OVERRIDE_GENERATION
// TARGET_BACKEND: JVM

// FILE: J.java

interface J extends I {

}

// FILE: E.kt

interface II {
    fun foo() {}
}

interface I : II {
}

abstract class C: I {
    override abstract fun foo()
}

abstract class D : C(), J {}

class E : D() {
    override fun foo() {}
}

fun foo(x : I) {
    x.foo()
}