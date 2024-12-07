// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-36188

interface A {
    fun foo(a: String = "Fail"): String
}
interface A2 : A
interface B {
    fun foo(a: String = "OK"): String
}
class Impl : A2, B {
    override fun foo(<!MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_DEPRECATION_WARNING!>a: String<!>) = a
}

fun box(): String = Impl().foo()