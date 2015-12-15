open class A {
    open fun foo() {

    }
}

interface Z {
    fun foo()
}

class B: A, Z {
    override fun <caret>foo() {

    }
}