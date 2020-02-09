// FLOW: IN

interface I {
    fun foo(p: Int)
}

class C : I {
    override fun foo(p: Int) {
        println(<caret>p)
    }

    fun f() {
        foo(1)
    }
}

fun f(i: I) {
    i.foo(2)
}
