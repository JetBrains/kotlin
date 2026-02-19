interface I {
    fun foo() {}
}

class C : I {
    override fun foo() {
        <expr>super<I></expr>.foo()
    }
}