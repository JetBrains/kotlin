open class A {
    open fun <caret>foo() {

    }
}

interface Z {
    fun foo()
}

class B: A, Z {
    override fun foo() {

    }
}