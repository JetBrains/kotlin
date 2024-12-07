class <caret>A : B, C {
    override fun foo(x: Int) {
    }

    override fun foo(x: String) {
    }
}

interface B: C, D

interface C {
    fun foo(x: Int)
    fun foo(x: String)
}

interface D {
    fun foo(x: Int)
    fun foo(x: String)
}
