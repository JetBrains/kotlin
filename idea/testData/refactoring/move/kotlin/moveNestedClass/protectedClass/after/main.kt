open class A {
    private val a = B()

    protected class C()
}

class X : A() {
    private val b = B()
}