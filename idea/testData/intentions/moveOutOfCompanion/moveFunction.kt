class A {
    companion object {
        class B {

        }

        fun <caret>foo() {

        }
    }

    fun bar() {
        foo()
    }
}