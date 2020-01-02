interface IFoo {
    fun foo()
}

fun outer() {
    fun IFoo.foo() {}
}