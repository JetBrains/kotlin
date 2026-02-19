// LANGUAGE: +ContextParameters
// IGNORE_FE10

interface Context2<A> {
    fun getContextElement(): A
}

class Context2Impl<A>(val value: A) : Context2<A> {
    override fun getContextElement(): A = value
}

context(b: B, `fun`: A)
fun foo() = Unit

context(_: Context2<C<String>>)
fun bar() {}
