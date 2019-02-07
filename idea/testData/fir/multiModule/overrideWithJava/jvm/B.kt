class B : A() {
    override fun foo(): B = this
    fun bar(): B = this // Ambiguity, no override here

    fun test() {
        foo()
        bar()
    }
}
