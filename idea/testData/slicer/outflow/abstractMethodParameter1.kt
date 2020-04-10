// FLOW: OUT

interface I {
    fun foo(<caret>p: Any)
}

class C : I {
    override fun foo(p: Any) {
        val v = p
    }
}
