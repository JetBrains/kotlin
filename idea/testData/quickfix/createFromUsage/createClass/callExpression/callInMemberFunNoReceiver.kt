// "Create class 'Foo'" "true"

class A {
    class B {
        fun test() = <caret>Foo(2, "2")
    }
}