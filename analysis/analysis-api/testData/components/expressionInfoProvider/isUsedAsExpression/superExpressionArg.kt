open class A {
    open fun test(x: Int): String {
        return "OK"
    }
}

class B : A() {
    override fun test(x: Int): String {
        return super.test(<expr>x * 2</expr>)
    }
}