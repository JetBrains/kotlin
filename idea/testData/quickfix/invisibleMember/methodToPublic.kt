// "Make member visibility 'public'" "true"

open class A {
    private fun doSth() {
    }
}

class B : A() {
    fun bar() {
        <caret>doSth()
    }
}