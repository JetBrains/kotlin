open class A {
    open fun test(x: Int): String {
        return "OK"
    }
}

class B : A() {
    override fun test(x: Int): String {
        return <expr>super</expr>.test(x * 2)
    }
}