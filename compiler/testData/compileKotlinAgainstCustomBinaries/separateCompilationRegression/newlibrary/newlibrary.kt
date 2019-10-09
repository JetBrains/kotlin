interface A {
    fun foo() = "FAIL"
}

interface B : A {
    override fun foo() = "OK"
}