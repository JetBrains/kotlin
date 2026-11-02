class A {
    val x: Int = 1

    companion object {
        val A.x: Int get() = <expr>x</expr>
    }
}
