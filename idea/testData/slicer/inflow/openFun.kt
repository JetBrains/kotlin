// FLOW: IN
// WITH_RUNTIME

open class C {
    private var other: C? = null

    open fun foo(p: Int) {
        println(p + 1)
        other?.bar(p + 1)
    }

    open fun bar(p: Int) {
        println(<caret>p + 2)
    }
}

class D : C() {
    private var other: D? = null

    override fun foo(p: Int) {
        println(p + 3)
        other?.bar(p + 3)
    }

    override fun bar(p: Int) {
        println(p + 4)
    }
}

