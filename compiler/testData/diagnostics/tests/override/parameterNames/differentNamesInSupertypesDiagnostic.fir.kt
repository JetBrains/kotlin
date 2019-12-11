interface C {
    fun foo(a : Int)
}

interface D {
    fun foo(b : Int)
}

interface E : C, D

interface F : C, D {
    override fun foo(a : Int) {
        throw UnsupportedOperationException()
    }
}