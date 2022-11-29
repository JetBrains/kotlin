open class A {
    open fun test(x: Int): String {
        return "OK"
    }
}

class B : A() {
    override fun test(x: Int): String {
        return <expr>super.test(x * 2)</expr>
    }
}