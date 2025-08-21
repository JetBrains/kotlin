interface I {
    fun foo() {}
}

class C : I {
    override fun foo() {
        <expr>super</expr><I>.foo()
    }
}