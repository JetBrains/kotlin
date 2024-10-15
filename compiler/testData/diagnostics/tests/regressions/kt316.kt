// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// KT-316 Members of traits must be open by default

interface B {
    fun bar() {}
    fun foo() {}
}

open class A() : B{
    override fun foo() {}
}
