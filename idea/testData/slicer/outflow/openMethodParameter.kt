// FLOW: OUT

open class C {
    open fun foo(<caret>p: Any) {
        println(p)
    }
}

class D : C() {
    override fun foo(p: Any) {
        println(p + 1)
    }
}
