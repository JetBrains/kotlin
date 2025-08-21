interface I {
    fun foo() {}
}

class C : I {
    override fun foo() {
        super<<expr>I</expr>>.foo()
    }
}