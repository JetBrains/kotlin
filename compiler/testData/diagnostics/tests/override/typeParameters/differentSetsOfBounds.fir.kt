interface A {
    fun <T> foo() where T : Any, T : Cloneable?
}

interface B : A {
    override fun <T> foo() where T : Any?, T : Cloneable
}
