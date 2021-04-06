// IGNORE_BACKEND_FIR: JVM_IR

// Note: this test will fail in Kotlin 1.6 (see AbstractClassMemberNotImplementedWithIntermediateAbstractClass feature)
// FILE: 1.kt
class Test: Impl(), CProvider

fun box() = "OK"

// FILE: 2.kt
open class C
class D: C()

interface CProvider {
    fun getC(): C
}

interface DProvider {
    fun getC(): D = D()
}

open class Impl: DProvider
