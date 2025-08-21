open class C1 {
    open fun foo() {
    }
}

class C2 : C1() {
    override fun foo() {
    }

    inner class Inner {
        fun bar() {
            this<expr>@C2</expr>.foo()
        }
    }
}