class A {
    fun foo() {}

    fun goo() {
        fun <caret>innerGoo() {
            foo()
        }
        innerGoo()
    }
}