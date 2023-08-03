// !LANGUAGE: +ContextReceivers

interface Context1

interface Context2<A> {
    fun getContextElement(): A
}

class Context2Impl<A>(val value: A) : Context2<A> {
    override fun getContextElement(): A = value
}

context(Int, String)
class A {
    context(Double)
    constructor(int: Int) {}
}

context(Context1, c2@Context2<String>)
class B {
    override fun toString(): String = getContextElement()
}

context(Context2<A>)
class C<A> {
    val a: A
        get() = getContextElement()
}

context(Context1)
@Deprecated("Use `B` instead.")
class D

context(b@B, `fun`@A)
fun foo() = Unit

context(Context2<C<String>>)
fun bar() = with (Context2Impl(getContextElement().a)) { C() }
