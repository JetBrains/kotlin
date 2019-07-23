open class A {
    open fun isFoo(): Boolean = true
}

class B : A() {
    private val isFoo: Int = 42

    <caret>override fun isFoo(): Boolean = super.isFoo()
}