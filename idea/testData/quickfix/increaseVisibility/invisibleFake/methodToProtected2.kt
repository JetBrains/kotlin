// "Make 'doSth' protected" "true"

open class A {
    private fun doSth() {
    }
}

open class B : A()

class C : B() {
    fun bar() {
        <caret>doSth()
    }
}