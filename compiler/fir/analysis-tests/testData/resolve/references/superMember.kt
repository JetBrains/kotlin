open class A {
    open fun foo() {}
}

class B : A() {
    fun bar() {
        foo()
    }
}