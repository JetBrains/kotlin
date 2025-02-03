// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
class A

open class Base<T> {
    context(a: T)
    open fun foo() { }

    context(a: T)
    open val b: String
        get() = "1"
}

class Derived : Base<A>()

class DerivedWithOverride : Base<A>() {
    context(a: A)
    override fun foo() {}

    context(a: A)
    override val b: String
        get() = "2"
}

fun usage() {
    with(A()) {
        DerivedWithOverride().foo()
        DerivedWithOverride().b
        Derived().foo()
        Derived().b
    }
}