// "Create property 'foo' as constructor parameter" "true"

class A {
    class B {
        fun test(): Int {
            <caret>foo = 1
            return foo
        }
    }
}
