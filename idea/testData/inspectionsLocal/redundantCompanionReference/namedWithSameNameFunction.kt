// PROBLEM: none
class B {
    companion object foo {
        fun foo() {}

        operator fun invoke() {
        }
    }

    fun test() {
        <caret>foo.foo()
    }
}