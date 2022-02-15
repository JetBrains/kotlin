// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

interface Base {
    fun foo()
}

expect object Implementation : Base

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

abstract class RealImplementation : Base {
    override fun foo() {}
}

actual object Implementation : RealImplementation(), Base
