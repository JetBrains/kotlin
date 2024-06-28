// FIR_IDENTICAL
// ISSUE: KT-36188

interface A {
    fun foo(a: String = "Fail"): String
}
interface B {
    fun foo(a: String = "OK"): String
}
class Impl : A, B {
    override fun foo(<!MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES!>a: String<!>) = a
}

fun box(): String = Impl().foo()
