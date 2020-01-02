interface A {
    fun <T> foo()
    fun <T> bar()
}

interface B {
    fun foo()
    fun bar()
}

interface C1 : A, B {
    override fun bar()
}
