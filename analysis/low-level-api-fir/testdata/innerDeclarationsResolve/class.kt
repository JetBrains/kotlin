class B {
    fun q(): C {}
    private val y = q()

    fun foo(a: A) = with(a) {
        bar("a", y)
    }
}