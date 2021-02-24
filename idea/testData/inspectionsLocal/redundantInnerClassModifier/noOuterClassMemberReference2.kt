fun foo() {}

class A {
    <caret>inner class B {
        fun b() {
            foo()
        }
    }
}