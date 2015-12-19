open class A {
    open fun Int.foo(n: XYZ): Int {
        return 1
    }
}

class B : A() {
    override fun Int.foo(n: XYZ): Int {
        return 2
    }
}