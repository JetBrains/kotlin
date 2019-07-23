open class A {
    open fun getFoo(i: Int): String? = null
}

class B : A() {
    private val foo = ""

    <caret>override fun getFoo(i: Int): String? = super.getFoo(i)
}