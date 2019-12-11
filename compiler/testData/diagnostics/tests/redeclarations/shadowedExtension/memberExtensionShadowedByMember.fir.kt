interface IFooBar {
    fun foo()
    val bar: Int
}

class Host {
    fun IFooBar.foo() {}
    val IFooBar.bar: Int get() = 42
}