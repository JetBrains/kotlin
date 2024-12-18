// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
class A

open class Base {
    context(a: A)
    open fun foo() {}

    context(a: A)
    open val b: String
        get() = "1"
}

class Derived: Base()

class DerivedWithoutContext : Base() {
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo() {}
    <!NOTHING_TO_OVERRIDE!>override<!> val b: String
        get() = "2"
}

class DerivedWithContext : Base() {
    context(a: A)
    override fun foo() {}

    context(a: A)
    override val b: String
        get() = "2"
}

fun usage() {
    with(A()) {
        Derived().foo()
        Derived().b
        DerivedWithContext().foo()
        DerivedWithContext().b
    }
}