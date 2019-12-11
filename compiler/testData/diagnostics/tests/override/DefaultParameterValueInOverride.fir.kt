open class A {
    open fun foo(a : Int) {}
}

class C : A() {
    override fun foo(a : Int = 1) {
    }
}

class D : A() {
    override fun foo(a : Int = 1) {
    }
}
