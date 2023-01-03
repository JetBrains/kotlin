external open class A {
    open fun f(x: Int): Unit

    open fun f(x: String): Unit
}

class InheritClass : A() {
    override fun f(x: Int): Unit { }
}
