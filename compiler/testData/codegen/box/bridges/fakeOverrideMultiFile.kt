// !LANGUAGE: -AbstractClassMemberNotImplementedWithIntermediateAbstractClass
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, NATIVE
// FIR status: don't support legacy feature

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
