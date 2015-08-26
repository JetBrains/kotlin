// IS_APPLICABLE: false

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
            supe<caret>r<Base>@A.foo()
        }
    }
}