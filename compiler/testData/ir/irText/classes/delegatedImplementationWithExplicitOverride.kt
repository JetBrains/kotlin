// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57754

interface IFooBar {
    fun foo()
    fun bar()
}

object FooBarImpl : IFooBar {
    override fun foo() {}
    override fun bar() {}
}

class C : IFooBar by FooBarImpl {
    override fun bar() {}
}
