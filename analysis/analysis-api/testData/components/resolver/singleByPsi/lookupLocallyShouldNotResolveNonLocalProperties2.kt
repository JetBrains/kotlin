class A {
    val x: Int = 1

    companion object {
        val A.x: Int get() = with(A()) { <expr>x</expr> }
    }
}
