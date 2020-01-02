interface Base {
    fun foo()
}
val String.test: Base = object: Base {
    override fun foo() {
        this@test
    }
}