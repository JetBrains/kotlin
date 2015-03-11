package foo

class A {
    class B {
        default object
    }
}

fun A.B.Default.foo() {}

fun some() {
    A.B.<caret>
}

// EXIST: foo
