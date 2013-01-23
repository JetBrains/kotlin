// "Add 'inner' modifier" "true"
class A {
    fun foo() = 42

    class B {
        fun bar() = <caret>foo()
    }
}
