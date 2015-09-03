open class Base {
    open fun foo() {
    }
}

class A : Base() {
    override fun foo() {
        super.foo()
    }

    inner class C {
        fun test() {
            super<Base><caret>@A.foo()
        }
    }
}