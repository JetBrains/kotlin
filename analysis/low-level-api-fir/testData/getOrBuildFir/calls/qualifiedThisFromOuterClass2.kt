open class C1 {
    open fun foo() {
    }
}

class C2 : C1() {
    override fun foo() {
    }

    inner class Inner {
        fun bar() {
            <expr>this</expr>@C2.foo()
        }
    }
}