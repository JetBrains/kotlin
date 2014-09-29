// "Create function 'foo' from usage" "true"

class A {
    object B {
        fun test(): Int {
            return <caret>foo(2, "2")
        }
    }
}