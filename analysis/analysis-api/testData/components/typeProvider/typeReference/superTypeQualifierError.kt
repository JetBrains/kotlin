interface I {
    fun foo() {}
}

class C : I {
    override fun foo() {
        super<<caret>String>.foo()
    }
}