// "Make 'foo' protected" "true"

open class A {
    private val foo = 1
}

open class B : A()

class C : B() {
    fun bar() {
        <caret>foo
    }
}