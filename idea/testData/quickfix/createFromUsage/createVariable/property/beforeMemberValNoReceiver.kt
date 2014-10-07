// "Create property 'foo' from usage" "true"
// ERROR: Property must be initialized or be abstract

class A {
    class B {
        fun test(): Int {
            return <caret>foo
        }
    }
}
