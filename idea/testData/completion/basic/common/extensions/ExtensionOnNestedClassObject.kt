package foo

class A {
    class B {
        class object
    }
}

fun A.B.Default.foo() {}

fun some() {
    A.B.<caret>
}

// EXIST: foo
