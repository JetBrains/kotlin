// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
class A {
    fun foo(a: String): String { return a }
}

context(ctx: T)
fun <T> implicit(): T = ctx

open class Base {
    open fun foo(x: context(A)() -> Unit) {}

    open fun bar(): context(A)() -> Unit {
        return {}
    }

    open val x: context(A)() -> Unit = {}
}

class Derived : Base()

class DerivedOverrideWithValue : Base() {
    override fun foo(x: (A) -> Unit) {}

    override val x: (A) -> Unit
        get() = { }

    override fun bar(): (A) -> Unit {
        return {}
    }
}

class DerivedOverrideWithExtension : Base() {
    override fun foo(x: A.() -> Unit) {}

    override val x: A.() -> Unit
        get() = { }

    override fun bar(): A.() -> Unit {
        return {}
    }
}

class DerivedOverrideWithContext : Base() {
    override fun foo(x: context(A)() -> Unit) {}

    override val x: context(A)() -> Unit
        get() = { }

    override fun bar(): context(A)() -> Unit {
        return {}
    }
}

fun usage() {
    Derived().foo { implicit<A>().foo("") }
    Derived().bar()(A())
    Derived().x(A())
    DerivedOverrideWithValue().foo { y: A -> y.foo("") }
    DerivedOverrideWithValue().bar()(A())
    DerivedOverrideWithValue().x(A())
    DerivedOverrideWithExtension().foo { implicit<A>().foo("") }
    DerivedOverrideWithExtension().bar()(A())
    DerivedOverrideWithExtension().x(A())
    DerivedOverrideWithContext().foo { implicit<A>().foo("") }
    DerivedOverrideWithContext().bar()(A())
    DerivedOverrideWithContext().x(A())
}