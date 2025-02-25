// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
class A

open class Base {
    context(a: A)
    open fun foo() { }

    context(a: A)
    open val b: String
        get() = ""
}

class Derived : Base() {
    context(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>x<!>: A)
    override fun foo() {}

    context(x: A)
    override val b: String
        get() = "2"
}