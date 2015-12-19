open class A {
    open fun Int.foo(n: Int): XYZ {
        return 1
    }
}

class B : A() {
    override fun Int.foo(n: Int): XYZ {
        return 2
    }
}