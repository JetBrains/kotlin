// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt
annotation class Ann

interface I {
    @Ann
    fun noAnnotationOnActual()
}

expect class FakeOverrideExpect : I

interface I2 {
    fun noAnnotationOnActual()
}

expect class FakeOverrideActual : I2 {
    @Ann
    override fun noAnnotationOnActual()
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual class FakeOverrideExpect : I {
    override fun noAnnotationOnActual() {}
}

abstract class Intermediate : I2 {
    override fun noAnnotationOnActual() {}
}

actual class FakeOverrideActual : Intermediate(), I2
