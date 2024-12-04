open class A {
    open operator fun plus(a: A) = A()
}

class B : A() {
    override fun p<caret>lus(a: A) = B()
}