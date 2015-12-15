open class A {
    open fun foo() {

    }
}

interface Z {
    fun <caret>foo()
}

class B: A(), Z {
    override fun foo() {

    }
}