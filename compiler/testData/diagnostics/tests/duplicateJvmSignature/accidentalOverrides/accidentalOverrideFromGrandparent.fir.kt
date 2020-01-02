// !DIAGNOSTICS: -UNUSED_PARAMETER
interface Foo<X> {
    fun foo(x: X)
}

open class FooImpl : Foo<String> {
    override fun foo(x: String) {
    }
}

open class FooImpl2 : FooImpl() {
    fun foo(x: Any) {
    }
}
