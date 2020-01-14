interface T {
    fun foo(
            a: Int = 1,
            b: String = "2"
    )
}

open class A: T {
    override fun foo(
            a: Int,
            b: String
    ) {
        throw UnsupportedOperationException()
    }
}

class B: A() {
    override fun foo(
            a: Int,
            b: String
    ) {
        throw UnsupportedOperationException()
    }
}