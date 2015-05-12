interface T {
    fun <caret>foo(b: String = "2",
            a: Int = 1)
}

open class A: T {
    override fun foo(b: String,
                     a: Int) {
        throw UnsupportedOperationException()
    }
}

class B: A() {
    override fun foo(b: String,
                     a: Int) {
        throw UnsupportedOperationException()
    }
}