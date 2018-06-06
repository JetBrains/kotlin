open class B: A<String>() {
    override fun foo(t: String) {
        super<A>.foo(t)
    }

    open fun baz(a: A<String>) {
        foo("", 0)
    }

    open fun baz(a: A<Number>) {
        a.foo(0, "")
    }
}