// "Add 'inner' modifier" "true"
class A {
    fun foo() = 42

    inner class B {
        fun bar() = <caret>foo()
    }
}
