open class A {
    open fun Int.<caret>foo(n: Int): Int {
        return 1
    }
}

class B : A() {
    override fun Int.foo(n: Int): Int {
        return 2
    }
}